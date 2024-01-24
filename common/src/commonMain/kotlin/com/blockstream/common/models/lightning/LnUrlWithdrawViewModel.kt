package com.blockstream.common.models.lightning

import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LnUrlWithdrawResult
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.lightning.domain
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.maxWithdrawableSatoshi
import com.blockstream.common.lightning.minWithdrawableSatoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.min


abstract class LnUrlWithdrawViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "LNURLWithdraw"

    @NativeCoroutinesState
    abstract val withdrawaLimits: StateFlow<String>

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amountCurrency: MutableStateFlow<String>

    abstract val amountIsEditable: Boolean

    @NativeCoroutinesState
    abstract val exchange: StateFlow<String>

    @NativeCoroutinesState
    abstract val description : MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val error: StateFlow<String?>

    abstract val redeemMessage: String
}

class LnUrlWithdrawViewModel(greenWallet: GreenWallet, val requestData: LnUrlWithdrawRequestData) :
    LnUrlWithdrawViewModelAbstract(greenWallet = greenWallet) {
    override val withdrawaLimits = MutableStateFlow("")

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    override val amountCurrency: MutableStateFlow<String> = MutableStateFlow(Denomination.default(session).unit(session = session))

    override val amountIsEditable: Boolean = requestData.minWithdrawableSatoshi() != requestData.maxWithdrawableSatoshi()

    override val exchange: MutableStateFlow<String> = MutableStateFlow("")

    override val description: MutableStateFlow<String> = MutableStateFlow(requestData.defaultDescription)

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override val redeemMessage = "id_you_are_redeeming_funds_from_s|${requestData.domain()}"

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue){
            withdraw()
        }
    }

    private var minWithdraw: Long = 0
    private var maxWithdraw: Long = 0

    init {
        bootstrap()

        logger.d { "LnUrlWithdrawRequestData: $requestData" }

        // Set amount if min/max is the same
        if (!amountIsEditable) {
            viewModelScope.coroutineScope.launch(context = logException(countly)) {
                amount.value = requestData.minWithdrawableSatoshi().toAmountLook(
                    session = session,
                    denomination = denomination.value,
                    withUnit = false,
                    withGrouping = false
                ) ?: ""
            }
        }

        amount.onEach {
            updateExchange()
            check()
        }.launchIn(viewModelScope.coroutineScope)

        denomination.onEach {
            amountCurrency.value = it.unit(session = session)
        }.launchIn(viewModelScope.coroutineScope)

        combine(session.lightningNodeInfoStateFlow, denomination) { nodeState, _ ->
            nodeState
        }.onEach {
            // Min
            minWithdraw = min(
                it.maxReceivableSatoshi(),
                requestData.minWithdrawableSatoshi()
            )

            // Max
            maxWithdraw = min(
                it.maxReceivableSatoshi(),
                requestData.maxWithdrawableSatoshi()
            )

            withdrawaLimits.value = "id_withdraw_limits_s__s|${minWithdraw}|${maxWithdraw}"
        }.launchIn(viewModelScope.coroutineScope)
    }

    private suspend fun amountInSatoshi() = if (amountIsEditable) {
        UserInput.parseUserInputSafe(
            session = session,
            input = amount.value,
            denomination = denomination.value
        ).getBalance()?.satoshi ?: 0
    } else {
        requestData.maxWithdrawableSatoshi()
    }

    private fun updateExchange() {
        amount.value.let { amount ->
            doAsync({
                UserInput.parseUserInputSafe(
                    session = session,
                    input = amount,
                    denomination = denomination.value
                ).toAmountLookOrEmpty(
                    denomination = Denomination.exchange(session = session, denomination = denomination.value),
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false).let {
                    if(it.isNotBlank()) "≈ $it" else it
                }
            }, preAction = null, postAction = null, onSuccess = {
                exchange.value = it
            }, onError = {
                exchange.value = ""
            })
        }
    }

    private suspend fun check(){
        val satoshi = amountInSatoshi()

        val balance = session.accountAssets(account = session.lightningAccount).value.policyAsset

        _error.value = if (satoshi <= 0L) {
            "id_invalid_amount"
        } else if (satoshi < minWithdraw) {
            "id_amount_must_be_at_least_s|${minWithdraw.toAmountLook(session = session, denomination = denomination.value)}"
        } else if (satoshi > balance) {
            "id_insufficient_funds"
        } else if (satoshi > maxWithdraw) {
            "id_amount_must_be_at_most_s|${maxWithdraw.toAmountLook(session = session, denomination = denomination.value)}"
        } else {
            null
        }
    }

    private fun withdraw() {
        doAsync({
            session.lightningSdk.withdrawLnUrl(requestData = requestData, amountInSatoshi(), description.value).also {
                if (it is LnUrlWithdrawResult.ErrorStatus) {
                    throw Exception(it.data.reason)
                }
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    override suspend fun denominatedValue(): DenominatedValue {
        return UserInput.parseUserInputSafe(
            session,
            amount.value,
            denomination = denomination.value
        ).getBalance().let {
            DenominatedValue(
                balance = it,
                assetId = BTC_POLICY_ASSET,
                denomination = denomination.value
            )
        }
    }

    override fun setDenominatedValue(denominatedValue: DenominatedValue) {
        amount.value = denominatedValue.asInput(session) ?: ""
        _denomination.value = denominatedValue.denomination
    }

    override fun errorReport(exception: Throwable): ErrorReport {
        return ErrorReport.create(throwable = exception, network = session.lightning, session = session)
    }

    companion object: Loggable()
}

class LnUrlWithdrawViewModelPreview(greenWallet: GreenWallet) :
    LnUrlWithdrawViewModelAbstract(greenWallet = greenWallet) {
    companion object {
        fun preview(): LnUrlWithdrawViewModelPreview {
            return LnUrlWithdrawViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }

    override val withdrawaLimits: StateFlow<String> = MutableStateFlow("")
    override val amount: MutableStateFlow<String> = MutableStateFlow("")
    override val amountCurrency: MutableStateFlow<String> = MutableStateFlow("")
    override val amountIsEditable: Boolean = false
    override val exchange: MutableStateFlow<String> = MutableStateFlow("")
    override val description: MutableStateFlow<String> = MutableStateFlow("")
    override val error: MutableStateFlow<String> = MutableStateFlow("")
    override val redeemMessage: String = ""

}