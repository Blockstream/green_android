package com.blockstream.green.gdk

import android.graphics.drawable.Drawable
import com.blockstream.gdk.*
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.green.BuildConfig
import com.blockstream.green.database.Wallet
import com.blockstream.green.settings.SettingsManager
import com.blockstream.libgreenaddress.GASession
import com.blockstream.libgreenaddress.KotlinGDK
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.Bridge
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.Session
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import com.greenaddress.jade.HttpRequestHandler
import com.greenaddress.jade.HttpRequestProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import mu.KLogging
import java.net.URL


class GreenSession constructor(
    private val settingsManager: SettingsManager,
    private val assetsManager: AssetManager,
    private val greenWallet: GreenWallet
) : HttpRequestHandler, HttpRequestProvider, AssetsProvider {
    var isWatchOnly: Boolean = false

    // Only needed for v3 codebase
    var watchOnlyUsernameBridge: String? = null

    private val balancesSubject = BehaviorSubject.createDefault<List<BalancePair>>(listOf())

    private val subAccountsSubject = BehaviorSubject.createDefault<List<SubAccount>>(listOf())
    private val blockSubject = BehaviorSubject.create<Block>()
    private val feesSubject = BehaviorSubject.createDefault<List<Long>>(listOf())
    private val settingsSubject = BehaviorSubject.create<Settings>()
    private val twoFactorResetSubject = BehaviorSubject.create<TwoFactorReset>()
    private val torStatusSubject = BehaviorSubject.create<TORStatus?>()
    private val networkSubject = BehaviorSubject.create<NetworkEvent>()

    val gaSession: GASession = greenWallet.createSession()
    private val disposables = CompositeDisposable()

    var hwWallet: HWWallet? = null
        private set

    lateinit var network: Network
        private set

    val networks
        get() = greenWallet.networks

    val isLiquid
        get() = network.isLiquid

    val isElectrum
        get() = network.isElectrum

    val isMainnet
        get() = network.isMainnet

    var isConnected = false
        private set

    val hasDevice
        get() = hwWallet != null

    val userAgent by lazy {
        String.format("green_android_%s_%s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    fun getSubAccountsObservable(): Observable<List<SubAccount>> = subAccountsSubject.hide()
    fun getTorStatusObservable(): Observable<TORStatus> = torStatusSubject.hide()
    fun getSettingsObservable(): Observable<Settings> = settingsSubject.hide()
    fun getNetworkEventObservable(): Observable<NetworkEvent> = networkSubject.hide()
    fun getTwoFactorResetObservable(): Observable<TwoFactorReset> = twoFactorResetSubject.hide()
    fun getBalancesObservable(): Observable<List<BalancePair>> = balancesSubject.hide()

    fun getTwoFactorReset(): TwoFactorReset? = twoFactorResetSubject.value

    fun getSettings() : Settings? = settingsSubject.value

    fun getFees(): List<Long> = feesSubject.value

    val availableCurrencies by lazy {
        greenWallet.getAvailableCurrencies(gaSession)
    }

    fun networkFromWallet(wallet: Wallet) = greenWallet.networks.getNetworkById(wallet.network)

    fun connect(network: Network, hwWallet: HWWallet? = null) {
        disconnect()
        this.network = network
        this.hwWallet = hwWallet

        if(!Bridge.useGreenModule) {
            // Bridge Session to GDKSession
            Bridge.bridgeSession(
                gaSession,
                network.network,
                if (isWatchOnly) watchOnlyUsernameBridge else null
            )
        }

        val applicationSettings = settingsManager.getApplicationSettings()

        greenWallet.connect(
            gaSession,
            ConnectionParams(
                networkName = network.id,
                useTor = applicationSettings.tor && network.supportTorConnection, // Exclude Singlesig from Tor connection
                logLevel = if (BuildConfig.DEBUG) "debug" else "none",
                userAgent = userAgent,
                proxy = applicationSettings.proxyURL ?: ""
            )
        )


        // GDK doesn't send connection events on connect
        // to avoid having invalid events from previous connections
        // emulate a successful connect event
        NetworkEvent(connected = true, loginRequired = false, waiting = 0).let {
            networkSubject.onNext(it)

            // Pass notification to to GDKSession
            Session.getSession().also { v3Session ->
                v3Session
                    .notificationModel
                    .onNewNotification(
                        v3Session.nativeSession, GreenWallet.JsonDeserializer.encodeToJsonElement(Notification("network", network = it))
                    )
            }
        }
    }

    fun reconnectHint() = greenWallet.reconnectHint(gaSession)

    fun disconnect() {
        isConnected = false
        hwWallet?.disconnect()
        hwWallet = null
        greenWallet.disconnect(gaSession)
    }

    fun disconnectAsync() {
        isConnected = false

        observable {
            disconnect()
        }.subscribeBy(
            onError = {
                it.printStackTrace()
            }
        )
    }

    private fun generateMnemonic12() = greenWallet.generateMnemonic12()
    private fun generateMnemonic24() = greenWallet.generateMnemonic24()

    override fun getHttpRequest(): HttpRequestHandler {
        return this
    }

    fun httpRequest(data: JsonElement) = greenWallet.httpRequest(gaSession, data)

    override fun httpRequest(details: JsonNode?): JsonNode {

        val json = httpRequest(Json.parseToJsonElement(details.toString()))

        val mapper = ObjectMapper()
        val actualObj = mapper.readTree(json.toString())
        return actualObj
    }

    override fun httpRequest(
        method: String?,
        urls: MutableList<URL>?,
        data: String?,
        accept: String?,
        certs: MutableList<String>?
    ): JsonNode {
        val mapper = ObjectMapper()
        // Build the json parameters
        val details: ObjectNode = mapper.createObjectNode()

        // Method and URLs
        details.put("method", method)
        val urlsArray = details.putArray("urls")
        for (url in urls!!) {
            urlsArray.add(url.toExternalForm())
        }

        // Optional (POST) data, 'accept' strings, and additional certificates.
        if (data != null) {
            details.put("data", data)
        }
        if (accept != null) {
            details.put("accept", accept)
        }
        if (certs != null) {
            val certsArray = details.putArray("root_certificates")
            for (cert in certs) {
                certsArray.add(cert)
            }
        }

        // Call httpRequest passing the assembled json parameters
        return httpRequest(details)
    }

    fun createNewWallet(network: Network, providedMnemonic: String?) {
        isWatchOnly = false

        connect(network, hwWallet)
        val mnemonic = providedMnemonic ?: generateMnemonic12()

        AuthHandler(
            greenWallet,
            greenWallet.registerUser(gaSession, DeviceParams(), mnemonic)
        ).resolve()

        AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic))
        ).resolve()

        if(network.isElectrum){
            // Create SegWit Account
            AuthHandler(greenWallet,
                greenWallet
                .createSubAccount(gaSession, SubAccountParams("Segwit Account", AccountType.BIP84_SEGWIT))
            ).resolve()
        }

        isConnected = true
    }

    fun loginWatchOnly(wallet: Wallet, username: String, password: String) {
        loginWatchOnly(networkFromWallet(wallet), username, password)
    }

    fun loginWatchOnly(network: Network, username: String, password: String) {
        isWatchOnly = true
        watchOnlyUsernameBridge = username

        connect(network)
        AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(username = username, password = password))
        ).resolve()
        isConnected = true

        initializeSessionData()
    }

    fun loginWithDevice(
        network: Network,
        registerUser: Boolean,
        connectSession: Boolean,
        hwWallet: HWWallet,
        hardwareCodeResolver: HardwareCodeResolver
    ) {
        isWatchOnly = false

        if(connectSession) {
            connect(network, hwWallet)
        }

        this.hwWallet = hwWallet

        val device = hwWallet.hwDeviceData.toDevice()

        if(registerUser) {
            AuthHandler(
                greenWallet,
                greenWallet.registerUser(gaSession, DeviceParams(device), "")
            ).resolve(hardwareWalletResolver = hardwareCodeResolver)
        }

        AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, deviceParams = DeviceParams(device))
        ).resolve(hardwareWalletResolver = hardwareCodeResolver)

        isConnected = true

        initializeSessionData()
    }

    fun loginWithMnemonic(
        network: Network,
        mnemonic: String,
        password: String = ""
    ) {
        isWatchOnly = false

        connect(network)
        AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic, password = password))
        ).resolve()

        isConnected = true

        if(network.isElectrum){

            // On Singlesig, check if there is a SegWit account already restored or create one
            val subAccounts = AuthHandler(
                greenWallet,
                greenWallet.getSubAccounts(gaSession)
            ).result<SubAccounts>().subaccounts

            if(subAccounts.firstOrNull { it.type == AccountType.BIP84_SEGWIT } == null){
                // Create SegWit Account
                AuthHandler(greenWallet,
                    greenWallet
                        .createSubAccount(gaSession, SubAccountParams("Segwit Account", AccountType.BIP84_SEGWIT))
                ).resolve()
            }
        }

        initializeSessionData()
    }

    fun loginWithPin(wallet: Wallet, pin: String, pinData: PinData) {
        isWatchOnly = false

        connect(networkFromWallet(wallet))
        AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(pin = pin, pinData = pinData))
        ).resolve()

        isConnected = true

        initializeSessionData()
    }

    private fun initializeSessionData() {
        if(Bridge.useGreenModule) {
            updateSubAccounts()
        }

        if (network.isLiquid) {
            assetsManager.updateAssetsIfNeeded(this)
        }
    }

    fun setPin(pin: String) =
        greenWallet.setPin(gaSession, greenWallet.getMnemonicPassphrase(gaSession), pin)

    fun getMnemonicPassphrase() = greenWallet.getMnemonicPassphrase(gaSession)

    fun getReceiveAddress(index: Long) = AuthHandler(
        greenWallet,
        greenWallet.getReceiveAddress(gaSession, ReceiveAddressParams(index))
    )

    override fun refreshAssets(params: AssetsParams) = greenWallet.refreshAssets(gaSession, params)

    fun createSubAccount(params: SubAccountParams) =
        AuthHandler(greenWallet, greenWallet.createSubAccount(gaSession, params))

    fun getSubAccounts() =
        AuthHandler(greenWallet, greenWallet.getSubAccounts(gaSession))

    fun getSubAccount(index: Long) =
        AuthHandler(greenWallet, greenWallet.getSubAccount(gaSession, index))

    fun renameSubAccount(index: Long, name: String) = greenWallet.renameSubAccount(
        gaSession,
        index,
        name
    )

    fun getTransactions(params: TransactionParams) =
        AuthHandler(greenWallet, greenWallet.getTransactions(gaSession, params))

    fun changeSettingsTwoFactor(method: String, methodConfig: TwoFactorMethodConfig) =
        AuthHandler(
            greenWallet, greenWallet.changeSettingsTwoFactor(
                gaSession,
                method,
                methodConfig
            )
        )

    fun getTwoFactorConfig() = greenWallet.getTwoFactorConfig(gaSession)

    fun getWatchOnlyUsername() = greenWallet.getWatchOnlyUsername(gaSession)

    fun setWatchOnly(username: String, password: String) = greenWallet.setWatchOnly(
        gaSession,
        username,
        password
    )

    fun twofactorCancelReset() =
        AuthHandler(greenWallet, greenWallet.twofactorCancelReset(gaSession))

    fun twofactorChangeLimits(limits: Limits) =
        AuthHandler(greenWallet, greenWallet.twofactorChangeLimits(gaSession, limits))

    fun changeSettings(settings: Settings) =
        AuthHandler(greenWallet, greenWallet.changeSettings(gaSession, settings))

    fun updateSettings(){
        observable {
            greenWallet.getSettings(gaSession)
        }.retry(1)
            .subscribeBy(
                onSuccess = {
                    settingsSubject.onNext(it)
                },
                onError = {
                    it.printStackTrace()
//                    settingsSubject.onError(it)
                }).addTo(disposables)

    }

    fun updateSubAccounts() {
        // Electrum Network support only a single account, no need to continue
        if(network.isElectrum) return


        observable {
            AuthHandler(greenWallet, greenWallet.getSubAccounts(gaSession)).result<SubAccounts>()
        }.retry(1)
            .subscribeBy(
                onSuccess = {
                    subAccountsSubject.onNext(it.subaccounts)
                },
                onError = {
                    it.printStackTrace()
                }).addTo(disposables)
    }

    fun updateBalance(account: Long){
        observable {
            it.getBalance(BalanceParams(account))
        }
        .retry(1)
        .subscribeBy(onError = {
            it.printStackTrace()
        }, onSuccess = {
            balancesSubject.onNext(it)
        }).addTo(disposables)
    }

    private fun getBalance(params: BalanceParams): List<BalancePair> {
        AuthHandler(greenWallet, greenWallet.getBalance(gaSession, params)).resolve()
            .result<BalanceMap>().let { balanceMap ->

                return balanceMap.toSortedMap { o1, o2 ->
                    if (o1 == network.policyAsset) -1 else o1.compareTo(o2)
                }.map { it.toPair() }
            }
    }

    fun convertAmount(convert: Convert) = greenWallet.convertAmount(gaSession, convert)

    fun onNewNotification(notification: Notification) {
        logger().info { "onNewNotification $notification" }

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    blockSubject.onNext(it)
                }
            }
            "fees" -> {
                notification.fees?.let {
                    feesSubject.onNext(it)
                }
            }
            "settings" -> {
                notification.settings?.let {
                    settingsSubject.onNext(it)
                }
            }
            "twofactor_reset" -> {
                notification.twoFactorReset?.let {
                    twoFactorResetSubject.onNext(it)
                }
            }
            "tor" -> {
                notification.torStatus?.let {
                    torStatusSubject.onNext(it)
                }
            }
            "network" -> {
                notification.network?.let {
                    networkSubject.onNext(it)
                }
            }
        }
    }

    fun getAsset(assetId : String): Asset? = assetsManager.getAsset(assetId)
    fun getAssetDrawableOrDefault(assetId : String): Drawable? = assetsManager.getAssetDrawableOrDefault(assetId)

    internal fun destroy() {
        disconnect()
        disposables.clear()
    }

    companion object: KLogging()
}