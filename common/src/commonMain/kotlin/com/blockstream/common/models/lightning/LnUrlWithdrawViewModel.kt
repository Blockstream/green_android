package com.blockstream.common.models.lightning

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_must_be_at_least_s
import blockstream_green.common.generated.resources.id_amount_must_be_at_most_s
import blockstream_green.common.generated.resources.id_insufficient_funds
import blockstream_green.common.generated.resources.id_invalid_amount
import blockstream_green.common.generated.resources.id_s_will_send_you_the_funds_it
import blockstream_green.common.generated.resources.id_success
import blockstream_green.common.generated.resources.id_withdraw
import blockstream_green.common.generated.resources.id_withdraw_limits_s__s
import blockstream_green.common.generated.resources.id_you_are_redeeming_funds_from_s
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LnUrlWithdrawResult
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SupportData
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.lightning.domain
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.maxWithdrawableSatoshi
import com.blockstream.common.lightning.minWithdrawableSatoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.toAmountLook
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.math.min

abstract class LnUrlWithdrawViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "LNURLWithdraw"

    @NativeCoroutinesState
    abstract val withdrawalLimits: StateFlow<String>

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    abstract val isAmountLocked: Boolean

    @NativeCoroutinesState
    abstract val exchange: StateFlow<String>

    @NativeCoroutinesState
    abstract val description: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val error: StateFlow<String?>

    abstract val redeemMessage: String
}

class LnUrlWithdrawViewModel(greenWallet: GreenWallet, val requestData: LnUrlWithdrawRequestData) :
    LnUrlWithdrawViewModelAbstract(greenWallet = greenWallet) {
    override val withdrawalLimits = MutableStateFlow("")

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    override val isAmountLocked: Boolean =
        requestData.minWithdrawableSatoshi() == requestData.maxWithdrawableSatoshi()

    override val exchange: MutableStateFlow<String> = MutableStateFlow("")

    override val description: MutableStateFlow<String> =
        MutableStateFlow(requestData.defaultDescription)

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override var redeemMessage = ""

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            withdraw()
        }
    }

    private var minWithdraw: Long = 0
    private var maxWithdraw: Long = 0

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_withdraw), subtitle = greenWallet.name)

            redeemMessage = getString(Res.string.id_you_are_redeeming_funds_from_s, requestData.domain())
        }

        if (appInfo.isDevelopmentOrDebug) {
            logger.d { "LnUrlWithdrawRequestData: $requestData" }
        }

        // Set amount if min/max is the same
        if (isAmountLocked) {
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
            tryCatch {
                updateExchange()
                check()
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(session.lightningSdk.nodeInfoStateFlow, denomination) { nodeState, denomination ->
            // Min
            minWithdraw = min(
                nodeState.maxReceivableSatoshi(),
                requestData.minWithdrawableSatoshi()
            )

            // Max
            maxWithdraw = min(
                nodeState.maxReceivableSatoshi(),
                requestData.maxWithdrawableSatoshi()
            )

            withdrawalLimits.value = getString(
                Res.string.id_withdraw_limits_s__s, minWithdraw.toAmountLook(
                    session = session,
                    withUnit = false,
                    denomination = denomination
                ) ?: "", maxWithdraw.toAmountLook(
                    session = session,
                    withUnit = true,
                    denomination = denomination
                ) ?: ""
            )
        }.launchIn(viewModelScope.coroutineScope)

        bootstrap()
    }

    private suspend fun amountInSatoshi() = if (isAmountLocked) {
        requestData.maxWithdrawableSatoshi()
    } else {
        UserInput.parseUserInputSafe(
            session = session,
            input = amount.value,
            denomination = denomination.value
        ).getBalance()?.satoshi
    }

    private fun updateExchange() {
        amount.value.let { amount ->
            doAsync({
                UserInput.parseUserInputSafe(
                    session = session,
                    input = amount,
                    denomination = denomination.value
                ).toAmountLookOrEmpty(
                    denomination = Denomination.exchange(
                        session = session,
                        denomination = denomination.value
                    ),
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false
                ).let {
                    if (it.isNotBlank()) "≈ $it" else it
                }
            }, preAction = null, postAction = null, onSuccess = {
                exchange.value = it
            }, onError = {
                exchange.value = ""
            })
        }
    }

    private suspend fun check() {
        val satoshi = amountInSatoshi()

        if (satoshi == null) {
            _error.value = null
            _isValid.value = false
        } else {
            val balance =
                session.accountAssets(account = session.lightningAccount).value.policyAsset

            _error.value = if (satoshi <= 0L) {
                getString(Res.string.id_invalid_amount)
            } else if (satoshi < minWithdraw) {
                getString(
                    Res.string.id_amount_must_be_at_least_s, minWithdraw.toAmountLook(
                        session = session,
                        denomination = denomination.value
                    ) ?: ""
                )
            } else if (satoshi > balance) {
                getString(Res.string.id_insufficient_funds)
            } else if (satoshi > maxWithdraw) {
                getString(
                    Res.string.id_amount_must_be_at_most_s, maxWithdraw.toAmountLook(
                        session = session,
                        denomination = denomination.value
                    ) ?: ""
                )
            } else {
                null
            }
            _isValid.value = _error.value == null
        }
    }

    private fun withdraw() {
        doAsync({
            session.lightningSdk.withdrawLnUrl(
                requestData = requestData,
                amount = amountInSatoshi() ?: throw Exception("No amount specified"),
                description = description.value
            ).also {
                if (it is LnUrlWithdrawResult.ErrorStatus) {
                    throw Exception(it.data.reason)
                }
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            postSideEffect(
                SideEffects.NavigateBack(
                    title = StringHolder.create(Res.string.id_success),
                    message = StringHolder(string = getString(Res.string.id_s_will_send_you_the_funds_it, requestData.domain()))
                )
            )
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

    override fun errorReport(exception: Throwable): SupportData {
        return SupportData.create(
            throwable = exception,
            network = session.lightning,
            session = session
        )
    }

    companion object : Loggable()
}

class LnUrlWithdrawViewModelPreview(greenWallet: GreenWallet) :
    LnUrlWithdrawViewModelAbstract(greenWallet = greenWallet) {

    override val withdrawalLimits: StateFlow<String> = MutableStateFlow("1 - 2 sats")
    override val amount: MutableStateFlow<String> = MutableStateFlow("")
    override val isAmountLocked: Boolean = false
    override val exchange: MutableStateFlow<String> = MutableStateFlow("")
    override val description: MutableStateFlow<String> = MutableStateFlow("")
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val redeemMessage: String = "id_you_are_redeeming_funds_from|blockstream.com"

    init {
        onProgress.value = true

        viewModelScope.launch {
            delay(3000)
            onProgress.value = false
        }
    }

    companion object {
        fun preview(): LnUrlWithdrawViewModelPreview {
            return LnUrlWithdrawViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }
}