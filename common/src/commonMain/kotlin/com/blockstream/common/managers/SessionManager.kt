package com.blockstream.common.managers

import com.blockstream.common.CountlyBase
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GASession
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.TorEvent
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.lightning.LightningBridge
import com.blockstream.common.lightning.LightningManager
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.Timer
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.annotation.Single
import kotlin.collections.set
import kotlin.properties.Delegates

class SessionManager constructor(
    appInfo: AppInfo,
    lifecycleManager: LifecycleManager,
    private val lightningManager: LightningManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private var countly: CountlyBase,
    private val gdk: Gdk,
    private val wally: Wally
) {
    private val userAgent = "${appInfo.userAgent}_${appInfo.version}_${appInfo.type}"

    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val httpRequestProvider : GdkSession by lazy {
        createSession()
    }

    private val torNetworkSession : GdkSession by lazy {
        createSession().also {
            it.torStatusFlow.onEach { torEvent ->
                _torProxyProgress.value = torEvent
            }.launchIn(scope)
        }
    }

    private var torEnabled : Boolean by Delegates.observable(settingsManager.appSettings.tor) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if (newValue) {
                startTorNetworkSessionIfNeeded()
            } else {
                _torProxy.value = null
                _torProxyProgress.value = TorEvent(progress = 100)
                torNetworkSession.disconnectAsync(LogoutReason.USER_ACTION)
            }
        }
    }

    private val _torProxyProgress : MutableStateFlow<TorEvent> = MutableStateFlow(TorEvent(progress = 100))
    val torProxyProgress
        get() = _torProxyProgress.asStateFlow()

    private val _torProxy : MutableStateFlow<String?> = MutableStateFlow(null)

    private val gdkSessions = mutableSetOf<GdkSession>()
    private val walletSessions = mutableMapOf<String, GdkSession>()
    private var onBoardingSession: GdkSession? = null

    private var timeoutTimers = mutableListOf<Timer>()

    val pendingUri: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _connectionChangeEvent = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).also {
        // Set initial value
        it.tryEmit(Unit)
    }

    val connectionChangeEvent
        get() = _connectionChangeEvent.asSharedFlow()

    private val _hardwareWallets = MutableStateFlow<List<GreenWallet>>(listOf())
    val hardwareWallets
        get() = _hardwareWallets.asStateFlow()

    private  val _ephemeralWallets = MutableStateFlow<List<GreenWallet>>(listOf())
    val ephemeralWallets
        get() = _ephemeralWallets.asStateFlow()

    init {
        lifecycleManager.lifecycleState.onEach { lifecycle ->
            if (lifecycle.isForeground()) {
                timeoutTimers.forEach { it.cancel() }
                timeoutTimers.clear()

                startTorNetworkSessionIfNeeded()
            } else {
                for (session in gdkSessions.filter { it.isConnected }) {
                    val sessionTimeout = (session.getSettings(null)?.altimeout ?: 1) * 60 * 1000L

                    logger.d { "Set timeout after ${sessionTimeout / 1000}..." }
                    timeoutTimers += Timer(sessionTimeout) {
                        logger.d { "Session timeout, disconnecting..." }
                        session.disconnectAsync(reason = LogoutReason.AUTO_LOGOUT_TIMEOUT)
                    }
                }
            }
        }.launchIn(CoroutineScope(context = Dispatchers.Default))

        settingsManager.appSettingsStateFlow.onEach {
            torEnabled = it.tor
        }.launchIn(CoroutineScope(context = Dispatchers.Default))

        gdk.setNotificationHandler { gaSession: GASession, jsonObject: Any ->
            try {
                gdkSessions.forEach {
                    it.onNewNotification(
                        gaSession,
                        JsonDeserializer.decodeFromJsonElement(jsonObject as JsonElement)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        connectionChangeEvent.onEach {
            getConnectedEphemeralWalletSessions().filter { it.ephemeralWallet?.isLightning == false && it.ephemeralWallet?.isHardware == true }.mapNotNull { it.ephemeralWallet }.let {
                _hardwareWallets.value = it
            }

            getConnectedEphemeralWalletSessions().filter { it.ephemeralWallet?.isLightning == false && it.ephemeralWallet?.isHardware == false }.mapNotNull { it.ephemeralWallet }.let {
                _ephemeralWallets.value = it
            }
        }.launchIn(scope)

        _torProxy.filterNotNull().onEach {
            countly.updateTorProxy(it)
        }.launchIn(CoroutineScope(context = Dispatchers.Default))
    }

    fun getDeviceSessionForNetworkAllPolicies(device: DeviceInterface, network: Network, isEphemeral: Boolean): GdkSession? {
        return gdkSessions.find { it.device == device && it.defaultNetworkOrNull?.isBitcoin == network.isBitcoin && it.defaultNetworkOrNull?.isTestnet == network.isTestnet && (it.ephemeralWallet != null) == isEphemeral}
    }

    fun getDeviceSessionForEnvironment(device: DeviceInterface, isTestnet: Boolean, isEphemeral: Boolean): GdkSession? {
        return gdkSessions.find { it.device == device && it.isTestnet == isTestnet && (it.ephemeralWallet != null) == isEphemeral }
    }

    fun getWalletSessionOrNull(wallet: GreenWallet): GdkSession? {
        return getWalletSessionOrNull(wallet.id)
    }

    fun getWalletSessionOrCreate(wallet: GreenWallet): GdkSession {
        return getWalletSessionOrNull(wallet.id) ?: createSession().also {
            if(wallet.isEphemeral){
                it.setEphemeralWallet(wallet)
            }

            walletSessions[wallet.id] = it
        }
    }

    fun getWalletSessionOrOnboarding(wallet: GreenWallet?): GdkSession =
        wallet?.let { getWalletSessionOrNull(it) } ?: run { getOnBoardingSession() }

    fun getWalletSessionOrNull(walletId: String): GdkSession? {
        return walletSessions[walletId] ?: gdkSessions.find { it.ephemeralWallet?.id == walletId }?.let {
            return it
        }
    }

    fun getEphemeralWalletSession(walletHashId: String, isHardware: Boolean = false) =
        gdkSessions.find { it.ephemeralWallet?.xPubHashId == walletHashId && it.ephemeralWallet?.isHardware == isHardware }

    fun getWalletIdFromSession(session: GdkSession): String? {
        return walletSessions.filterValues { it == session }.keys.firstOrNull()
    }

    fun getSessions() : Set<GdkSession> = gdkSessions

    fun destroyWalletSession(wallet: GreenWallet){
        walletSessions[wallet.id]?.let{
            it.destroy()
            gdkSessions.remove(it)
        }

        walletSessions.remove(wallet.id)
    }

    fun destroyEphemeralSession(gdkSession: GdkSession){
        // Remove from greenSessions
        gdkSessions.remove(gdkSession)

        // Remove from walletSessions
        gdkSession.ephemeralWallet?.let { walletSessions.remove(it.id) }

        gdkSession.destroy()
    }

    private fun getConnectedEphemeralWalletSessions(): List<GdkSession>{
        return walletSessions.values.filter { it.ephemeralWallet != null && it.isConnected }.toList()
    }

    fun getConnectedHardwareWalletSessions(): List<GdkSession>{
        return walletSessions.values.filter { it.isHardwareWallet && it.isConnected }.toList()
    }

    fun getNextEphemeralId() : Long = (getConnectedEphemeralWalletSessions().filter { it.ephemeralWallet?.isHardware == false }.mapNotNull { it.ephemeralWallet?.ephemeralId }.maxOrNull() ?: 0) + 1

    fun getConnectedDevices(): List<DeviceInterface>{
        return walletSessions.values
            .filter { it.device?.isOffline == false }
            .mapNotNull { it.device }
            .toList()
    }

    // OnBoardingSession waits patiently to be upgraded to a proper wallet session
    fun getOnBoardingSession(): GdkSession {
        // Create a new session if doesn't exists
        return (onBoardingSession ?: createSession().also {
            onBoardingSession = it
        })
    }

    fun getConnectedSessions(): List<GdkSession> {
        return gdkSessions.filter { it.isConnected }
    }

    // Provide an upgradeSession if not sure if it's a OnBoardingSession
    fun upgradeOnBoardingSessionToWallet(wallet: GreenWallet) {
        onBoardingSession?.let {
            walletSessions[wallet.id] = it
            // fire connection change event so that all listeners can track the new session status
            fireConnectionChangeEvent()
            onBoardingSession = null
        }
    }

    // Always use this method to create a Session as it keeps track of gaSession & GreenSession
    private fun createSession(): GdkSession {
        val session = GdkSession(
            userAgent = userAgent,
            sessionManager = this,
            lightningManager = lightningManager,
            settingsManager = settingsManager,
            assetManager = assetManager,
            gdk = gdk,
            wally = wally,
            countly = countly
        )

        gdkSessions.add(session)

        return session
    }

    @NativeCoroutinesIgnore
    suspend fun getLightningBridge(mnemonic: String, isTestnet: Boolean): LightningBridge {
        val lightningLoginData = httpRequestProvider.getWalletIdentifier(
            network = httpRequestProvider.networks.bitcoinElectrum(isTestnet),
            loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic),
        )

        return lightningManager.getLightningBridge(lightningLoginData)
    }

    private fun startTorNetworkSessionIfNeeded() {
        val applicationSettings = settingsManager.getApplicationSettings()
        // If user provides a "sock5://", handle it as Orbot proxy, no need to start GDK TOR session
        if (applicationSettings.tor && applicationSettings.proxyUrl?.startsWith("socks5://") != true) {
            if (!torNetworkSession.isConnected) {
                // Re-initiate connection
                scope.launch(context = Dispatchers.IO + logException(countly)) {
                    gdk.networks().bitcoinElectrum.also { network ->
                        torNetworkSession.connect(network = network, initNetworks = listOf(network))
                    }
                    _torProxy.value = torNetworkSession.getProxySettings().proxy
                }
            }
        }
    }

    fun fireConnectionChangeEvent(){
        _connectionChangeEvent.tryEmit(Unit)
    }

    fun disconnectAll(){
        logger.i { "Disconnecting all sessions" }
        getConnectedSessions().onEach {
            it.disconnectAsync(LogoutReason.USER_ACTION)
        }
    }

    companion object: Loggable()
}