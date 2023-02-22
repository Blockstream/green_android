package com.blockstream.green.ui.receive

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import breez_sdk.InputType
import breez_sdk.LnInvoice
import breez_sdk.SwapInfo
import com.blockstream.green.data.Denomination
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Address
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.data.DenominatedValue
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.extensions.string
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.bottomsheets.DenominationListener
import com.blockstream.green.ui.bottomsheets.INote
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toAmountLookOrNa
import com.blockstream.lightning.amountSatoshi
import com.blockstream.lightning.channelFeePercent
import com.blockstream.lightning.channelMinimumFeeSatoshi
import com.blockstream.lightning.expireIn
import com.blockstream.lightning.inboundLiquiditySatoshi
import com.blockstream.lightning.maxReceivableSatoshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ReceiveViewModel @AssistedInject constructor(
    @SuppressLint("StaticFieldLeak")
    @ApplicationContext val context: Context,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted private val savedStateHandle: SavedStateHandle,
    @Assisted wallet: Wallet,
    @Assisted initAccountAsset: AccountAsset,
) : AbstractAssetWalletViewModel(
    sessionManager,
    walletRepository,
    countly,
    wallet,
    initAccountAsset
), DenominationListener, INote {
    var isDevelopment = isDevelopmentFlavor

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

    val channelFeePercent: MutableLiveData<String> = MutableLiveData("")
    val channelMinFee: MutableLiveData<String> = MutableLiveData("")
    val inboundLiquidity: MutableLiveData<String> = MutableLiveData("")
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
            }.launchIn(lifecycleScope)

        // Generate address when account & account type changes
        accountLiveData.asFlow().onEach {
            generateAddress()
        }.launchIn(lifecycleScope)

        combine(accountLiveData.asFlow(), lightningInvoice.asFlow(), swapInfo.asFlow(), showOnchainAddress.asFlow()) { account, lightningInvoice, swapInfo, _ ->
            Triple(account , lightningInvoice, swapInfo)
        }.onEach {
            if(account.isLightning){
                addressLiveData.value = if (showOnchainAddress.boolean()) swapInfo.value?.let {
                    Address.fromSwapInfo(swapInfo = it)
                } else lightningInvoice.value?.let { Address.fromInvoice(invoice = it) }

                update()
            }
        }.launchIn(lifecycleScope)


        session.lightning?.also {
            // Support single lightning account, else we have to incorporate account change events
            val lightningAccount = session.lightningAccount

            combine(session.lightningSdk.lspInfoStateFlow.filterNotNull(), denomination.asFlow()) { lspInfo , _ ->
                lspInfo
            }.onEach { lspInfo ->
                channelFeePercent.value = "${lspInfo.channelFeePercent()}%"
                channelMinFee.value = lspInfo.channelMinimumFeeSatoshi().toAmountLook(
                    session = session,
                    assetId = account.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                )
            }.launchIn(viewModelScope)

            combine(amount.asFlow(), channelFeePercent.asFlow(), channelMinFee.asFlow()) { _, _, _ ->
                Unit
            }.onEach {
                updateAmountExchangeRate()
            }
            .launchIn(viewModelScope)

            denomination.asFlow()
                .onEach {
                    amountCurrency.value = it.unit(session, lightningAccount.network.policyAsset)
                }.launchIn(viewModelScope)

            combine(session.lightningNodeInfoStateFlow, denomination.asFlow()) { nodeState, _ ->
                nodeState
            }.onEach {
                maxReceiveAmount.value = it.maxReceivableSatoshi().toAmountLookOrNa(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                )

                inboundLiquidity.value = it.inboundLiquiditySatoshi().toAmountLookOrNa(
                    session = session,
                    assetId = lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                )
            }.launchIn(lifecycleScope)

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

            }.launchIn(lifecycleScope)

            session.lastInvoicePaid.filterNotNull().onEach { paidDetails ->
                if(paidDetails.paymentHash == lightningInvoice.value?.paymentHash){

                    (withContext(context = Dispatchers.IO) {
                        // Parse the actual Bolt11 invoice
                        session.parseInput(paidDetails.bolt11)
                    }?.second as? InputType.Bolt11)?.also {
                        onEvent.value = ConsumableEvent(GdkEvent.SuccessWithData(it.invoice))
                        lightningInvoice.value = null
                    }

                }
            }.launchIn(lifecycleScope)
        }
    }

    fun generateAddress() {
        logger.info { "Generating address for ${account.name}" }
        showAssetWhitelistWarning.value = account.isLiquid && session.device?.isLedger == true
        canValidateAddressInDevice.value = session.device?.let { device ->
            !account.isLightning && (
            device.isJade ||
                    (device.isLedger && network.isLiquid && !network.isSinglesig) ||
                    (device.isLedger && !network.isLiquid && network.isSinglesig) ||
                    (device.isTrezor && !network.isLiquid && network.isSinglesig)
                    )
        } ?: false

        if (!account.isLightning) {
            doUserAction({
                session.getReceiveAddress(account)
            }, onSuccess = {
                addressLiveData.value = it
                update()
            })
        }
    }

    fun validateAddressInDevice() {
        countly.verifyAddress(session, account)

        addressLiveData.value?.let { address ->
            deviceAddressValidationEvent.value = ConsumableEvent(null)

            session.hwWallet?.let { hwWallet ->
                doUserAction({
                    hwWallet.getGreenAddress(
                        network,
                        null,
                        account,
                        address.userPath,
                        address.subType ?: 0
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
        if(account.isLightning){
            addressUri.value = addressLiveData.value?.address?.takeIf { it.isNotBlank() }?.let {
                isAddressUri.value = false
                Uri.Builder().also {
                    it.scheme(account.network.bip21Prefix)
                    it.opaquePart(addressLiveData.value?.address?.uppercase()) // bech32 is case insensitive
                }.toString()
            }
        }else if (requestAmount.value != null) {
            isAddressUri.value = true

            // Use 2 different builders, we are restricted by spec
            // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part

            val scheme = Uri.Builder().also {
                it.scheme(account.network.bip21Prefix)
                it.opaquePart(addressLiveData.value?.address)
            }.toString()

            val query = Uri.Builder().also {
                if (!requestAmount.value.isNullOrBlank()) {
                    it.appendQueryParameter("amount", requestAmount.value)
                }

                if (network.isLiquid) {
                    it.appendQueryParameter("assetid", accountAsset.assetId)
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

    private fun setLightningInvoice(invoice: LnInvoice) {
        doUserAction({
            invoice.amountSatoshi().let { amountSatoshi ->
                amountSatoshi.toAmountLookOrNa(
                    session = session,
                    assetId = account.network.policyAsset,
                    denomination = denomination.value.takeIf { it?.isFiat == false } ?: Denomination.default(session),
                    withUnit = true
                ) to amountSatoshi.toAmountLookOrNa(
                    session = session,
                    assetId = account.network.policyAsset,
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
                DAY_IN_MILLIS
            ).toString()

            lightningInvoice.value = invoice
        })
    }

    fun createLightningInvoice(){
        doUserAction({
            val amount = UserInput.parseUserInput(
                session = session,
                input = amount.value,
                assetId = account.network.policyAsset,
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
                    assetId = accountAsset.assetId,
                    denomination = denomination.value
                ).getBalance()
            }

            balance to (balance?.let {
                "≈ " + it.toAmountLook(
                    session = session,
                    assetId = accountAsset.assetId,
                    denomination = Denomination.exchange(session = session, denomination = denomination.value),
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false
                )
            } ?: "")
        }, preAction = null, postAction = null, onSuccess = {
            val balance = it.first

            amountExchange.postValue(it.second)

            amountIsValid.value = if (amount.string().isBlank()) {
                0
            } else if (balance == null) {
                -1
            } else {
                if (balance.satoshi >= 0 &&
                    balance.satoshi <= session.lightningNodeInfoStateFlow.value.maxReceivableSatoshi() &&
                    (
                            balance.satoshi <= session.lightningNodeInfoStateFlow.value.inboundLiquiditySatoshi() ||
                                    session.lspInfoStateFlow.value?.let { balance.satoshi >= it.channelMinimumFeeSatoshi() } == true
                            )
                ) 1 else -1
            }

            showLiquidityFee.value = amountIsValid.value == 1 && session.lspInfoStateFlow.value != null &&
                    (session.lightningNodeInfoStateFlow.value.inboundLiquidityMsats == 0uL || (balance != null && balance.satoshi >= session.lightningNodeInfoStateFlow.value.inboundLiquiditySatoshi()))

            viewModelScope.launch {
                liquidityFeeError.value = if (amountIsValid.value == -1 && balance != null) {
                    val channelMinimum =
                        session.lightningSdk.lspInfoStateFlow.value?.channelMinimumFeeSatoshi() ?: 0
                    if (balance.satoshi > session.lightningNodeInfoStateFlow.value.maxReceivableSatoshi()) {
                        context.getString(R.string.id_the_amount_you_requested_is_above)
                    } else if (balance.satoshi < channelMinimum) {
                        context.getString(
                            R.string.id_the_amount_you_requested_is_below,
                            channelMinimum.toAmountLook(
                                session = session,
                                withUnit = true
                            )
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

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            savedStateHandle: SavedStateHandle,
            wallet: Wallet,
            initAccountAsset: AccountAsset
        ): ReceiveViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
            wallet: Wallet,
            initAccountAsset: AccountAsset
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    return assistedFactory.create(
                        handle,
                        wallet,
                        initAccountAsset,
                    ) as T
                }
            }
    }

    override fun setDenomination(denominatedValue: DenominatedValue) {
        amount.value = denominatedValue.asInput(session) ?: ""
        denomination.value = denominatedValue.denomination
    }
}
