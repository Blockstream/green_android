package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_reenable_2fa
import blockstream_green.common.generated.resources.id_redeposit
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
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.feeRateWithUnit
import com.rickclephas.kmp.observableviewmodel.launch
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
                _showFeeSelector.value = accountAsset.account.network.isBitcoin
                        || (accountAsset.account.network.isLiquid && getFeeRate(FeePriority.High()) > accountAsset.account.network.defaultFee)

                _network.value = accountAsset.account.network

                combine(_feeEstimation.filterNotNull(), _feePriorityPrimitive) { _ ->
                    createTransactionParams.value = createTransactionParams()
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
        val unspentOutputs = session.getUnspentOutputs(account = account, isExpired = isRedeposit2FA)

        // For each assetId, create an output. TODO handle case where no lbtc utxo is available to cover the fees
        return unspentOutputs.unspentOutputs.keys.map { key ->
            AddressParams(
                address = session.getReceiveAddress(account).address,
                satoshi = 0,
                isGreedy = true,
                assetId = key.takeIf { account.isLiquid }
            )
        }.let { params ->
            CreateTransactionParams(
                from = accountAsset.value,
                isRedeposit = true,
                addressees = params.map { it.toJsonElement() },
                addresseesAsParams = params,
                feeRate = getFeeRate(),
                utxos = unspentOutputs.unspentOutputsAsJsonElement
            )
        }.also {
            createTransactionParams.value = it
        }
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

                val tx = session.createTransaction(network, params)

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

        }, mutex = createTransactionMutex, preAction = {
            onProgress.value = true
            _isValid.value = false
        }, onSuccess = {
            createTransaction.value = it
            _isValid.value = it != null
            _error.value = null

            if(finalCheckBeforeContinue && params != null && it != null){
                session.pendingTransaction = Triple(
                    params, it, TransactionSegmentation(
                        transactionType = TransactionType.REDEPOSIT,
                    )
                )

                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SendConfirm(
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

    init {
        _showFeeSelector.value = true
        banner.value = Banner.preview3
    }

    companion object {
        fun preview() = RedepositViewModelPreview(previewWallet(), previewAccountAsset())
    }
}