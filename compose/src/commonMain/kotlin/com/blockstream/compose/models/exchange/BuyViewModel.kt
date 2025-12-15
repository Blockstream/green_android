package com.blockstream.compose.models.exchange

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_too_high
import blockstream_green.common.generated.resources.id_amount_too_low
import blockstream_green.common.generated.resources.id_buy
import blockstream_green.common.generated.resources.id_connecting_to_1d
import blockstream_green.common.generated.resources.id_error
import blockstream_green.common.generated.resources.id_please_select_your_correct_billing
import blockstream_green.common.generated.resources.id_select_your_region
import blockstream_green.common.generated.resources.id_something_went_wrong
import blockstream_green.common.generated.resources.id_the_address_is_valid
import blockstream_green.common.generated.resources.id_verify_address
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.OpenBrowserType
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.data.gdk.data.Address
import com.blockstream.data.managers.LocaleManager
import com.blockstream.data.utils.UserInput
import com.blockstream.domain.hardware.VerifyAddressUseCase
import com.blockstream.domain.meld.GetLastSuccessfulPurchaseExchange
import com.blockstream.domain.meld.MeldUseCase
import com.blockstream.utils.Loggable
import com.blockstream.network.dataOrThrow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class BuyViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null
) : CreateTransactionViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = accountAssetOrNull
) {
    override fun screenName(): String = "Buy"

    internal val meldUseCase: MeldUseCase by inject()
    internal val verifyAddressUseCase: VerifyAddressUseCase by inject()
    private val localeManager: LocaleManager by inject()
    internal val getLastSuccessfulPurchaseExchange: GetLastSuccessfulPurchaseExchange by inject()

    abstract val showRecoveryConfirmation: StateFlow<Boolean>
    abstract val showAccountSelector: StateFlow<Boolean>
    abstract val amount: MutableStateFlow<String>
    abstract val amountHint: StateFlow<String?>

    abstract val suggestedAmounts: StateFlow<List<String>>

    internal val country = MutableStateFlow(
        (settingsManager.getCountry() ?: localeManager.getCountry() ?: "US").uppercase()
    )

    internal val userPickedQuote = MutableStateFlow(false)

    internal val address = MutableStateFlow<Address?>(null)

    internal val _quote = MutableStateFlow<com.blockstream.data.meld.data.QuoteResponse?>(null)
    val quote: StateFlow<com.blockstream.data.meld.data.QuoteResponse?> = _quote

    internal val quotes = MutableStateFlow<List<com.blockstream.data.meld.data.QuoteResponse>>(emptyList())

    internal val limits = MutableStateFlow<List<String>>(emptyList())

    internal val _onProgressQuote = MutableStateFlow(false)
    val onProgressQuote: StateFlow<Boolean> = _onProgressQuote

    internal val _onProgressBuy = MutableStateFlow(false)
    val onProgressBuy: StateFlow<Boolean> = _onProgressBuy

    fun changeCountry(country: com.blockstream.data.meld.models.Country) {
        this.country.value = country.countryCode.uppercase().also {
            settingsManager.setCountry(it)
        }
    }

    fun changeQuote(quote: com.blockstream.data.meld.data.QuoteResponse? = null) {
        if (quote == null) {
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.BuyQuotes(
                        greenWallet = greenWallet,
                        quotes = com.blockstream.data.meld.data.QuotesResponse(quotes = quotes.value),
                        selectedServiceProvider = this._quote.value?.serviceProvider
                    )
                )
            )
        } else {
            _quote.value = quote
            userPickedQuote.value = true
        }
    }

    fun buy() {
        doAsync({
            meldUseCase.createCryptoWidgetUseCase(
                cryptoQuote = quote.value!!,
                address = address.value!!.address,
                greenWallet = greenWallet
            ).dataOrThrow().widgetUrl
        }, preAction = {
            onProgress.value = true
            _onProgressBuy.value = true
            onProgressDescription.value =
                getString(Res.string.id_connecting_to_1d, quote.value?.serviceProvider ?: "")
        }, postAction = {
            onProgress.value = false
            _onProgressBuy.value = false
            onProgressDescription.value = null
            countly.buyRedirect()
        }, onSuccess = {
            countly.buyRedirect()
            postSideEffect(SideEffects.OpenBrowser(url = it, type = OpenBrowserType.MELD))
        })
    }

    fun changeAccount() {
        session.accounts.value.filter { it.isBitcoin }.also { accounts ->
            postEvent(
                NavigateDestinations.Accounts(
                    greenWallet = greenWallet,
                    accounts = AccountAssetBalanceList(accounts.map {
                        it.accountAssetBalance
                    }),
                    withAsset = false
                )
            )
        }
    }
}

class BuyViewModel(greenWallet: GreenWallet, initialAccountAsset: AccountAsset? = null) :
    BuyViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = initialAccountAsset) {

    private val hideWalletBackupAlert = MutableStateFlow(false)

    override val showRecoveryConfirmation: StateFlow<Boolean> =
        combine(greenWalletFlow, hideWalletBackupAlert) { greenWallet, hideWalletBackupAlert ->
            !hideWalletBackupAlert && greenWallet?.isRecoveryConfirmed == false
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    override val showAccountSelector: StateFlow<Boolean> = session.ifConnected {
        MutableStateFlow(session.accounts.value.filter { it.isBitcoin }.size > 1)
    } ?: MutableStateFlow(false)

    private val _suggestedAmounts = MutableStateFlow(emptyList<String>())
    override val suggestedAmounts: StateFlow<List<String>> = _suggestedAmounts

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    private val _amountHint = MutableStateFlow<String?>(null)
    override val amountHint = _amountHint

    init {
        // Default to Fiat denomination
        _denomination.value = Denomination.defaultOrFiat(session, isFiat = true)


        viewModelScope.launch {
            getLastSuccessfulPurchaseExchange(GetLastSuccessfulPurchaseExchange.Params(greenWallet.xPubHashId))
        }

        session.ifConnected {
            val accounts = session.accounts.value.filter { it.isBitcoin }

            if (accounts.isEmpty()) {
                postSideEffect(
                    SideEffects.NavigateBack(
                        title = StringHolder(stringResource = Res.string.id_error),
                        message = StringHolder(string = "You don't have a Bitcoin account")
                    )
                )
            } else {
                val activeAccountId =
                    (accountAsset.value?.account?.id ?: session.activeAccount.value?.id)
                accountAsset.value = accounts.find { it.id == activeAccountId }?.accountAsset
                    ?: accounts.first().accountAsset
            }

            combine(country, session.settings().filterNotNull()) { country, settings ->
                updateNavData(country)
                _suggestedAmounts.value = meldUseCase.defaultValuesUseCase(settings.pricing.currency)
            }.launchIn(this)
        }


        combine(
            quotes,
            getLastSuccessfulPurchaseExchange.observe()
        ) { quotesList: List<com.blockstream.data.meld.data.QuoteResponse>, lastProvider: String? ->
            _quote.value = quote.value?.takeIf { userPickedQuote.value }?.let { quote ->
                // keep the same provider if user picked it
                quotesList.find {
                    it.serviceProvider == quote.serviceProvider
                }
            } ?: lastProvider?.let { provider ->
                // otherwise, try to find the last successful provider
                quotesList.find { it.serviceProvider == provider }
            } ?: quotesList.firstOrNull()?.also {
                // fallback to first available quote
                userPickedQuote.value = false
            }
        }.launchIn(this)


        session.ifConnected {
            combine(amount, country) { amount, _ ->
                updateQuotes(amount)
            }.launchIn(this)
        }

        combine(quote, address) { quote, address ->
            _isValid.value = quote != null && address != null
        }.launchIn(this)

        accountAsset.filterNotNull().onEach {
            doAsync({
                session.getReceiveAddress(it.account)
            }, onSuccess = {
                address.value = it
            }, onError = {
                address.value = null
                postSideEffect(SideEffects.ErrorDialog(error = it, supportData = errorReport(it)))
            })
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is Events.DismissWalletBackupAlert -> {
                viewModelScope.launch {
                    hideWalletBackupAlert.value = true
                }
            }
        }
    }

    private suspend fun updateNavData(country: String) {
        _navData.value = NavData(
            title = getString(Res.string.id_buy), actions = listOfNotNull(
                NavAction(
                    title = country,
                    onClick = {
                        viewModelScope.launch {
                            postEvent(
                                NavigateDestinations.MeldCountries(
                                    greenWallet = greenWallet,
                                    title = getString(Res.string.id_select_your_region),
                                    subtitle = getString(Res.string.id_please_select_your_correct_billing),
                                )
                            )
                        }
                    }
                ),
                NavAction(
                    title = getString(Res.string.id_verify_address),
                    isMenuEntry = true,
                    onClick = {
                        verifyAddressOnDevice()
                    }
                ).takeIf { session.isHardwareWallet }
            )
        )
    }

    private fun verifyAddressOnDevice() {
        doAsync({
            val address = address.value!!

            // Display DeviceInteraction
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.DeviceInteraction(
                        greenWalletOrNull = greenWalletOrNull,
                        deviceId = sessionOrNull?.device?.connectionIdentifier,
                        verifyAddress = address.address
                    )
                )
            )

            verifyAddressUseCase.invoke(
                session = session,
                account = account,
                address = address
            )
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_the_address_is_valid)))
            postSideEffect(SideEffects.Dismiss)
        }, onError = {
            postSideEffect(SideEffects.ErrorDialog(it))
            postSideEffect(SideEffects.Dismiss)
        })
    }

    private var updateQuotesJob: Job? = null

    private suspend fun mapMeldErrorToUserFriendly(errorMessage: String?): String {
        return when (errorMessage) {
            "INVALID_AMOUNT_TOO_LOW" -> getString(Res.string.id_amount_too_low)
            "INVALID_AMOUNT_TOO_HIGH" -> getString(Res.string.id_amount_too_high)
            else ->  getString(Res.string.id_something_went_wrong)
        }
    }

    private fun updateQuotes(amount: String) {
        updateQuotesJob?.cancel()
        updateQuotesJob = doAsync(
            {
                if (amount.isBlank()) {
                    null
                } else {
                    meldUseCase.createCryptoQuoteUseCase(
                        session = session,
                        country = country.value,
                        enrichedAsset = accountAsset.value!!.asset,
                        amount = amount,
                        denomination = denomination.value,
                        greenWallet = greenWallet
                    ).dataOrThrow()
                }
            },
            preAction = {
                onProgress.value = true
                _onProgressQuote.value = true
            }, postAction = {
                onProgress.value = false
                _onProgressQuote.value = false
            },
            onSuccess = {
                quotes.value = it ?: emptyList()
                _error.value = null
            }, onError = {
                viewModelScope.launch {
                    _error.value = mapMeldErrorToUserFriendly(it.message)
                }
                quotes.value = emptyList()
            }
        )
    }

    override suspend fun denominatedValue(): DenominatedValue? {
        return accountAsset.value?.assetId?.let { assetId ->
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

    companion object : Loggable()
}

class BuyViewModelPreview(greenWallet: GreenWallet) :
    BuyViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = null) {

    override val showRecoveryConfirmation: StateFlow<Boolean> = MutableStateFlow(false)
    override val showAccountSelector: StateFlow<Boolean> = MutableStateFlow(true)

    override val amount: MutableStateFlow<String> = MutableStateFlow("500")
    override val amountHint: StateFlow<String?> = MutableStateFlow(null)
    override val suggestedAmounts: StateFlow<List<String>> =
        MutableStateFlow(listOf("200", "400", "800"))

    init {
        _denomination.value = Denomination.FIAT("USD")

        accountAsset.value = previewAccountAsset()

        com.blockstream.data.meld.data.QuoteResponse(
            transactionType = "",
            sourceAmount = "1.0",
            sourceAmountWithoutFees = "1.0",
            fiatAmountWithoutFees = "1.0",
            sourceCurrencyCode = "EUR",
            countryCode = "GR",
            totalFee = "1.0",
            transactionFee = "1.0",
            destinationAmount = "1.0",
            destinationCurrencyCode = "USD",
            exchangeRate = "1.0",
            paymentMethodType = "CASH",
            customerScore = "1.0",
            serviceProvider = "Service Provider",
        ).also {
            _quote.value = it
            quotes.value = listOf(it)
        }
    }

    companion object {
        fun preview() = BuyViewModelPreview(previewWallet())
    }
}