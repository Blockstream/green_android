package com.blockstream.common.models.home

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.green_shield
import blockstream_green.common.generated.resources.id_about
import blockstream_green.common.generated.resources.id_app_settings
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Sliders
import com.blockstream.common.Urls
import com.blockstream.common.data.Banner
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

abstract class HomeViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String? = "Home"

    @NativeCoroutinesState
    abstract val isEmptyWallet: StateFlow<Boolean?>

    @NativeCoroutinesState
    abstract val allWallets: StateFlow<List<WalletListLook>?>
}

class HomeViewModel(val isGetStarted: Boolean = false) : HomeViewModelAbstract() {

    class LocalEvents {
        object GetStarted : Event
        object ConnectJade : Event
        class SelectWallet(val greenWallet: GreenWallet) : Event
        class ClickTermsOfService : Events.OpenBrowser(Urls.TERMS_OF_SERVICE)
        class ClickPrivacyPolicy : Events.OpenBrowser(Urls.PRIVACY_POLICY)
    }

    private var _activeEvent: Event? = null

    init {
        viewModelScope.launch(context = logException()) {
            // Update Remote Config when app is initiated, that's the easiest way
            countly.updateRemoteConfig(force = false)
        }

        viewModelScope.launch {
            _navData.value = NavData(
                actions = listOfNotNull(
                    NavAction(
                        titleRes = Res.string.id_app_settings,
                        imageVector = PhosphorIcons.Regular.Sliders,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.AppSettings))
                    },
                    NavAction(
                        titleRes = Res.string.id_about,
                        icon = Res.drawable.green_shield,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.About))
                    }
                )
            )
        }

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(this)

        bootstrap()
    }

    override val isEmptyWallet: StateFlow<Boolean?> =
        (if (isGetStarted) flowOf(true) else database.walletsExistsFlow().map {
            !it
        }).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    @NativeCoroutinesState
    override val allWallets = combine(
        database.getAllWalletsFlow(),
        sessionManager.ephemeralWallets,
        sessionManager.connectionChangeEvent
    ) { wallets, ephemeralWallets, _ ->
        wallets + ephemeralWallets
    }.map {
        it.map { greenWallet ->
            WalletListLook.create(greenWallet, sessionManager)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.SelectWallet -> {
                viewModelScope.launch {
                    selectWallet(event.greenWallet)
                }
            }

            is Events.Continue -> {
                _activeEvent?.also {
                    handleActions(it)
                }
            }

            is LocalEvents.GetStarted -> {
                handleActions(event)
            }

            is LocalEvents.ConnectJade -> {
                countly.hardwareWallet()
                handleActions(event)
            }
        }
    }

    private suspend fun selectWallet(wallet: GreenWallet) {
        val session: GdkSession = sessionManager.getWalletSessionOrCreate(wallet)

        if (session.isConnected) {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(wallet)))
        } else if (wallet.isHardware && !wallet.isWatchOnly && database.getLoginCredential(
                wallet.id,
                CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
            ) == null
        ) {
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.DeviceScan(
                        greenWallet = wallet
                    )
                )
            )
        } else {
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.Login(
                        greenWallet = wallet,
                        autoLoginWallet = true
                    )
                )
            )
        }
    }

    private fun handleActions(event: Event) {
        _activeEvent = event

        when (event) {
            is LocalEvents.GetStarted -> {
                if (handleConsentDialog()) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetupNewWallet))
                }
            }

            is LocalEvents.ConnectJade -> {
                if (handleConsentDialog()) {
                    settingsManager.setDeviceTermsAccepted()
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.UseHardwareDevice))
                }
            }
        }
    }

    private fun handleConsentDialog(): Boolean {
        return shouldShowConsentDialog().also {
            if (it) {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Analytics))
            }
        }.let { !it }
    }

    private fun shouldShowConsentDialog(): Boolean {
        return settingsManager.analyticsFeatureEnabled && (!settingsManager.isAskedAboutAnalyticsConsent() && !settingsManager.getApplicationSettings().analytics)
    }

    companion object : Loggable()
}

class HomeViewModelPreview(
    softwareWallets: List<WalletListLook>,
    ephemeralWallets: List<WalletListLook>,
    hardwareWallets: List<WalletListLook>,
) : HomeViewModelAbstract() {
    override val isEmptyWallet: StateFlow<Boolean?> = MutableStateFlow(
        viewModelScope,
        softwareWallets.isEmpty() && ephemeralWallets.isEmpty() && hardwareWallets.isEmpty()
    )

    @NativeCoroutinesState
    override val allWallets: StateFlow<List<WalletListLook>?> =
        MutableStateFlow(viewModelScope, softwareWallets)

//    @NativeCoroutinesState
//    override val softwareWallets: StateFlow<List<WalletListLook>?> =
//        MutableStateFlow(viewModelScope, softwareWallets)
//
//    @NativeCoroutinesState
//    override val ephemeralWallets: StateFlow<List<WalletListLook>> =
//        MutableStateFlow(viewModelScope, ephemeralWallets)
//
//    @NativeCoroutinesState
//    override val hardwareWallets: StateFlow<List<WalletListLook>> =
//        MutableStateFlow(viewModelScope, hardwareWallets)

    init {
        banner.value = Banner.preview3
        promo.value = Promo.preview6
    }

    companion object {
        fun previewEmpty() = HomeViewModelPreview(
            listOf(), listOf(), listOf()
        )

        fun previewSoftwareOnly() = HomeViewModelPreview(
            listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false)
            ), listOf(), listOf()
        )

        fun previewHardwareOnly() = HomeViewModelPreview(
            listOf(),
            listOf(),
            listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false)
            )
        )

        fun previewAll() = HomeViewModelPreview(
            listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false)
            ), listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = true),
            ), listOf(
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true)
            )
        )
    }
}