package com.blockstream.green.ui.receive

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import androidx.lifecycle.*
import breez_sdk.InputType
import breez_sdk.LnInvoice
import breez_sdk.ReceivePaymentResponse
import breez_sdk.SwapInfo
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.lightning.expireIn
import com.blockstream.common.lightning.feeSatoshi
import com.blockstream.common.lightning.fromInvoice
import com.blockstream.common.lightning.fromSwapInfo
import com.blockstream.common.lightning.inboundLiquiditySatoshi
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.receiveAmountSatoshi
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.common.utils.UserInput
import com.blockstream.green.R
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import com.blockstream.green.ui.bottomsheets.DenominationListener
import com.blockstream.green.ui.bottomsheets.INote
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toAmountLookOrNa
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam


@KoinViewModel
class ReceiveViewModel constructor(
    @SuppressLint("StaticFieldLeak") val context: Context,
    @InjectedParam wallet: GreenWallet,
    @InjectedParam initAccountAsset: AccountAsset,
) : AbstractAssetWalletViewModel(
    wallet,
    initAccountAsset
), DenominationListener, INote {
    var addressLiveData = MutableLiveData<Address>()
    val addressAsString: String get() = addressLiveData.value?.address ?: ""
    val addressUri = MutableLiveData<String>("")

    val requestAmount = MutableLiveData<String?>()

    val amount: MutableLiveData<String> = MutableLiveData("")
    val amountCurrency = MutableLiveData<String>("")
    val amountExchange = MutableLiveData("")
    val amountIsValid = MutableLiveData(0)
    val denomination = MutableLiveData(Denomination.default(session))
    val maxReceiveAmount: MutableLiveData<String> = MutableLiveData("")

    val note: MutableLiveData<String> = MutableLiveData("")

    val isSetupChannel: MutableLiveData<Boolean> = MutableLiveData(false)

    val channelFee: MutableLiveData<String> = MutableLiveData("")
    val channelFeeFiat: MutableLiveData<String> = MutableLiveData("")

    val inboundLiquidity: MutableLiveData<String> = MutableLiveData("")
    val inboundLiquidityFiat: MutableLiveData<String> = MutableLiveData("")

    val liquidityFeeError: MutableLiveData<String?> = MutableLiveData(null)
    val showLiquidityFee: MutableLiveData<Boolean> = MutableLiveData(false)

    val onChainMin: MutableLiveData<String> = MutableLiveData("")
    val onChainMax: MutableLiveData<String> = MutableLiveData("")

    val lightningInvoice = MutableLiveData<LnInvoice?>(null)
    val swapInfo = MutableLiveData<SwapInfo?>(null)
    val showOnchainAddress = MutableLiveData<Boolean>(false)

    val invoiceAmount = MutableLiveData<String>()
    val invoiceExpiration = MutableLiveData<String>()
    val invoiceFiatAmount = MutableLiveData<String>()

    val addressQRBitmap = MutableLiveData<Bitmap?>()

    val isAddressUri = MutableLiveData(false)

    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    // only show if we are on Liquid and we are using Ledger
    val showAssetWhitelistWarning = MutableLiveData(false)
    val canValidateAddressInDevice = MutableLiveData(false)

    val accountAssetLocked = MutableLiveData(false)

    init {
        accountAssetLiveData.asFlow()
            .distinctUntilChanged()
            .onEach {
                clearRequestAmount()
            }.launchIn(viewModelScope.coroutineScope)

        // Generate address when account & account type changes
        accountLiveData.asFlow().onEach {
            generateAddress()
        }.launchIn(viewModelScope.coroutineScope)

        combine(accountLiveData.asFlow(), lightningInvoice.asFlow(), swapInfo.asFlow(), showOnchainAddress.asFlow()) { account, lightningInvoice, swapInfo, _ ->
            Triple(account , lightningInvoice, swapInfo)
        }.onEach {
            if(accountValue.isLightning){
                addressLiveData.value = if (showOnchainAddress.boolean()) swapInfo.value?.let {
                    Address.fromSwapInfo(swapInfo = it)
                } else lightningInvoice.value?.let { Address.fromInvoice(invoice = it) }

                update()
            }
        }.launchIn(viewModelScope.coroutineScope)


        session.lightning?.also {
            // Support single lightning account, else we have to incorporate account change events
            val lightningAccount = session.lightningAccount

            combine(session.lightningSdk.lspInfoStateFlow.filterNotNull(), amount.asFlow(), denomination.asFlow()) { lspInfo , _, _ ->
                lspInfo
            }.onEach { _ ->
                updateAmountExchangeRate()
            }.launchIn(viewModelScope.coroutineScope)

            denomination.asFlow()
                .onEach {
                    amountCurrency.value = it.unit(session, lightningAccount.network.policyAsset)
                }.launchIn(viewModelScope.coroutineScope)

            combine(session.lightningNodeInfoStateFlow, denomination.asFlow()) { nodeState, _ ->
                nodeState
            }.onEach {
                maxReceiveAmount.value = it.maxReceivableSatoshi().toAmountLookOrNa(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                )

                isSetupChannel.value = it.inboundLiquiditySatoshi() == 0L

                inboundLiquidity.value = it.inboundLiquiditySatoshi().toAmountLookOrNa(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = denomination.value?.notFiat(),
                    withUnit = true
                ) ?: ""

                inboundLiquidityFiat.value = it.inboundLiquiditySatoshi().toAmountLook(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = Denomination.fiat(session),
                    withUnit = true
                ) ?: ""
            }.launchIn(viewModelScope.coroutineScope)

            combine(swapInfo.asFlow().filterNotNull(), denomination.asFlow()) { swapInfo, _ ->
                swapInfo
            }.onEach {
                onChainMin.value = it.minAllowedDeposit.toAmountLookOrNa(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true,

                )

                onChainMax.value = it.maxAllowedDeposit.toAmountLookOrNa(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                )

            }.launchIn(viewModelScope.coroutineScope)

            session.lastInvoicePaid.filterNotNull().onEach { paidDetails ->
                if(paidDetails.paymentHash == lightningInvoice.value?.paymentHash){

                    (withContext(context = Dispatchers.IO) {
                        // Parse the actual Bolt11 invoice
                        session.parseInput(paidDetails.bolt11)
                    }?.second as? InputType.Bolt11)?.also {
                        postSideEffect(SideEffects.Success(it.invoice))
                        lightningInvoice.value = null
                    }

                }
            }.launchIn(viewModelScope.coroutineScope)
        }
    }

    fun generateAddress() {
        logger.info { "Generating address for ${accountValue.name}" }
        showAssetWhitelistWarning.value = accountValue.isLiquid && session.device?.isLedger == true
        canValidateAddressInDevice.value = session.device?.let { device ->
            !accountValue.isLightning && (
            device.isJade ||
                    (device.isLedger && network.isLiquid && !network.isSinglesig) ||
                    (device.isLedger && !network.isLiquid && network.isSinglesig) ||
                    (device.isTrezor && !network.isLiquid && network.isSinglesig)
                    )
        } ?: false

        if (!accountValue.isLightning) {
            doUserAction({
                session.getReceiveAddress(accountValue)
            }, onSuccess = {
                addressLiveData.value = it
                update()
            })
        }
    }

    fun validateAddressInDevice() {
        countly.verifyAddress(session, accountValue)

        addressLiveData.value?.let { address ->
            deviceAddressValidationEvent.value = ConsumableEvent(null)

            session.gdkHwWallet?.let { hwWallet ->
                doUserAction({
                    hwWallet.getGreenAddress(
                        network = network,
                        hwInteraction = null,
                        account = accountValue,
                        path = address.userPath ?: listOf(),
                        csvBlocks = address.subType ?: 0
                    )
                }, preAction = null, postAction = null, timeout = 30, onSuccess = {
                    if (it == address.address) {
                        deviceAddressValidationEvent.value = ConsumableEvent(true)
                    } else {
                        deviceAddressValidationEvent.value = ConsumableEvent(false)
                    }
                })
            }
        }
    }

    private fun update() {
        updateAddressUri()
        updateQR()
    }

    private fun updateAddressUri() {
        if (accountValue.isLightning) {
            if (showOnchainAddress.boolean()) {
                addressUri.value = addressLiveData.value?.address?.takeIf { it.isNotBlank() }
                isAddressUri.value = false
            } else {
                addressUri.value = addressLiveData.value?.address?.takeIf { it.isNotBlank() }?.let {
                    isAddressUri.value = false
                    Uri.Builder().also {
                        it.scheme(accountValue.network.bip21Prefix)
                        it.opaquePart(addressLiveData.value?.address?.uppercase()) // bech32 is case insensitive
                    }.toString()
                }
            }
        } else if (requestAmount.value != null) {
            isAddressUri.value = true

            // Use 2 different builders, we are restricted by spec
            // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part

            val scheme = Uri.Builder().also {
                it.scheme(accountValue.network.bip21Prefix)
                it.opaquePart(addressLiveData.value?.address)
            }.toString()

            val query = Uri.Builder().also {
                if (!requestAmount.value.isNullOrBlank()) {
                    it.appendQueryParameter("amount", requestAmount.value)
                }

                if (network.isLiquid) {
                    it.appendQueryParameter("assetid", accountAssetValue.assetId)
                }

            }.toString()

            addressUri.value = scheme + query
        } else {
            isAddressUri.value = false
            addressUri.value = addressLiveData.value?.address
        }
    }

    override fun saveNote(note: String) {
        this.note.value = note
        if(this.amount.value.isNotBlank()) {
            createLightningInvoice()
        }
    }

    private fun updateQR() {
        addressQRBitmap.postValue(createQrBitmap(addressUri.value ?: ""))
    }

    fun setRequestAmount(amount: String?) {
        requestAmount.value = amount
        update()
    }

    private fun setLightningInvoice(paymentResponse: ReceivePaymentResponse) {
        val invoice = paymentResponse.lnInvoice
        val openingFeeParams = paymentResponse.openingFeeParams

        doUserAction({

            invoice.receiveAmountSatoshi(openingFeeParams).let { amountSatoshi ->
                amountSatoshi.toAmountLookOrNa(
                    session = session,
                    assetId = accountValue.network.policyAsset,
                    denomination = denomination.value.takeIf { it?.isFiat == false } ?: Denomination.default(session),
                    withUnit = true
                ) to amountSatoshi.toAmountLookOrNa(
                    session = session,
                    assetId = accountValue.network.policyAsset,
                    denomination = Denomination.fiat(session),
                    withUnit = true
                )

            }
        }, onSuccess = {
            invoiceAmount.value = it.first
            invoiceFiatAmount.value = it.second

//            val now = Clock.System.now()
//            val instantInTheFuture: Instant = Instant.fromEpochMilliseconds(invoice.expiry.toLong())
//            val durationSinceThen: Duration = instantInTheFuture - now

            invoiceExpiration.value = DateUtils.getRelativeTimeSpanString(
                invoice.expireIn().toEpochMilliseconds(),
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()

            lightningInvoice.value = invoice
        })
    }

    fun createLightningInvoice(){
        doUserAction({
            val amount = UserInput.parseUserInput(
                session = session,
                input = amount.value,
                assetId = accountValue.network.policyAsset,
                denomination = denomination.value
            ).getBalance()?.satoshi ?: 0

            session.createLightningInvoice(amount, note.value ?: "")
        }, onSuccess = {
            setLightningInvoice(it)
        })
    }

    fun createOnchain(){
        doUserAction({
            session.receiveOnchain()
        }, onSuccess = {
            swapInfo.value = it
            showOnchainAddress.value = true
        })
    }

    private fun updateAmountExchangeRate() {
        // Convert between BTC / Fiat
        doUserAction({
            val balance = amount.string().takeIf { it.isNotBlank() }?.let {
                UserInput.parseUserInput(
                    session = session,
                    input = it,
                    assetId = accountAssetValue.assetId,
                    denomination = denomination.value
                ).getBalance()
            }

            (balance?.let {
                "≈ " + it.toAmountLook(
                    session = session,
                    assetId = accountAssetValue.assetId,
                    denomination = Denomination.exchange(
                        session = session,
                        denomination = denomination.value
                    ),
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false
                )
            } ?: "").also {
                amountExchange.postValue(it)
            }

            balance to
                    balance?.satoshi?.let {
                        if (it > session.lightningNodeInfoStateFlow.value.inboundLiquiditySatoshi()) session.lightningSdk.openChannelFee(
                            it
                        ) else null
                    }
        }, preAction = null, postAction = null, onSuccess = {
            val balance = it.first
            val openChannelFee = it.second

            amountIsValid.value = if (amount.string().isBlank()) {
                0
            } else if (balance == null) {
                -1
            } else {
                if (balance.satoshi >= 0 &&
                    balance.satoshi <= session.lightningNodeInfoStateFlow.value.maxReceivableSatoshi() &&
                    (balance.satoshi <= session.lightningNodeInfoStateFlow.value.inboundLiquiditySatoshi() || (balance.satoshi > (openChannelFee?.feeSatoshi() ?: 0)))
                ) 1 else -1
            }

            viewModelScope.coroutineScope.launch {

                channelFee.value = openChannelFee?.feeSatoshi()?.toAmountLook(
                    session = session,
                    assetId = accountValue.network.policyAsset,
                    denomination = denomination.value?.notFiat(),
                    withUnit = true
                ) ?: ""

                channelFeeFiat.value = openChannelFee?.feeSatoshi()?.toAmountLook(
                    session = session,
                    assetId = accountValue.network.policyAsset,
                    denomination = Denomination.fiat(session),
                    withUnit = true
                ) ?: ""

                showLiquidityFee.value =
                    amountIsValid.value == 1 && session.lspInfoStateFlow.value != null && (session.lightningNodeInfoStateFlow.value.inboundLiquidityMsats == 0uL || (balance != null && balance.satoshi >= session.lightningNodeInfoStateFlow.value.inboundLiquiditySatoshi()))

                liquidityFeeError.value = if (amountIsValid.value == -1 && balance != null) {
                    val maxReceivableSatoshi = session.lightningNodeInfoStateFlow.value.maxReceivableSatoshi()
                    val channelMinimum = openChannelFee?.feeSatoshi() ?: 0
                    if (balance.satoshi > maxReceivableSatoshi) {
                        context.getString(
                            R.string.id_you_cannot_receive_more_than_s,
                            maxReceivableSatoshi.toAmountLook(session = session, withUnit = true, denomination = denomination.value?.notFiat()),
                            maxReceivableSatoshi.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = Denomination.fiat(session)
                            )
                        )
                    } else if (balance.satoshi <= channelMinimum) {
                        context.getString(R.string.id_this_amount_is_below_the,
                            channelMinimum.toAmountLook(session = session, withUnit = true, denomination = denomination.value?.notFiat()),
                            channelMinimum.toAmountLook(session = session, withUnit = true, denomination = Denomination.fiat(session))
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }, onError = {
            amountExchange.postValue("")
            amountIsValid.value = -1
            liquidityFeeError.value = null
            showLiquidityFee.value = false
        })
    }

    fun clearRequestAmount() {
        setRequestAmount(null)
    }

    override fun setDenomination(denominatedValue: DenominatedValue) {
        amount.value = denominatedValue.asInput(session) ?: ""
        denomination.value = denominatedValue.denomination
    }
}
