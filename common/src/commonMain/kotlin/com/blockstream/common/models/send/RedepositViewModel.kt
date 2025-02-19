package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_redeposit
import blockstream_green.common.generated.resources.id_reenable_2fa
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.Banner
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.PendingTransaction
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.gdk.params.toJsonElement
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.feeRateWithUnit
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.getString

abstract class RedepositViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
) : CreateTransactionViewModelAbstract(
        greenWallet = greenWallet,
        accountAssetOrNull = accountAsset
    ) {
    override fun screenName(): String = "Redeposit"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.accountSegmentation(session = session, account = account)
    }
}

class RedepositViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    private val isRedeposit2FA: Boolean
) : RedepositViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    private val _isWatchOnly = MutableStateFlow(false)
    override val isWatchOnly = _isWatchOnly

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(if (isRedeposit2FA) Res.string.id_reenable_2fa else Res.string.id_redeposit),
                subtitle = greenWallet.name,
            )
        }

        if (account.isLightning) {
            postSideEffect(
                SideEffects.NavigateBack(
                    title = StringHolder.create("Lightning"),
                    message = StringHolder.create("Lightning redeposit is not supported")
                )
            )
        } else {
            session.ifConnected {
                _isWatchOnly.value = session.isWatchOnly

                _showFeeSelector.value = accountAsset.account.network.isBitcoin
                        || (accountAsset.account.network.isLiquid && getFeeRate(FeePriority.High()) > accountAsset.account.network.defaultFee)

                _network.value = accountAsset.account.network

                combine(_feeEstimation.filterNotNull(), _feePriorityPrimitive) { _ ->
                    createTransactionParams.value = tryCatch(context = Dispatchers.Default) { createTransactionParams() }
                }.launchIn(this)
            }
        }

        bootstrap()
    }


    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is Events.Continue -> {
                createTransactionParams.value?.also {
                    createTransaction(params = it, finalCheckBeforeContinue = true)
                }
            }

        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams {
        // val unspentOutputs = session.getUnspentOutputs(account = account, isExpired = isRedeposit2FA)

        // The following only works for re-depositing expired utxos not as a way to consolidate your utxos into one.

        // The UTXOs that should be re-deposited, Unspent outputs JSON as returned by GA_get_unspent_outputs.
        // Non-expired UTXOs will be ignored, except for LBTC UTXOs that may be required for fees when re-depositing assets.
        // For Liquid, all assets except LBTC must come from the same subaccount.
        val unspentOutputs = session.getUnspentOutputs(account = account, isExpired = false)

        return (if (isRedeposit2FA) {
            CreateTransactionParams(
                utxos = unspentOutputs.unspentOutputs,
                feeRate = getFeeRate(),
                feeSubaccount = account.pointer,
                isRedeposit = true
            )
        } else {
            val addressee = unspentOutputs.unspentOutputs.keys.map { key ->
                session.getReceiveAddress(account).let {
                    AddressParams(
                        address = it.address,
                        satoshi = 0,
                        isGreedy = true,
                        assetId = key.takeIf { account.isLiquid },
                        receiveAddress = it
                    )
                }
            }

            CreateTransactionParams(
                from = accountAsset.value,
                addressees = addressee.toJsonElement(),
                utxos = unspentOutputs.unspentOutputs,
                feeRate = getFeeRate(),
                isRedeposit = true
            )
        })
    }

    override fun createTransaction(
        params: CreateTransactionParams?,
        finalCheckBeforeContinue: Boolean
    ) {
        doAsync({
            if(params == null){
                return@doAsync null
            }

            accountAsset.value?.let { accountAsset ->
                val network = accountAsset.account.network

                val tx = if (isRedeposit2FA) session.createRedepositTransaction(
                    network = network,
                    params = params
                ) else session.createTransaction(network = network, params = params)
                    .let { transaction ->
                        // Copy userPath/subType from receiveAddress so that we can validate the address later in Jade
                        transaction.copy(outputs = transaction.outputs.map { output ->
                            params.addresseesAsParams?.find { it.address == output.address }?.let {
                                output.copy(
                                    userPath = it.receiveAddress?.userPath,
                                    subType = it.receiveAddress?.subType
                                )
                            } ?: output
                        })
                    }

                // Clear error as soon as possible
                if (tx.error.isBlank()) {
                    _error.value = null
                }

                tx.fee?.takeIf { it != 0L || tx.error.isNullOrBlank() }.also {
                    _feePriority.value = calculateFeePriority(
                        session = session,
                        feePriority = _feePriority.value,
                        feeAmount = it,
                        feeRate = tx.feeRate?.feeRateWithUnit()
                    )
                }

                tx.error.takeIf { it.isNotBlank() }?.also {
                    throw Exception(it)
                }

                tx
            }

        }, mutex = createTransactionMutex, onSuccess = {
            createTransaction.value = it
            _isValid.value = it != null
            _error.value = null

            if(finalCheckBeforeContinue && params != null && it != null){
                session.pendingTransaction = PendingTransaction(
                    params = params,
                    transaction = it,
                    segmentation = TransactionSegmentation(
                        transactionType = TransactionType.REDEPOSIT,
                    )
                )

                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SendConfirm(
                    greenWallet = greenWallet,
                    accountAsset = accountAsset.value!!,
                    denomination = denomination.value
                )))
            }
        }, onError = {
            createTransaction.value = null
            _isValid.value = false
            _error.value = it.message
        })
    }
}

class RedepositViewModelPreview(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    RedepositViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val isWatchOnly: StateFlow<Boolean> = MutableStateFlow(false)

    init {
        _showFeeSelector.value = true
        banner.value = Banner.preview3
    }

    companion object {
        fun preview() = RedepositViewModelPreview(previewWallet(), previewAccountAsset())
    }
}