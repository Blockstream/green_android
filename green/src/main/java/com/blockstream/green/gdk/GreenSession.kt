package com.blockstream.green.gdk

import android.graphics.drawable.Drawable
import android.util.SparseArray
import com.blockstream.gdk.*
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.green.ApplicationScope
import com.blockstream.green.BuildConfig
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.logException
import com.blockstream.libgreenaddress.GASession
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.jade.HttpRequestHandler
import com.greenaddress.jade.HttpRequestProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KLogging
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class GreenSession constructor(
    private val applicationScope: ApplicationScope,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val greenWallet: GreenWallet,
    val countly: Countly
) : HttpRequestHandler, HttpRequestProvider, AssetsProvider {
    var isWatchOnly: Boolean = false

    var activeAccount = 0L
        private set

    var activeAccountData: SubAccount? = null
        private set

    // Active Account
    private var balancesSubject = BehaviorSubject.createDefault(linkedMapOf(BalanceLoading))
    private var transactionsSubject = BehaviorSubject.createDefault(listOf(Transaction.LoadingTransaction))

    // All Accounts
    var walletBalances : WalletBalances = SparseArray()
        private set

    private var subAccountsSubject = BehaviorSubject.createDefault<List<SubAccount>>(listOf())
    private var systemMessageSubject = BehaviorSubject.create<String>()
    private var blockSubject = BehaviorSubject.create<Block>()
    private var settingsSubject = BehaviorSubject.create<Settings>()
    private var twoFactorResetSubject = BehaviorSubject.create<TwoFactorReset>()
    private val torStatusSubject = BehaviorSubject.create<TorEvent>()
    private var networkSubject = BehaviorSubject.create<NetworkEvent>()

    var gaSession: GASession = greenWallet.createSession()
    private val disposables = CompositeDisposable()

    val hwWallet: HWWallet?
        get() = device?.hwWallet

    var device: Device? = null
        private set

    var hardwareWallet: Wallet? = null

    lateinit var network: Network
        private set

    // Consider as initialized if network is set
    val isNetworkInitialized: Boolean
        get() = this::network.isInitialized

    var authenticationRequired = false

    val networks
        get() = greenWallet.networks

    val policyAsset
        get() = network.policyAsset

    val isLiquid
        get() = network.isLiquid

    val isTestnet
        get() = network.isTestnet

    val isElectrum
        get() = network.isElectrum

    val isMultisig
        get() = network.isMultisig

    val isSinglesig
        get() = isElectrum

    val isMainnet
        get() = network.isMainnet

    var isConnected : Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            sessionManager.fireConnectionChangeEvent()
        }
    }
        private set

    var walletHashId : String? = null
        private set

    val hasDevice
        get() = device != null

    var pendingTransaction: Pair<CreateTransactionParams, CreateTransaction>? = null

    val networkAssetManager: NetworkAssetManager
        get() = assetManager.getNetworkAssetManager(network)

    private val userAgent by lazy {
        String.format("green_android_%s_%s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    val blockHeight
        get() = blockSubject.value?.height ?: 0

    fun getBlockObservable(): Observable<Block> = blockSubject.hide()
    fun getTransationsObservable(): Observable<List<Transaction>> = transactionsSubject.hide()
    fun getSubAccountsObservable(): Observable<List<SubAccount>> = subAccountsSubject.hide()
    fun getSystemMessageObservable(): Observable<String> = systemMessageSubject.hide()
    fun getTorStatusObservable(): Observable<TorEvent> = torStatusSubject.hide()
    fun getSettingsObservable(): Observable<Settings> = settingsSubject.hide()
    fun getNetworkEventObservable(): Observable<NetworkEvent> = networkSubject.hide()
    fun getTwoFactorResetObservable(): Observable<TwoFactorReset> = twoFactorResetSubject.hide()
    fun getBalancesObservable(): Observable<Balances> = balancesSubject.hide()

    fun getTwoFactorReset(): TwoFactorReset? = twoFactorResetSubject.value

    fun getSettings() : Settings? = settingsSubject.value

    @Throws
    fun availableCurrencies() = greenWallet.getAvailableCurrencies(gaSession)

    fun networkFromWallet(wallet: Wallet) = greenWallet.networks.getNetworkById(wallet.network)

    fun setActiveAccount(account: Long){
        activeAccount = account
        activeAccountData = null

        applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
            try{
                getActiveSubAccount()
            }catch (e: Exception){
                activeAccountData = null
            }
        }

        updateTransactionsAndBalance(isReset = true, isLoadMore = false)
    }

    /*
    Electrum:
        electrum_url: main electrum server provinding data
        spv_enabled: if true, wallet verifies tx inclusion in block header chain using merkle proofs, using electrum_url
        spv_multi: if true (and spv_enabled is true) performs block header chain cross validation using multiple electrum servers
        spv_servers: list of electrum servers to use for cross validation, if empty (default) uses the ones listed in electrum official client

    Green:
        electrum_url: electrum server, used for (eventual) spv validation
        spv_enabled: if true, wallet verifies tx inclusion in block header chain using merkle proofs fetching info from electrum_url
        spv_multi: unused
        spv_servers: unused
     */
    private fun createConnectionParams(network: Network): ConnectionParams {
        val applicationSettings = settingsManager.getApplicationSettings()

        var electrumUrl: String? = null
        var spvServers: List<String>? = null

        // SPV for liquid is disabled // https://gl.blockstream.com/blockstream/green/gdk/-/issues/580
        val spvEnabled = applicationSettings.spv && !Network.isLiquid(network.id)
        var spvMulti = false // Only available in Singlesig

        if (network.isElectrum) {
            var tempUrl = applicationSettings.getPersonalElectrumServer(network)

            if (!tempUrl.isNullOrBlank()) {
                electrumUrl = tempUrl
            }

            spvMulti = applicationSettings.multiServerValidation
            tempUrl = applicationSettings.getSpvElectrumServer(network)

            if (spvMulti && !tempUrl.isNullOrBlank()) {
                spvServers = tempUrl
                    .split(",")
                    .map { it.trim() }
            }
        } else {
            val url = applicationSettings.getPersonalElectrumServer(network)

            if (spvEnabled && !url.isNullOrBlank()) {
                electrumUrl = url
            }
        }

        return ConnectionParams(
            networkName = network.id,
            useTor = applicationSettings.tor,
            userAgent = userAgent,
            proxy = applicationSettings.proxyUrl ?: "",
            spvEnabled = spvEnabled,
            spvMulti = spvMulti,
            electrumUrl = electrumUrl,
            spvServers = spvServers
        )
    }

    fun setInitialNetworkIfNeeded(n: Network){
        if(!isNetworkInitialized){
            network = n
        }
    }

    fun connect(n: Network) {
        disconnect()
        network = n

        greenWallet.connect(
            gaSession,
            createConnectionParams(network)
        )
    }

    fun getProxySettings() = greenWallet.getProxySettings(gaSession)

    fun reconnectHint(hint: ReconnectHintParams) =
        applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
            try{
                greenWallet.reconnectHint(gaSession, hint)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }

    fun disconnect() {
        isConnected = false
        
        authenticationRequired = false

        activeAccount = 0L
        activeAccountData = null

        // Recreate subject so that can be sure we have fresh data, especially on shared sessions eg. HWW sessions
        balancesSubject = BehaviorSubject.createDefault(linkedMapOf(BalanceLoading))
        transactionsSubject = BehaviorSubject.createDefault(listOf(Transaction.LoadingTransaction))
        subAccountsSubject = BehaviorSubject.createDefault(listOf())
        systemMessageSubject = BehaviorSubject.create()
        blockSubject = BehaviorSubject.create()
        settingsSubject = BehaviorSubject.create()
        twoFactorResetSubject = BehaviorSubject.create()
        networkSubject = BehaviorSubject.create()
        torStatusSubject.onNext(TorEvent(progress = 100)) // reset TOR status

        // Clear balances
        walletBalances.clear()

        val gaSessionToBeDestroyed = gaSession
        // Create a new gaSession
        gaSession = greenWallet.createSession()

        // Destroy gaSession
        greenWallet.destroySession(gaSessionToBeDestroyed)
    }

    fun disconnectAsync() {
        // Disconnect only if needed
        if(isConnected) {
            isConnected = false
            try {
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    disconnect()

                    if(hasDevice){
                        sessionManager.destroyHardwareSession(greenSession = this@GreenSession)
                    }
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    fun createNewWallet(network: Network, mnemonic: String): LoginData {
        isWatchOnly = false

        connect(network)

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
        registerUser: Boolean = true,
        device: Device,
        hardwareWalletResolver: HardwareWalletResolver
    ): LoginData {
        this.device = device
        isWatchOnly = false

        if(!isConnected) {
            connect(network)
        }

        device.deviceState
            .async()
            .subscribe {
                // Device went offline
                if(it == Device.DeviceState.DISCONNECTED){
                    disconnectAsync()
                }
            }.addTo(disposables)

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
            val subAccounts = getSubAccounts(SubAccountsParams(refresh = true)).subaccounts

            if(network.isElectrum){
                // On Singlesig, check if there is a SegWit account already restored or create one

                if(subAccounts.firstOrNull { it.type == AccountType.BIP84_SEGWIT } == null){
                    // Create SegWit Account
                    AuthHandler(greenWallet,
                        greenWallet
                            .createSubAccount(gaSession, SubAccountParams("Segwit Account", AccountType.BIP84_SEGWIT))
                    ).resolve(hardwareWalletResolver = hardwareWalletResolver)
                }
            }

            onLoginSuccess(it, 0)
        }
    }

    fun loginWithMnemonic(
        network: Network,
        mnemonic: String,
        password: String = "",
        initializeSession: Boolean = true,
    ): LoginData {
        isWatchOnly = false

        connect(network)
        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(gaSession, loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic, password = password))
        ).result<LoginData>().also {
           if(initializeSession && network.isElectrum){
               // On Singlesig, check if there is a SegWit account already restored or create one
               val subAccounts = getSubAccounts(SubAccountsParams(refresh = true)).subaccounts

               if(subAccounts.firstOrNull { it.type == AccountType.BIP84_SEGWIT } == null){
                   // Create SegWit Account
                   AuthHandler(greenWallet,
                       greenWallet
                           .createSubAccount(gaSession, SubAccountParams("Segwit Account", AccountType.BIP84_SEGWIT))
                   ).resolve(hardwareWalletResolver = DeviceResolver(this))
               }
           }

            onLoginSuccess(loginData = it, initAccountIndex = 0, initializeSession = initializeSession)
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

    private fun reLogin(): LoginData {
        return AuthHandler(
            greenWallet,
            greenWallet.loginUser(
                gaSession,
                deviceParams = DeviceParams(),
                loginCredentialsParams = LoginCredentialsParams()
            )
        ).result<LoginData>(hardwareWalletResolver = DeviceResolver(this)).also {
            authenticationRequired = false
        }
    }

    private fun onLoginSuccess(loginData: LoginData, initAccountIndex: Long, initializeSession: Boolean = true) {
        isConnected = true
        walletHashId = loginData.walletHashId
        if(initializeSession) {
            initializeSessionData(initAccountIndex)
        }
    }

    private fun initializeSessionData(initAccountIndex: Long) {
        var accountIndex = initAccountIndex

        // Check if active subaccount index was archived from a different client
        if(network.isMultisig){
            getSubAccounts(params = SubAccountsParams()).let { subAccounts ->
                if(subAccounts.subaccounts.find { it.pointer == initAccountIndex }?.hidden == true){
                    accountIndex = subAccounts.subaccounts.find { !it.hidden }?.pointer ?: 0
                }
            }
        }

        // Update Liquid Assets from GDK before getting balances to sort them properly
        updateLiquidAssets()

        updateSubAccountsAndBalances(isInitialize = true, refresh = false)

        updateSystemMessage()

        setActiveAccount(accountIndex)
    }

    fun updateLiquidAssets(){
        if(isLiquid) {
            networkAssetManager.updateAssetsIfNeeded(this)
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

    fun setTransactionMemo(txHash: String, memo: String): Boolean = try{
        greenWallet.setTransactionMemo(gaSession, txHash, memo)
        true
    }catch (e: Exception){
        e.printStackTrace()
        false
    }

    fun setPin(pin: String) =
        greenWallet.setPin(gaSession, greenWallet.getMnemonicPassphrase(gaSession), pin)

    fun getMnemonicPassphrase() = greenWallet.getMnemonicPassphrase(gaSession)

    fun getReceiveAddress(index: Long) = AuthHandler(
        greenWallet,
        greenWallet.getReceiveAddress(gaSession, ReceiveAddressParams(index))
    ).result<Address>(hardwareWalletResolver = DeviceResolver(this))

    override fun refreshAssets(params: AssetsParams) = greenWallet.refreshAssets(gaSession, params)

    fun createSubAccount(params: SubAccountParams, hardwareWalletResolver: HardwareWalletResolver) = AuthHandler(greenWallet, greenWallet.createSubAccount(gaSession, params))
            .result<SubAccount>(hardwareWalletResolver = hardwareWalletResolver)

    fun getSubAccounts(params: SubAccountsParams = SubAccountsParams()) = AuthHandler(greenWallet, greenWallet.getSubAccounts(gaSession, params))
        .result<SubAccounts>(hardwareWalletResolver = DeviceResolver(this))

    fun getActiveSubAccount() = getSubAccount(activeAccount)

    fun getSubAccount(index: Long) = AuthHandler(greenWallet, greenWallet.getSubAccount(gaSession, index)
        ).result<SubAccount>(hardwareWalletResolver = DeviceResolver(this)).also {
            if(it.pointer == activeAccount){
                activeAccountData = it
            }
    }

    fun updateSubAccount(params: UpdateSubAccountParams) = greenWallet.updateSubAccount(gaSession, params)

    fun getFeeEstimates() = try {
        greenWallet.getFeeEstimates(gaSession)
    } catch (e: Exception) {
        e.printStackTrace()
        FeeEstimation(fees = listOf(network.defaultFee))
    }

    fun getTransactions(params: TransactionParams) = AuthHandler(greenWallet, greenWallet.getTransactions(gaSession, params))
        .result<Transactions>(
            hardwareWalletResolver = DeviceResolver(this)
        )

    private var txOffset = 0
    private var transactionListBootstrapped = false
    var hasMoreTransactions = false
    var isLoadingTransactions = AtomicBoolean(false)
    fun updateTransactionsAndBalance(isReset: Boolean, isLoadMore: Boolean) : Boolean {

        // For the pager to be instantiated correctly a call with isReset=true should be called first.
        if(!(transactionListBootstrapped || isReset)){
            return false
        }

        // Prevent race condition
        if (!isLoadingTransactions.compareAndSet(false, true)){
            return false
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
                    subaccount = accountBeingFetched,
                    confirmations = 0
                )
            ).also { balances ->
                balancesSubject.onNext(balances)
                walletBalances.put(accountBeingFetched.toInt(), balances)
            }

            it.getTransactions(TransactionParams(subaccount = accountBeingFetched, offset = offset, limit = limit))
        }
        .retry(1)
        .doOnTerminate {
            isLoadingTransactions.set(false)
        }
        .subscribeBy(
            onError = {
                it.printStackTrace()
                it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }

                // Re-set the list to unblock endless loader
                transactionsSubject.onNext(
                    if (transactionsSubject.value?.getOrNull(0)?.isLoadingTransaction() == true) {
                        listOf()
                    } else {
                        transactionsSubject.value
                    }
                )
            }, onSuccess = {
                if (isReset || isLoadMore) {
                    hasMoreTransactions = it.transactions.size == TRANSACTIONS_PER_PAGE
                }
                if (isLoadMore) {
                    transactionsSubject.onNext(
                        (transactionsSubject.value ?: listOf()) + it.transactions
                    )
                    txOffset += TRANSACTIONS_PER_PAGE
                } else {
                    transactionsSubject.onNext(it.transactions)
                }

                // If user changed his active account without this method still running (blocked), check
                // if active account is changed and fetch transaction of the current active account
                if (accountBeingFetched != activeAccount) {
                    updateTransactionsAndBalance(isReset = true, isLoadMore = false)
                }

            }
        ).addTo(disposables)

        return true
    }

    fun getBalance(params: BalanceParams): Balances {
        AuthHandler(greenWallet, greenWallet.getBalance(gaSession, params)).resolve(hardwareWalletResolver = DeviceResolver(this))
            .result<BalanceMap>().let { balanceMap ->
                return LinkedHashMap(
                    balanceMap.toSortedMap { o1, o2 ->
                        when {
                            o1 == policyAsset -> -1
                            o2 == policyAsset -> 1
                            else -> {
                                val asset1 = networkAssetManager.getAsset(o1)
                                val icon1 = networkAssetManager.getAssetIcon(o1)

                                val asset2 = networkAssetManager.getAsset(o2)
                                val icon2 = networkAssetManager.getAssetIcon(o2)

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
            getSubAccounts()
        }
        .subscribeBy(
            onSuccess = {
                subAccountsSubject.onNext(it.subaccounts)
            },
            onError = {
                it.printStackTrace()
                it.message?.let { msg -> greenWallet.extraLogger?.log("ERR: $msg") }
            }
        ).addTo(disposables)
    }

    var isUpdatingSubAccounts = AtomicBoolean(false)
    fun updateSubAccountsAndBalances(isInitialize: Boolean = false, refresh: Boolean = false) {
        // Prevent race condition
        if (!isUpdatingSubAccounts.compareAndSet(false, true)){
            return
        }

        logger.info { "updateSubAccountsAndBalances" }

        observable {
            getSubAccounts(params = SubAccountsParams(refresh = refresh)).also {
                if(isInitialize){
                    // immediately update subAccounts subject
                    subAccountsSubject.onNext(it.subaccounts)
                }

                for(subaccount in it.subaccounts){
                    getBalance(BalanceParams(
                        subaccount = subaccount.pointer,
                        confirmations = 0
                    )).also {  accountBalances ->
                        walletBalances.put(subaccount.pointer.toInt(), accountBalances)
                    }
                }

                if(isInitialize) {
                    countly.activeWallet(
                        session = this,
                        walletBalances = walletBalances,
                        subAccounts = it.subaccounts
                    )
                }
            }
        }
        .retry(1)
        .doOnTerminate {
            isUpdatingSubAccounts.set(false)
        }
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
        }else if(isAsset && convert.assetAmount != null){
            val jsonElement = buildJsonObject {
                put("asset_info", convert.asset!!.toJsonElement())
                put(convert.asset?.assetId ?: "", convert.assetAmount)
            }
            greenWallet.convertAmount(gaSession, convert, jsonElement)
        } else{
            greenWallet.convertAmount(gaSession, convert)
        }
    }catch (e: Exception){
        e.printStackTrace()
        null
    }

    fun getUnspentOutputs(params: BalanceParams) = AuthHandler(
        greenWallet,
        greenWallet.getUnspentOutputs(gaSession, params)
    ).result<UnspentOutputs>(hardwareWalletResolver = DeviceResolver(this))

    fun createTransaction(unspentOutputs: UnspentOutputs, addresses: List<AddressParams>): CreateTransaction {
        val params = CreateTransactionParams(
            subaccount = activeAccount,
            utxos = unspentOutputs.unspentOutputsAsJsonElement,
            addressees = addresses
        )

        return AuthHandler(
            greenWallet,
            greenWallet.createTransaction(gaSession, params)
        ).result<CreateTransaction>(hardwareWalletResolver = DeviceResolver(this))
    }

    fun createTransaction(params: GAJson<*>) = AuthHandler(
        greenWallet,
        greenWallet.createTransaction(gaSession, params)
    ).result<CreateTransaction>(hardwareWalletResolver = DeviceResolver(this))

    fun updateCreateTransaction(createTransaction: CreateTransaction) =
        AuthHandler(
            greenWallet,
            greenWallet.updateTransaction(gaSession, createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>(hardwareWalletResolver = DeviceResolver(this))

    fun signTransaction(createTransaction: CreateTransaction) =
        AuthHandler(
            greenWallet,
            greenWallet.signTransaction(gaSession, createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>(hardwareWalletResolver = DeviceResolver(this))

    fun broadcastTransaction(transaction: String) = greenWallet.broadcastTransaction(gaSession, transaction)

    fun sendTransaction(createTransaction: CreateTransaction, twoFactorResolver: TwoFactorResolver) =
        AuthHandler(
            greenWallet,
            greenWallet.sendTransaction(gaSession, createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>(twoFactorResolver = twoFactorResolver, hardwareWalletResolver = DeviceResolver(this))

    fun onNewNotification(notification: Notification) {
        logger.info { "onNewNotification $notification" }

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    blockSubject.onNext(it)
                    // SingleSig after connect immediatelly sends a block with height 0
                    // it's not safe to call getTransactions so early
                    if(it.height > 0) {
                        updateTransactionsAndBalance(isReset = false, isLoadMore = false)
                    }
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
                notification.tor?.let {
                    torStatusSubject.onNext(it)
                }
            }
            "network" -> {
                notification.network?.let { event ->

                    if(isConnected){
                        if(event.isConnected && authenticationRequired){
                            applicationScope.launch(context = Dispatchers.IO + logException(countly)){
                                try{
                                    reLogin()
                                }catch (e: Exception){
                                    e.printStackTrace()
                                }
                            }
                        }else if(!event.isConnected){
                            // mark re-authentication is required
                            authenticationRequired = true
                        }
                    }

                    networkSubject.onNext(event)
                }
            }
            "ticker" -> {
                // UPDATE UI
            }
            "transaction" -> {
                notification.transaction?.let {
                    if(it.subaccounts.contains(activeAccount)){
                        updateTransactionsAndBalance(isReset = false, isLoadMore = false)
                    }else{
                        updateSubAccountsAndBalances()
                    }
                }
            }
        }
    }

    fun hasAssetIcon(assetId : String) = networkAssetManager.hasAssetIcon(assetId)
    fun getAsset(assetId : String): Asset? = networkAssetManager.getAsset(assetId)
    fun getAssetDrawableOrNull(assetId : String): Drawable? = networkAssetManager.getAssetDrawableOrNull(assetId)
    fun getAssetDrawableOrDefault(assetId : String): Drawable = networkAssetManager.getAssetDrawableOrDefault(assetId)

    internal fun destroy() {
        disconnect()
        disposables.clear()
    }

    companion object: KLogging(){
        const val TRANSACTIONS_PER_PAGE: Int = 30
    }
}