package com.blockstream.green.gdk

import com.blockstream.gdk.AuthHandler
import com.blockstream.gdk.BalanceMap
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.green.BuildConfig
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.database.Wallet
import com.blockstream.green.utils.AssetManager
import com.blockstream.libgreenaddress.GASession
import com.blockstream.libgreenaddress.KotlinGDK
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.Bridge
import com.greenaddress.jade.HttpRequestHandler
import com.greenaddress.jade.HttpRequestProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import mu.KLogging
import java.net.URL


class GreenSession constructor(
    private val settingsManager: SettingsManager,
    private val assetsManager: AssetManager,
    private val greenWallet: GreenWallet,
) : HttpRequestHandler, HttpRequestProvider {
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

    lateinit var network: Network
        private set

    val networks
        get() = greenWallet.networks

    val isLiquid
        get() = network.isLiquid

    val isMainnet
        get() = network.isMainnet

    private var isConnected = false

    val userAgent by lazy {
        String.format("green_android_%s_%s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    fun isConnected() = isConnected

    fun getSubAccountsObservable(): Observable<List<SubAccount>> = subAccountsSubject.hide()
    fun getTorStatusObservable(): Observable<TORStatus> = torStatusSubject.hide()
    fun getSettingsObservable(): Observable<Settings> = settingsSubject.hide()
    fun getNetworkEventObservable(): Observable<NetworkEvent> = networkSubject.hide()
    fun getBalancesObservable(): Observable<List<BalancePair>> = balancesSubject.hide()


    fun getSettings() : Settings? = settingsSubject.value

    fun getFees(): List<Long> = feesSubject.value

    val availableCurrencies by lazy {
        greenWallet.getAvailableCurrencies(gaSession)
    }

    fun networkFromWallet(wallet: Wallet) = greenWallet.networks.getNetworkById(wallet.network)

    fun connect(network: Network) {
        disconnect()
        this.network = network

        if(!Bridge.usePrototype) {
            // Bridge Session to GDKSession
            Bridge.bridgeSession(gaSession, network.network, if(isWatchOnly) watchOnlyUsernameBridge else null)
        }

        val applicationSettings = settingsManager.getApplicationSettings()

        greenWallet.connect(
            gaSession,
            ConnectionParams(
                networkName = network.id,
                useTor = applicationSettings.tor,
                logLevel = if(BuildConfig.DEBUG) "debug" else "none",
                userAgent = userAgent,
                proxy = applicationSettings.proxyURL ?: ""
            )
        )
    }

    fun disconnect() {
        isConnected = false
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

    private fun generateMnemonic() = greenWallet.generateMnemonic()

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

        connect(network)
        val mnemonic = providedMnemonic ?: generateMnemonic()

        AuthHandler(
            greenWallet,
            greenWallet.registerUser(gaSession, buildJsonObject { }, mnemonic)
        ).resolve()

        AuthHandler(
            greenWallet,
            greenWallet.loginWithMnemonic(gaSession, buildJsonObject { }, mnemonic, "")
        ).resolve()

        isConnected = true
    }

    fun loginWatchOnly(wallet: Wallet, username: String, password: String) {
        loginWatchOnly(networkFromWallet(wallet), username, password)
    }

    fun loginWatchOnly(network: Network, username: String, password: String) {
        isWatchOnly = true
        watchOnlyUsernameBridge = username

        connect(network)
        greenWallet.loginWatchOnly(gaSession, username, password)
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
            greenWallet.loginWithMnemonic(gaSession, buildJsonObject { }, mnemonic, password)
        ).resolve()

        isConnected = true


        initializeSessionData()
    }

    fun loginWithPin(wallet: Wallet, pin: String, pinData: PinData) {
        isWatchOnly = false

        connect(networkFromWallet(wallet))
        AuthHandler(
            greenWallet,
            greenWallet.loginWithPin(gaSession, pin, pinData)
        ).resolve()

        isConnected = true

        initializeSessionData()
    }

    private fun initializeSessionData() {
        if(Bridge.usePrototype) {
            updateSubAccounts()

            if (network.isLiquid) {
                initLiquidAssets()
            }
        }
    }

    fun setPin(pin: String) =
        greenWallet.setPin(gaSession, greenWallet.getMnemonicPassphrase(gaSession), pin)

    fun getMnemonicPassphrase() = greenWallet.getMnemonicPassphrase(gaSession)

    fun getReceiveAddress(index: Long) = AuthHandler(
        greenWallet,
        greenWallet.getReceiveAddress(gaSession, ReceiveAddressParams(index))
    )

    fun refreshAssets(params: AssetsParams) = greenWallet.refreshAssets(gaSession, params)

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

    fun twofactorChangeLimits(limits: JsonElement) =
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
                    if (o1 == "btc") -1 else o1.compareTo(o2)
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

    private fun initLiquidAssets() {
        if(Bridge.usePrototype) {

            try {
                if(!assetsManager.isUpToDate){
                    // Update from Cache
                    assetsManager.setCache(refreshAssets(AssetsParams(assets = true, icons = true, refresh = false)))

                    // Try to update the registry
                    assetsManager.updateAssets(refreshAssets(AssetsParams(assets = true, icons = true, refresh = true)))
                }

            }catch (e: Exception){
                e.printStackTrace()
            }
        }else{
            // Implement v3 if needed
        }
    }

    fun getAssets(): Assets = assetsManager.getAssets()

    internal fun destroy() {
        disconnect()
        disposables.clear()
    }

    fun isNotAuthorized(it: Throwable) =
        it.getGDKErrorCode() == KotlinGDK.GA_NOT_AUTHORIZED || it.message?.contains(":login failed:") == true


    companion object: KLogging()
}