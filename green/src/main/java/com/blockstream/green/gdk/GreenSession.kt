package com.blockstream.green.gdk

import android.graphics.drawable.Drawable
import com.blockstream.gdk.*
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.green.BuildConfig
import com.blockstream.green.database.Wallet
import com.blockstream.green.devices.Device
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
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.LinkedHashMap


class GreenSession constructor(
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val assetsManager: AssetManager,
    private val greenWallet: GreenWallet
) : HttpRequestHandler, HttpRequestProvider, AssetsProvider {
    var isWatchOnly: Boolean = false

    var activeAccount = 0L
        private set

    private val balancesSubject = BehaviorSubject.createDefault(linkedMapOf(BalanceLoading))
    private val transactionsSubject = BehaviorSubject.createDefault(listOf(Transaction.LoadingTransaction))
    private val assetsSubject: BehaviorSubject<Assets> = BehaviorSubject.createDefault(Assets())
    private val subAccountsSubject = BehaviorSubject.createDefault<List<SubAccount>>(listOf())
    private val systemMessageSubject = BehaviorSubject.create<String>()
    private val blockSubject = BehaviorSubject.create<Block>()
    private val feesSubject = BehaviorSubject.createDefault<List<Long>>(listOf())
    private val settingsSubject = BehaviorSubject.create<Settings>()
    private val twoFactorResetSubject = BehaviorSubject.create<TwoFactorReset>()
    private val torStatusSubject = BehaviorSubject.create<TORStatus?>()
    private val networkSubject = BehaviorSubject.create<NetworkEvent>()

    val gaSession: GASession = greenWallet.createSession()
    private val disposables = CompositeDisposable()

    val hwWallet: HWWallet?
        get() = device?.hwWallet

    var device: Device? = null
        private set

    lateinit var network: Network
        private set

    val networks
        get() = greenWallet.networks

    val policyAsset
        get() = network.policyAsset

    val isLiquid
        get() = network.isLiquid

    val isElectrum
        get() = network.isElectrum

    val isMainnet
        get() = network.isMainnet

    var isConnected = false
        private set

    var walletHashId : String? = null
        private set

    val hasDevice
        get() = device != null

    val userAgent by lazy {
        String.format("green_android_%s_%s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    val blockHeight
        get() = blockSubject.value?.height ?: 0

    fun getAssetsObservable(): Observable<Assets> = assetsSubject.hide()
    fun getBlockObservable(): Observable<Block> = blockSubject.hide()
    fun getTransationsObservable(): Observable<List<Transaction>> = transactionsSubject.hide()
    fun getSubAccountsObservable(): Observable<List<SubAccount>> = subAccountsSubject.hide()
    fun getSystemMessageObservable(): Observable<String> = systemMessageSubject.hide()
    fun getTorStatusObservable(): Observable<TORStatus> = torStatusSubject.hide()
    fun getSettingsObservable(): Observable<Settings> = settingsSubject.hide()
    fun getNetworkEventObservable(): Observable<NetworkEvent> = networkSubject.hide()
    fun getTwoFactorResetObservable(): Observable<TwoFactorReset> = twoFactorResetSubject.hide()
    fun getBalancesObservable(): Observable<Balances> = balancesSubject.hide()

    fun getTwoFactorReset(): TwoFactorReset? = twoFactorResetSubject.value

    fun getSettings() : Settings? = settingsSubject.value

    fun getFees(): List<Long> = feesSubject.value

    val availableCurrencies by lazy {
        greenWallet.getAvailableCurrencies(gaSession)
    }

    fun networkFromWallet(wallet: Wallet) = greenWallet.networks.getNetworkById(wallet.network)

    fun setActiveAccount(account: Long){
        activeAccount = account
        updateTransactionsAndBalance(isReset = true, isLoadMore = false)
    }

    fun connect(network: Network) {
        disconnect(disconnectDevice = false)

        this.network = network

        // Prevent multiple open sessions
        sessionManager.disconnectSessions(this)

        // Bridge Session to GDKSession
        Bridge.bridgeSession(
            gaSession,
            network.network
        )

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

    fun reconnectHint() = try {
        greenWallet.reconnectHint(gaSession)
    }catch (e: Exception){
        e.printStackTrace()
    }

    fun disconnect(disconnectDevice : Boolean = true) {
        if(isConnected){
            sessionManager.fireConnectionChangeEvent()
        }

        isConnected = false
        if(disconnectDevice){
            device?.disconnect()
            device = null
        }
        greenWallet.disconnect(gaSession)
    }

    fun disconnectAsync() {
        isConnected = false

        observable {
            disconnect()
        }.subscribeBy(
            onError = {
                it.printStackTrace()
                it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }
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

    fun createNewWallet(network: Network, providedMnemonic: String?): LoginData {
        isWatchOnly = false

        connect(network)
        val mnemonic = providedMnemonic ?: generateMnemonic12()

        AuthHandler(
            greenWallet,
            greenWallet.registerUser(gaSession, DeviceParams(), mnemonic)
        ).resolve()

        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic))
        ).result<LoginData>().also {
            if(network.isElectrum){
                // Create SegWit Account
                AuthHandler(greenWallet,
                    greenWallet
                        .createSubAccount(gaSession, SubAccountParams("Segwit Account", AccountType.BIP84_SEGWIT))
                ).resolve()
            }

            onLoginSuccess(it, 0)
        }
    }

    fun loginWatchOnly(wallet: Wallet, username: String, password: String) {
        loginWatchOnly(networkFromWallet(wallet), username, password)
    }

    fun loginWatchOnly(network: Network, username: String, password: String): LoginData {
        isWatchOnly = true

        connect(network)
        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(username = username, password = password))
        ).result<LoginData>().also {
            onLoginSuccess(it, 0)
        }
    }

    fun loginWithDevice(
        network: Network,
        registerUser: Boolean,
        device: Device,
        hardwareWalletResolver: HardwareWalletResolver
    ): LoginData {
        this.device = device
        isWatchOnly = false

        if(!isConnected) {
            connect(network)
        }

        val gdkDevice = device.hwWallet?.device

        if(registerUser) {
            AuthHandler(
                greenWallet,
                greenWallet.registerUser(gaSession, DeviceParams(gdkDevice), "")
            ).resolve(hardwareWalletResolver = hardwareWalletResolver)
        }

        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, deviceParams = DeviceParams(gdkDevice))
        ).result<LoginData>(hardwareWalletResolver = hardwareWalletResolver).also {
            onLoginSuccess(it, 0)
        }
    }

    fun loginWithMnemonic(
        network: Network,
        mnemonic: String,
        password: String = ""
    ): LoginData {
        isWatchOnly = false

        connect(network)
        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic, password = password))
        ).result<LoginData>().also {
           if(network.isElectrum){
               // On Singlesig, check if there is a SegWit account already restored or create one
               val subAccounts = AuthHandler(
                   greenWallet,
                   greenWallet.getSubAccounts(gaSession)
               ).result<SubAccounts>(hardwareWalletResolver = HardwareCodeResolver(hwWallet)).subaccounts

               if(subAccounts.firstOrNull { it.type == AccountType.BIP84_SEGWIT } == null){
                   // Create SegWit Account
                   AuthHandler(greenWallet,
                       greenWallet
                           .createSubAccount(gaSession, SubAccountParams("Segwit Account", AccountType.BIP84_SEGWIT))
                   ).resolve(hardwareWalletResolver = HardwareCodeResolver(hwWallet))
               }
           }

            onLoginSuccess(it, 0)
        }
    }

    fun loginWithPin(wallet: Wallet, pin: String, pinData: PinData): LoginData {
        isWatchOnly = false

        connect(networkFromWallet(wallet))
        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(pin = pin, pinData = pinData))
        ).result<LoginData>().also {
            onLoginSuccess(it, wallet.activeAccount)
        }
    }

    private fun onLoginSuccess(loginData: LoginData, initAccountIndex: Long){
        isConnected = true
        walletHashId = loginData.walletHashId
        initializeSessionData(initAccountIndex)

        sessionManager.fireConnectionChangeEvent()
    }

    private fun initializeSessionData(initAccountIndex: Long) {
        updateSubAccounts()
        updateSystemMessage()

        setActiveAccount(initAccountIndex)

        if (network.isLiquid) {
            assetsManager.updateAssetsIfNeeded(this)
        }
    }

    fun updateSystemMessage(){
        observable {
            greenWallet.getSystemMessage(gaSession)
        }
        .subscribeBy(
            onSuccess = {
                systemMessageSubject.onNext(it ?: "")
            },
            onError = {
                it.printStackTrace()
                it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }
            }).addTo(disposables)
    }

    fun ackSystemMessage(message: String) = AuthHandler(
            greenWallet,
            greenWallet.ackSystemMessage(gaSession, message))


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

    private fun getTransactions(params: TransactionParams) = AuthHandler(greenWallet, greenWallet.getTransactions(gaSession, params))

    private var txOffset = 0
    var hasMoreTransactions = false
    var isLoadingTransactions = AtomicBoolean(false)
    var transactionListBootstrapped = false
    fun updateTransactionsAndBalance(isReset: Boolean, isLoadMore: Boolean) {

        // For the pager to be instantiated correctly a call with isReset=true should be called first.
        if(!(transactionListBootstrapped || isReset)){
            return
        }

        // Prevent race condition
        if (!isLoadingTransactions.compareAndSet(false, true)){
            logger.info { "Prevent race condition" }
            return
        }

        transactionListBootstrapped = true

        val accountBeingFetched = activeAccount

        observable {
            var offset = 0

            if (isReset) {
                balancesSubject.onNext(linkedMapOf(BalanceLoading))
                transactionsSubject.onNext(listOf(Transaction.LoadingTransaction))
                txOffset = 0
            } else if (isLoadMore) {
                offset = txOffset + TRANSACTIONS_PER_PAGE
            }

            val limit = if (isReset || isLoadMore) TRANSACTIONS_PER_PAGE else (txOffset + TRANSACTIONS_PER_PAGE)

            it.getBalance(
                BalanceParams(
                    subaccount = activeAccount,
                    confirmations = 0
                )
            ).also { balances ->
                balancesSubject.onNext(balances)
            }

            it.getTransactions(TransactionParams(subaccount = activeAccount, offset = offset, limit = limit))
                .result<Transactions>(
                    hardwareWalletResolver = HardwareCodeResolver(hwWallet)
                )
        }
        .retry(1)
        .doOnTerminate {
            isLoadingTransactions.set(false)
        }
        .subscribeBy(onError = {
            it.printStackTrace()
            it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }
        }, onSuccess = {
            if (isReset || isLoadMore) {
                hasMoreTransactions = it.transactions.size == TRANSACTIONS_PER_PAGE
            }
            if (isLoadMore) {
                transactionsSubject.onNext((transactionsSubject.value ?: listOf()) + it.transactions)
                txOffset += TRANSACTIONS_PER_PAGE
            }else{
                transactionsSubject.onNext(it.transactions)
            }

            // If user changed his active account without this method still running (blocked), check
            // if active account is changed and fetch transaction of the current active account
            if(accountBeingFetched != activeAccount){
                updateTransactionsAndBalance(isReset = true, isLoadMore = false)
            }

        }).addTo(disposables)
    }

    private fun getBalance(params: BalanceParams): Balances {
        AuthHandler(greenWallet, greenWallet.getBalance(gaSession, params)).resolve(hardwareWalletResolver = HardwareCodeResolver(hwWallet))
            .result<BalanceMap>().let { balanceMap ->
                return LinkedHashMap(
                    balanceMap.toSortedMap { o1, o2 ->
                        when {
                            o1 == policyAsset -> -1
                            o2 == policyAsset -> 1
                            else -> {
                                val asset1 = assetsManager.getAsset(o1)
                                val icon1 = assetsManager.getAssetIcon(o1)

                                val asset2 = assetsManager.getAsset(o2)
                                val icon2 = assetsManager.getAssetIcon(o2)

                                if ((icon1 == null) xor (icon2 == null)) {
                                    if (icon1 != null) -1 else 1
                                } else if ((asset1 == null) xor (asset2 == null)) {
                                    if (asset1 != null) -1 else 1
                                } else if (asset1 != null && asset2 != null) {
                                    asset1.name.compareTo(asset2.name)
                                } else {
                                    o1.compareTo(o2)
                                }
                            }
                        }
                    }
                )
            }
    }

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

    fun twofactorReset(email:String, isDispute: Boolean) =
        AuthHandler(greenWallet, greenWallet.twofactorReset(gaSession, email, isDispute))

    fun twofactorUndoReset(email: String) =
        AuthHandler(greenWallet, greenWallet.twofactorUndoReset(gaSession, email))

    fun twofactorCancelReset() =
        AuthHandler(greenWallet, greenWallet.twofactorCancelReset(gaSession))

    fun twofactorChangeLimits(limits: Limits) =
        AuthHandler(greenWallet, greenWallet.twofactorChangeLimits(gaSession, limits))

    fun sendNlocktimes() = greenWallet.sendNlocktimes(gaSession)

    fun changeSettings(settings: Settings) =
        AuthHandler(greenWallet, greenWallet.changeSettings(gaSession, settings))

    fun setCsvTime(value: Int) =
        AuthHandler(greenWallet, greenWallet.setCsvTime(gaSession, value))

    fun updateSettings(){
        logger.info { "updateSettings" }

        observable {
            greenWallet.getSettings(gaSession)
        }.retry(1)
            .subscribeBy(
                onSuccess = {
                    settingsSubject.onNext(it)
                },
                onError = {
                    it.printStackTrace()
                    // settingsSubject.onError(it)
                    it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }
                }).addTo(disposables)

    }

    fun updateSubAccounts() {
        logger.info { "updateSubAccounts" }

        observable {
            AuthHandler(greenWallet, greenWallet.getSubAccounts(gaSession)).result<SubAccounts>(hardwareWalletResolver = HardwareCodeResolver(hwWallet))
        }.retry(1)
            .subscribeBy(
                onSuccess = {
                    subAccountsSubject.onNext(it.subaccounts)
                },
                onError = {
                    it.printStackTrace()
                    it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }
                }).addTo(disposables)
    }

    // asset_info in Convert object can be null for liquid assets that don't have asset metadata
    // if no asset is given, no conversion is needed (conversion will be identified as a btc value in gdk)
    fun convertAmount(convert: Convert, isAsset: Boolean = false) = try{
        if(isAsset && convert.asset == null){
            Balance.fromAssetWithoutMetadata(convert)
        }else {
            greenWallet.convertAmount(gaSession, convert)
        }
    }catch (e: Exception){
        e.printStackTrace()
        null
    }

    fun onNewNotification(notification: Notification) {
        logger.info { "onNewNotification $notification" }

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    blockSubject.onNext(it)
                    updateTransactionsAndBalance(isReset = false, isLoadMore = false)
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
            "ticker" -> {
                // UPDATE UI
            }
            "transaction" -> {
                notification.transaction?.let {
                    if(it.subaccounts.contains(activeAccount)){
                        updateTransactionsAndBalance(isReset = false, isLoadMore = false)
                    }
                }
            }
        }
    }

    // TODO implement
    private fun updateAssets(assets: Assets) {
        assetsSubject.onNext(assets)
    }

    fun getAsset(assetId : String): Asset? = assetsManager.getAsset(assetId)
    fun getAssetDrawableOrDefault(assetId : String): Drawable = assetsManager.getAssetDrawableOrDefault(assetId)

    internal fun destroy() {
        disconnect()
        disposables.clear()
    }

    companion object: KLogging(){
        const val TRANSACTIONS_PER_PAGE: Int = 30
    }
}