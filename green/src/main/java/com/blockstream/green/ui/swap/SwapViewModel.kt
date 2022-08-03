package com.blockstream.green.ui.swap;

import androidx.lifecycle.*
import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.green.data.Countly
import com.blockstream.green.data.EnrichedAsset
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.logException
import com.blockstream.green.extensions.toggle
import com.blockstream.green.gdk.TwoFactorResolver
import com.blockstream.green.gdk.assetTicker
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import mu.KLogging
import org.json.JSONArray
import org.json.JSONObject


class SwapViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted proposal: SwapProposal?,
) : AbstractSwapWalletViewModel(sessionManager, walletRepository, countly, wallet, proposal) {

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

    private var _remoteAssetsLiveData = MutableLiveData<Map<String, EnrichedAsset>>(mapOf())
    val remoteAssetsLiveData: LiveData<Map<String, EnrichedAsset>> get() = _remoteAssetsLiveData

    var remoteAssets
        get() = remoteAssetsLiveData.value!!
        set(value) {
            _remoteAssetsLiveData.value = value
        }

    private var _toAssetIdLiveData = MutableLiveData<String>()
    val toAssetIdLiveData: LiveData<String> get() = _toAssetIdLiveData

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

    val enabledAccounts = session.accounts.filter { it.isMultisig && it.isLiquid && it.type == AccountType.STANDARD }

    init {
        proposal ?: run {
            viewModelScope.launch(context = logException(countly)) {
                utxos = withContext(context = Dispatchers.IO) {
                    session.getUnspentOutputs(enabledAccounts).unspentOutputs.values.flatten()
                        .sortedWith(session::sortUtxos)
                }

                utxo = utxos.firstOrNull()
            }
        }

        utxoLiveData.asFlow().onEach {
            updateToAssets()

        }.launchIn(viewModelScope)

        session.enrichedAssetsFlow.onEach {
            remoteAssets = it
            updateToAssets()
        }.launchIn(lifecycleScope)

        combine(utxoLiveData.asFlow(), toAssetIdLiveData.asFlow(), exchangeRateDirection.asFlow(), toAmount.asFlow()) { _, _, _, _ ->
            Unit
        }.debounce(50).onEach {

            doUserAction({
                // Convert utxo satoshi values
                val utxoAmount = if(utxo?.assetId.isPolicyAsset(session)){
                    session.convertAmount(
                        utxo?.assetId,
                        Convert(satoshi = utxo!!.satoshi)
                    )!!.valueInMainUnit
                }else{
                    session.convertAmount(
                        utxo?.assetId,
                        Convert(satoshi = utxo!!.satoshi, session.getAsset(utxo!!.assetId)),
                        isAsset = true
                    )!!.valueInMainUnit
                }

                logger.info { "utxoAmount $utxoAmount" }

                // Make conversion for BTC values
                val toAmount = UserInput.parseUserInput(
                    session = session,
                    input = toAmount.value!!,
                    assetId = toAssetId!!,
                    isFiat = false
                ).getBalance(session)!!.valueInMainUnit

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

        }.launchIn(viewModelScope)

        viewModelScope.launch {
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

        doUserAction({
            val utxo = proposal.inputs.first()
            val out = proposal.outputs.first()
            val utxoAmount = utxo.amount.toAmountLook(session, utxo.assetId,
                isFiat = false,
                withUnit = false
            )
            val outAmount = out.amount.toAmountLook(session, out.assetId,
                isFiat = false,
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
            (session.walletAssetsFlow.value.keys + remoteAssets.filterValues { !it.isAmp }.keys).filter { it != utxo?.assetId && it != BTC_POLICY_ASSET }
                .toList()

        if (toAssetId.isNullOrBlank() || toAssetId == utxo?.assetId) {
            toAssetId = toAssets.firstOrNull()
        }
    }

    fun createSwapProposal(twoFactorResolver: TwoFactorResolver) {
        doUserAction({
            val satoshi = UserInput.parseUserInput(
                session = session,
                input = toAmount.value!!,
                assetId = toAssetId!!,
                isFiat = false
            ).getBalance(session)!!.satoshi

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
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(proposal)))
        })
    }

    fun completeSwapProposal(twoFactorResolver: TwoFactorResolver) {
        if (proposal == null) {
            return
        }

        doUserAction({
            val v0List= LiquiDexV0List(
                proposals = listOf(proposal!!)
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
                subaccount = enabledAccounts.first().pointer,
                addressees = null,
                sendAll = false,
                feeRate = null,
                utxos = null
            )

            session.pendingTransaction = params to tx
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(tx)))
        })
    }

    private fun updateTransaction(tx: CreateTransaction) : CreateTransaction {
        val string = tx.jsonElement!!.toString()
        val tmp = JSONObject(string)
        tmp.put("sign_with", JSONArray(listOf("user", "green-backend").toTypedArray()) )
        val json = GdkBridge.JsonDeserializer.parseToJsonElement(tmp.toString())
        val updatedTx = GdkBridge.JsonDeserializer.decodeFromJsonElement<CreateTransaction>(json).let {
            if (it is GAJson<*> && it.keepJsonElement) {
                it.jsonElement = json
            }
            it
        }
        return updatedTx
    }

    fun switchExchangeRate(){
        exchangeRateDirection.toggle()
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            proposal: SwapProposal?
        ): SwapViewModel
    }

    companion object : KLogging() {

        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            proposal: SwapProposal?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, proposal) as T
            }
        }
    }
}
