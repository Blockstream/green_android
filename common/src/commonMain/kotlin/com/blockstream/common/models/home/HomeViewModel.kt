package com.blockstream.common.models.home

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.blockstream_logo
import blockstream_green.common.generated.resources.id_about
import blockstream_green.common.generated.resources.id_app_settings
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Sliders
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.domain.navigation.NavigateToWallet
import com.blockstream.green.data.banner.Banner
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

abstract class HomeViewModelAbstract(val isGetStarted: Boolean = false) : GreenViewModel() {
    override fun screenName(): String? = if (isGetStarted) "GetStarted" else "Home"

    @NativeCoroutinesState
    abstract val isEmptyWallet: StateFlow<Boolean?>

    @NativeCoroutinesState
    abstract val allWallets: StateFlow<List<WalletListLook>?>

    @NativeCoroutinesState
    abstract val showV5Upgrade: StateFlow<Boolean>
}

class HomeViewModel(isGetStarted: Boolean = false) : HomeViewModelAbstract(isGetStarted = isGetStarted) {

    private val navigateToWallet: NavigateToWallet by inject()

    class LocalEvents {
        object GetStarted : Event
        object ConnectJade : Event
        object UpgradeV5 : Event
        class SelectWallet(val greenWallet: GreenWallet) : Event
        class ClickTermsOfService : Events.OpenBrowser(Urls.TERMS_OF_SERVICE)
        class ClickPrivacyPolicy : Events.OpenBrowser(Urls.PRIVACY_POLICY)
    }

    private var _activeEvent: Event? = null

    override val isEmptyWallet: StateFlow<Boolean?> =
        (if (isGetStarted) flowOf(true) else database.walletsExistsFlow().map {
            !it
        }).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val showV5Upgrade: StateFlow<Boolean> = combine(
        settingsManager.isV5UpgradedFlow(),
        isEmptyWallet.filterNotNull()
    ) { isV5Upgraded, isEmptyWallet ->
        if (!isV5Upgraded) {
            if (isEmptyWallet) {
                // Mark it as upgraded
                settingsManager.setV5Upgraded()
            }

            !isEmptyWallet
        } else false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

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

    init {
        viewModelScope.launch(context = logException()) {
            // Update Remote Config when app is initiated, that's the easiest way
            countly.updateRemoteConfig(force = false)
        }

        showV5Upgrade.onEach {
            _navData.value = NavData(
                isVisible = !it,
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
                        icon = Res.drawable.blockstream_logo,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.About))
                    }
                )
            )
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.SelectWallet -> {
                viewModelScope.launch {
                    postSideEffect(
                        SideEffects.NavigateTo(destination = navigateToWallet(event.greenWallet))
                    )
                }
            }

            is Events.Continue -> {
                _activeEvent?.also {
                    handleActions(it)
                }
            }

            is LocalEvents.UpgradeV5 -> {
                settingsManager.setV5Upgraded()
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

    private fun handleActions(event: Event) {
        _activeEvent = event

        when (event) {
            is LocalEvents.GetStarted -> {
                if (handleConsentDialog()) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetupNewWallet))
                    countly.getStarted()
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

    override val showV5Upgrade: StateFlow<Boolean> = MutableStateFlow(true)

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