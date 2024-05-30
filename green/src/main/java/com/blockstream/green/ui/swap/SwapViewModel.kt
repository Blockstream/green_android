package com.blockstream.green.ui.swap;

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.CountlyAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.assetTicker
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.LiquiDexV0List
import com.blockstream.common.gdk.data.SwapAsset
import com.blockstream.common.gdk.data.SwapProposal
import com.blockstream.common.gdk.data.Utxo
import com.blockstream.common.gdk.params.CompleteSwapParams
import com.blockstream.common.gdk.params.CreateSwapParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.gdk.params.LiquidDexV0AssetParams
import com.blockstream.common.gdk.params.LiquidDexV0Params
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.UserInput
import com.blockstream.green.extensions.toggle
import com.blockstream.green.utils.exchangeRate
import com.blockstream.green.utils.toAmountLook
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import org.json.JSONArray
import org.json.JSONObject
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@FlowPreview
@KoinViewModel
class SwapViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam val proposal: SwapProposal?,
) : GreenViewModel(wallet) {

    var utxos: List<Utxo> = listOf()

    val exchangeRate = MutableLiveData<String?>(null)
    val exchangeRateDirection = MutableLiveData<Boolean>(false)
    val inputLiveData = MutableLiveData<SwapAsset?>(null)
    val outputLiveData = MutableLiveData<SwapAsset?>(null)

    private var _utxoLiveData = MutableLiveData<Utxo?>()
    val utxoLiveData: LiveData<Utxo?> get() = _utxoLiveData

    var utxo
        get() = _utxoLiveData.value
        set(value) {
            _utxoLiveData.value = value
            exchangeRateDirection.value = false
        }

    private var _remoteAssetsLiveData = MutableLiveData<Map<String, CountlyAsset>>(mapOf())
    val remoteAssetsLiveData: LiveData<Map<String, CountlyAsset>> get() = _remoteAssetsLiveData

    var remoteAssets
        get() = remoteAssetsLiveData.value!!
        set(value) {
            _remoteAssetsLiveData.value = value
        }

    private var _toAssetIdLiveData = MutableLiveData<String?>()
    val toAssetIdLiveData: LiveData<String?> get() = _toAssetIdLiveData

    var toAssetId
        get() = _toAssetIdLiveData.value
        set(value) {
            _toAssetIdLiveData.value = value
            toAssetTicker.value = value.assetTicker(session)
        }

    val toAssetTicker = MutableLiveData<String>()

    private var _toAssetsLiveData = MutableLiveData<List<String>>(listOf())
    val toAssetsLiveData: LiveData<List<String>> get() = _toAssetsLiveData

    var toAssets
        get() = _toAssetsLiveData.value!!
        set(value) {
            _toAssetsLiveData.value = value
        }

    val toAmount = MutableLiveData<String>()

    val enabledAccounts = session.accounts.value.filter { it.isMultisig && it.isLiquid && it.type == AccountType.STANDARD }

    init {
        proposal ?: run {
            viewModelScope.coroutineScope.launch(context = logException(countly)) {
                utxos = withContext(context = Dispatchers.IO) {
                    session.getUnspentOutputs(enabledAccounts).unspentOutputs.values.flatten()
                        .sortedWith(session::sortUtxos)
                }

                utxo = utxos.firstOrNull()
            }
        }

        utxoLiveData.asFlow().onEach {
            updateToAssets()

        }.launchIn(viewModelScope.coroutineScope)

        session.enrichedAssets.onEach {
            remoteAssets = it.map { it.assetId to it }.toMap()
            updateToAssets()
        }.launchIn(viewModelScope.coroutineScope)

        combine(utxoLiveData.asFlow(), toAssetIdLiveData.asFlow(), exchangeRateDirection.asFlow(), toAmount.asFlow()) { _, _, _, _ ->
            Unit
        }.debounce(50).onEach {

            doAsync({
                // Convert utxo satoshi values
                val utxoAmount = if(utxo?.assetId.isPolicyAsset(session)){
                    session.convert(
                        assetId = utxo?.assetId,
                        asLong = utxo!!.satoshi
                    )!!.valueInMainUnit
                }else{
                    session.convert(
                        assetId = utxo?.assetId,
                        asLong = utxo!!.satoshi
                    )!!.valueInMainUnit
                }

                // Make conversion for BTC values
                val toAmount = UserInput.parseUserInput(
                    session = session,
                    input = toAmount.value!!,
                    assetId = toAssetId!!
                ).getBalance()!!.valueInMainUnit

                val rate = if(exchangeRateDirection.value == false){
                    exchangeRate(
                        session,
                        assetId1 = utxo!!.assetId,
                        amount1 = utxoAmount,
                        assetId2 = toAssetId!!,
                        amount2 = toAmount
                    )
                }else{
                    exchangeRate(
                        session,
                        assetId1 = toAssetId!!,
                        amount1 = toAmount,
                        assetId2 = utxo!!.assetId,
                        amount2 = utxoAmount,
                    )
                }

                rate
            }, preAction = null, postAction = null, onSuccess = {
                exchangeRate.postValue(it)
            }, onError = {
                exchangeRate.postValue(null)
            })

        }.launchIn(viewModelScope.coroutineScope)

        viewModelScope.coroutineScope.launch {
            proposal?.let { parseProposal(it) }
        }
    }

    private fun parseProposal(proposal: SwapProposal) {
        proposal.inputs.first().let {
            inputLiveData.postValue(it)
        }
        proposal.outputs.first().let {
            outputLiveData.postValue(it)
        }

        doAsync({
            val utxo = proposal.inputs.first()
            val out = proposal.outputs.first()
            val utxoAmount = utxo.amount.toAmountLook(session, utxo.assetId,
                withUnit = false
            )
            val outAmount = out.amount.toAmountLook(session, out.assetId,
                withUnit = false
            )
            outAmount?.let { toAmount.postValue(it) }

            val rate = if(exchangeRateDirection.value == false){
                exchangeRate(
                    session,
                    assetId1 = utxo.assetId,
                    amount1 = utxoAmount!!,
                    assetId2 = out.assetId,
                    amount2 = outAmount!!
                )
            }else{
                exchangeRate(
                    session,
                    assetId1 = out.assetId,
                    amount1 = outAmount!!,
                    assetId2 = utxo.assetId,
                    amount2 = utxoAmount!!,
                )
            }
            rate
        }, preAction = null, postAction = null, onSuccess = {
            exchangeRate.postValue(it)
        }, onError = {
            exchangeRate.postValue(null)
        })
    }

    private fun updateToAssets() {
        toAssets =
            (session.walletAssets.value.assets.keys + remoteAssets.filterValues { !it.isAmp }.keys).filter { it != utxo?.assetId && it != BTC_POLICY_ASSET }
                .toList()

        if (toAssetId.isNullOrBlank() || toAssetId == utxo?.assetId) {
            toAssetId = toAssets.firstOrNull()
        }
    }

    fun createSwapProposal(twoFactorResolver: TwoFactorResolver) {
        doAsync({
            val satoshi = UserInput.parseUserInput(
                session = session,
                input = toAmount.value!!,
                assetId = toAssetId!!
            ).getBalance()!!.satoshi

            val v0Params = LiquidDexV0Params(
                send = listOf(utxo!!.jsonElement!!),
                receive = listOf(LiquidDexV0AssetParams(assetId = toAssetId!!, satoshi = satoshi))
            )

            val params = CreateSwapParams(liquidexV0 = v0Params)

            val result = session.createSwapTransaction(
                network = session.liquidMultisig!!,
                params = params,
                twoFactorResolver = twoFactorResolver
            )

            result.liquiDexV0.toSwapProposal()
        }, onSuccess = {
            postSideEffect(SideEffects.Navigate(proposal))
        })
    }

    fun completeSwapProposal(twoFactorResolver: TwoFactorResolver) {
        if (proposal == null) {
            return
        }

        doAsync({
            val v0List= LiquiDexV0List(
                proposals = listOf(proposal)
            )
            val unspentOutputs = session.getUnspentOutputs(enabledAccounts.first())
            val params = CompleteSwapParams(
                liquidexV0 = v0List,
                utxos = unspentOutputs.unspentOutputsAsJsonElement
            )

            val tx = session.completeSwapTransaction (
                network = session.liquidMultisig!!,
                params = params,
                twoFactorResolver = twoFactorResolver
            )
            tx.signWith = listOf("user", "green-backend")
            updateTransaction(tx)
        }, onSuccess = { tx ->
            val params = CreateTransactionParams(
                addressees = null,
                feeRate = null,
                utxos = null
            )

            session.pendingTransaction = Triple(
                params, tx, TransactionSegmentation(
                    transactionType = TransactionType.SWAP,
                    addressInputType = null,
                    sendAll = false
                )
            )
            postSideEffect(SideEffects.Navigate(tx))
        })
    }

    private fun updateTransaction(tx: CreateTransaction) : CreateTransaction {
        val string = tx.jsonElement!!.toString()
        val tmp = JSONObject(string)
        tmp.put("sign_with", JSONArray(listOf("user", "green-backend").toTypedArray()) )
        val json = JsonDeserializer.parseToJsonElement(tmp.toString())
        val updatedTx = JsonDeserializer.decodeFromJsonElement<CreateTransaction>(json).let {
            if (it.keepJsonElement()) {
                it.jsonElement = json
            }
            it
        }
        return updatedTx
    }

    fun switchExchangeRate(){
        exchangeRateDirection.toggle()
    }

    companion object: Loggable()
}
