package com.blockstream.common.models.exchange

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_buy
import blockstream_green.common.generated.resources.id_type_an_amount_between_s_and_s
import breez_sdk.SwapInfo
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.extensions.tryCatchNullSuspend
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.lightning.satoshi
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.sideeffects.OpenBrowserType
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toAmountLookOrNa
import com.blockstream.domain.meld.CreateCryptoQuoteUseCase
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class OnOffRampsViewModelAbstract(
    greenWallet: GreenWallet
) : CreateTransactionViewModelAbstract(
    greenWallet = greenWallet
) {
    override fun screenName(): String = "OnOffRamps"

    @NativeCoroutinesState
    abstract val showRecoveryConfirmation: StateFlow<Boolean>

    abstract val buyAsset: MutableStateFlow<AssetBalance?>

    abstract val buyAccount: MutableStateFlow<AccountAssetBalance?>

    abstract val buyAssets: StateFlow<List<AssetBalance>>

    abstract val buyAccounts: StateFlow<List<AccountAssetBalance>>

    abstract val isBuy: MutableStateFlow<Boolean>

    abstract val amount: MutableStateFlow<String>
    abstract val amountExchange: StateFlow<String>
    abstract val amountError: StateFlow<String?>
    abstract val amountHint: StateFlow<String?>

    abstract val isSandboxEnvironment: MutableStateFlow<Boolean>
}

class OnOffRampsViewModel(greenWallet: GreenWallet) :
    OnOffRampsViewModelAbstract(greenWallet = greenWallet) {

    private val createCryptoQuoteUseCase: CreateCryptoQuoteUseCase by inject()

    private val hideWalletBackupAlert = MutableStateFlow(false)

    override val showRecoveryConfirmation: StateFlow<Boolean> =
        combine(greenWalletFlow, hideWalletBackupAlert) { greenWallet, hideWalletBackupAlert ->
            !hideWalletBackupAlert && greenWallet?.isRecoveryConfirmed == false
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    override val isSandboxEnvironment: MutableStateFlow<Boolean> =
        MutableStateFlow(appInfo.isDevelopment)

    private val availableBuyAssets = session.ifConnected {
        listOfNotNull(
            session.bitcoin?.policyAsset,
            // session.liquid?.policyAsset.takeIf { appInfo.isDevelopmentOrDebug },
        ).map {
            AssetBalance.create(EnrichedAsset.create(session = session, assetId = it))
        }
    } ?: listOf()

    override val isBuy: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override val buyAssets: StateFlow<List<AssetBalance>> = MutableStateFlow(availableBuyAssets)

    private val _buyAccounts: MutableStateFlow<List<AccountAssetBalance>> =
        MutableStateFlow(listOf())
    override val buyAccounts: StateFlow<List<AccountAssetBalance>> = _buyAccounts

    override val buyAsset: MutableStateFlow<AssetBalance?> =
        MutableStateFlow(availableBuyAssets.firstOrNull())

    override val buyAccount: MutableStateFlow<AccountAssetBalance?> =
        MutableStateFlow(session.ifConnected {
            (listOfNotNull(session.activeAccount.value) + session.accounts.value).firstOrNull { it.network.policyAsset == buyAsset.value?.assetId }?.accountAssetBalance
        })

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    private val _amountExchange = MutableStateFlow("")
    override val amountExchange: StateFlow<String> = _amountExchange

    private val _amountError = MutableStateFlow<String?>(null)
    override val amountError = _amountError

    private val _amountHint = MutableStateFlow<String?>(null)
    override val amountHint = _amountHint

    private var swapInfo: SwapInfo? = null

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_buy))
        }

        session.ifConnected {

            // Check if account match the assetId
            buyAccount.onEach {
                if (it?.assetId != buyAsset.value?.assetId) {
                    buyAccount.value = null
                }
            }.launchIn(this)

            buyAsset.onEach { buyAsset ->
                _buyAccounts.value =
                    session.accounts.value.filter { it.network.policyAsset == buyAsset?.assetId }
                        .map {
                            it.accountAssetBalance
                        }

                if (buyAccount.value?.account?.network?.policyAsset != buyAsset?.assetId) {
                    buyAccount.value = _buyAccounts.value.firstOrNull()
                }

                // Clear amount if asset changed
                amount.value = ""

            }.launchIn(this)

            combine(buyAsset, buyAccount, amountError) { buyAsset, buyAccount, amountError ->
                _isValid.value = buyAsset != null && buyAccount != null && amountError == null
            }.launchIn(this)

            combine(buyAsset, buyAccount, amount) { asset, account, amount ->

                tryCatchNullSuspend {
                    if(amount.isNotBlank()) {
//                        val quote = createCryptoQuoteUseCase(cryptoQuote = CryptoQuoteRequest(
//                            sourceAmount = amount,
//                            sourceCurrencyCode = session.settings().value?.pricing?.currency ?: "USD",
//                            destinationCurrencyCode = asset?.asset?.ticker ?: "BTC"
//                        )
//                        ).first()

//                        logger.d { "$quote" }

//                        _amountExchange.value = session.convert(
//                            assetId = BTC_POLICY_ASSET,
//                            asString = quote.destinationAmount.toString(),
//                            denomination = BTC_UNIT
//                        )?.toAmountLook(
//                            session = session,
//                            assetId = asset?.asset?.assetId,
//                            denomination = Denomination.exchange(session, denomination.value),
//                            withUnit = true,
//                            withGrouping = true,
//                            withMinimumDigits = false
//                        ) ?: ""

                    }
                }


                // TODO support sellAsset
                val balance = asset?.assetId?.takeIf { it.isPolicyAsset(session) }?.let { assetId ->
                    UserInput.parseUserInputSafe(
                        session = session,
                        input = amount,
                        assetId = assetId,
                        denomination = denomination.value
                    ).getBalance()
                }

                _amountExchange.value = balance?.let {
                    "≈ " + it.toAmountLook(
                        session = session,
                        assetId = asset.assetId,
                        denomination = Denomination.exchange(session, denomination.value),
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                } ?: ""

                var maxValue: Long? = null
                var minValue: Long? = null

                val isLightning = account?.account?.isLightning == true
                var isError = false
                var hintOrError: String? = null

                if (isLightning) {
                    // Cache SwapInfo
                    if (swapInfo == null) {
                        swapInfo = tryCatch(context = Dispatchers.Default) {
                            session.receiveOnchain()
                        }
                    }

                    maxValue = swapInfo?.maxAllowedDeposit
                    minValue = swapInfo?.minAllowedDeposit

                    hintOrError = swapInfo?.let {
                        getString(
                            Res.string.id_type_an_amount_between_s_and_s,
                            it.minAllowedDeposit.toAmountLookOrNa(
                                session = session,
                                assetId = asset?.assetId,
                                denomination = denomination.value,
                                withUnit = true,
                            ),
                            it.maxAllowedDeposit.toAmountLookOrNa(
                                session = session,
                                assetId = asset?.assetId,
                                denomination = denomination.value,
                                withUnit = true
                            ),
                            it.channelOpeningFees?.minMsat?.satoshi()?.toAmountLook(
                                session = session,
                                assetId = asset?.assetId,
                                withUnit = true
                            ) ?: "-"
                        )
                    }

                    isError =
                        (balance?.satoshi?.let { it >= (minValue ?: 0) && it < (maxValue ?: 0) }
                            ?: true).not()
                }

                if (isError) {
                    _amountError.value = hintOrError
                    _amountHint.value = null
                } else {
                    _amountError.value = null
                    _amountHint.value = hintOrError
                }

            }.launchIn(this)

            // Default to Fiat denomination
            _denomination.value = Denomination.defaultOrFiat(session, isFiat = true)
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when(event) {
            is Events.DismissWalletBackupAlert -> {
                viewModelScope.launch {
                    hideWalletBackupAlert.value = true
                }
            }
            is Events.Continue -> {
                redirectToRamps()
            }
        }
    }

    private fun redirectToRamps() {
        doAsync({

            val meldKey =
                if (isSandboxEnvironment.value) MELD_DEVELOPMENT_KEY else MELD_PRODUCTION_KEY

            if (isBuy.value) {
                val address = session.getReceiveAddressAsString(buyAccount.value!!.account)
                val ticker = buyAsset.value?.asset?.ticker(session)

                val balance = amount.value.takeIf { it.isNotBlank() }?.let {
                    UserInput.parseUserInputSafe(
                        session = session,
                        input = it,
                        assetId = buyAsset.value?.assetId,
                        denomination = denomination.value
                    ).getBalance()
                }

                val buyAmountInFiat = balance?.fiat ?: "200"
                val buyAmountFiatCurrency = balance?.fiatCurrency

                "${meldUrl(isSandboxEnvironment = isSandboxEnvironment.value)}/?publicKey=$meldKey&walletAddressLocked=$address&destinationCurrencyCodeLocked=$ticker&sourceAmount=$buyAmountInFiat&sourceCurrencyCode=$buyAmountFiatCurrency&transactionType=BUY"
            } else {
                val ticker = buyAsset.value?.asset?.ticker
                "${meldUrl(isSandboxEnvironment = isSandboxEnvironment.value)}/?publicKey=$meldKey&destinationCurrencyCodeLocked=$ticker&transactionType=SELL"
            }

        }, onSuccess = {
            countly.buyRedirect()
            postSideEffect(SideEffects.OpenBrowser(url = it, type = OpenBrowserType.OPEN_SYSTEM))
        })
    }

    override suspend fun denominatedValue(): DenominatedValue? {
        // TODO support sellAsset
        return buyAsset.value?.assetId?.let { assetId ->
            UserInput.parseUserInputSafe(
                session = session,
                input = amount.value,
                denomination = denomination.value,
                assetId = assetId
            ).getBalance().let {
                DenominatedValue(
                    balance = it,
                    assetId = assetId,
                    denomination = denomination.value
                )
            }
        }
    }

    override fun setDenominatedValue(denominatedValue: DenominatedValue) {
        _denomination.value = denominatedValue.denomination
        amount.value = denominatedValue.asInput ?: ""
    }

    companion object : Loggable() {
        private const val MELD_PRODUCTION = "https://ramps.blockstream.com"
        private const val MELD_SANDBOX = "https://ramps-sb.blockstream.com"

        // TODO
        private const val MELD_PRODUCTION_KEY =
            "WXDgw7kt8bwb7xTUXv1zMq:7Cp7PgRXHgui27QX5cLKcmMsA3GybZ"
        private const val MELD_DEVELOPMENT_KEY =
            "WQ55yQMaD8BCbULXdrMwUU:MexHFod7bds1SUTpWC86wUaVqWeqGbebBb"

        fun meldUrl(isSandboxEnvironment: Boolean) =
            if (isSandboxEnvironment) MELD_SANDBOX else MELD_PRODUCTION
    }
}

class OnOffRampsViewModelPreview(greenWallet: GreenWallet) :
    OnOffRampsViewModelAbstract(greenWallet = greenWallet) {

    override val showRecoveryConfirmation: StateFlow<Boolean> = MutableStateFlow(false)

    override val isSandboxEnvironment: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override val buyAsset: MutableStateFlow<AssetBalance?> =
        MutableStateFlow(AssetBalance.create(EnrichedAsset.PreviewBTC))
    override val buyAccount: MutableStateFlow<AccountAssetBalance?> = MutableStateFlow(
        previewAccountAssetBalance()
    )

    override val buyAssets: StateFlow<List<AssetBalance>> = MutableStateFlow(
        listOf(
            AssetBalance.create(EnrichedAsset.PreviewBTC),
            // AssetBalance.create(EnrichedAsset.PreviewLBTC)
        )
    )

    override val buyAccounts: StateFlow<List<AccountAssetBalance>> = MutableStateFlow(listOf())

    override val isBuy: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val amount: MutableStateFlow<String> = MutableStateFlow("0.1")
    override val amountExchange: StateFlow<String> = MutableStateFlow("0.1 USD")
    override val amountError: StateFlow<String?> = MutableStateFlow("Error")
    override val amountHint: StateFlow<String?> = MutableStateFlow(null)

    companion object {
        fun preview() = OnOffRampsViewModelPreview(previewWallet())
    }
}