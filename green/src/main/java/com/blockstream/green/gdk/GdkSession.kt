package com.blockstream.green.gdk

import android.graphics.drawable.Drawable
import com.blockstream.gdk.*
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.gdk.params.TransactionParams.Companion.TRANSACTIONS_PER_PAGE
import com.blockstream.green.BuildConfig
import com.blockstream.green.data.Countly
import com.blockstream.green.data.EnrichedAsset
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.devices.Device
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.extensions.logException
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.libgreenaddress.GAAuthHandler
import com.blockstream.libgreenaddress.GASession
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.jade.HttpRequestHandler
import com.greenaddress.jade.HttpRequestProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KLogging
import java.net.URL
import kotlin.properties.Delegates

typealias WalletBalances = Map<String, Long>

typealias Assets = Map<String, Long>
typealias AssetPair = Pair<String, Long>

typealias AccountId = String
/* Handles multiple GDK sessions per network */
class GdkSession constructor(
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val gdkBridge: GdkBridge,
    private val countly: Countly
) : HttpRequestHandler, HttpRequestProvider, AssetsProvider {
    fun createScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) = CoroutineScope(SupervisorJob() + dispatcher + logException(countly))

    private val scope = createScope(Dispatchers.Default)

    val isTestnet: Boolean // = false
        get() = defaultNetwork.isTestnet

    val isMainnet: Boolean
        get() = !isTestnet

    var isWatchOnly: Boolean = false

    private var _activeAccountStateFlow: MutableStateFlow<Account?> = MutableStateFlow(null)
    val activeAccountFlow get() = _activeAccountStateFlow.asStateFlow()
    val activeAccountOrNull get() = _activeAccountStateFlow.value
    val activeAccount get() = _activeAccountStateFlow.value!!

    // Block notification handling until all networks are initialized
    private var blockNotificationHandling = false

    // Wallet
    private var _walletTotalBalanceSharedFlow = MutableStateFlow(-1L)
    val walletTotalBalanceFlow get() = _walletTotalBalanceSharedFlow.asStateFlow()

    private var _walletAssetsFlow : MutableStateFlow<Assets> = MutableStateFlow(AssetsLoading)
    val walletAssetsFlow: StateFlow<Assets> get() = _walletAssetsFlow.asStateFlow()
    val walletAssets: Assets get() = _walletAssetsFlow.value

    private var _enrichedAssetsFlow : MutableStateFlow<Map<String, EnrichedAsset>> = MutableStateFlow(mapOf())
    val enrichedAssetsFlow: StateFlow<Map<String, EnrichedAsset>> get() = _enrichedAssetsFlow.asStateFlow()
    val enrichedAssets: Map<String, EnrichedAsset> get() = _enrichedAssetsFlow.value

    private var _walletHasHistorySharedFlow = MutableStateFlow(false)
    val walletHasHistory get() = _walletHasHistorySharedFlow.value

    // Assets
    private var _accountAssetsFlow = mutableMapOf<AccountId, MutableStateFlow<Assets>>()
    private fun accountAssetsStateFlow(account: Account): MutableStateFlow<Assets> {
        return _accountAssetsFlow.getOrPut(account.id) {
            MutableStateFlow(mapOf())
        }
    }

    fun accountAssetsFlow(account: Account) = accountAssetsStateFlow(account).asStateFlow()
    fun accountAssets(account: Account): Assets = accountAssetsFlow(account).value

    private val _accountsAndBalanceUpdatedSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    val accountsAndBalanceUpdatedFlow get() = _accountsAndBalanceUpdatedSharedFlow.asSharedFlow()

    // Transactions
    private var _walletTransactionsStateFlow : MutableStateFlow<List<Transaction>> = MutableStateFlow(listOf(Transaction.LoadingTransaction))
    val walletTransactionsFlow get() = _walletTransactionsStateFlow.asStateFlow()

    private var _accountTransactionsStateFlow = mutableMapOf<AccountId, MutableStateFlow<List<Transaction>>>()
    private fun accountTransactionsStateFlow(account: Account): MutableStateFlow<List<Transaction>>{
        return _accountTransactionsStateFlow.getOrPut(account.id) {
            MutableStateFlow(listOf(Transaction.LoadingTransaction))
        }
    }
    fun accountTransactionsFlow(account: Account) = accountTransactionsStateFlow(account).asStateFlow()
    fun accountTransactions(account: Account) = accountTransactionsStateFlow(account).value

    private var _accountTransactionsPagerSharedFlow = mutableMapOf<AccountId, MutableSharedFlow<Boolean>>()
    private fun accountTransactionsPagerSharedFlow(account: Account): MutableSharedFlow<Boolean>{
        return _accountTransactionsPagerSharedFlow.getOrPut(account.id) {
            MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
    }
    fun accountTransactionsPagerFlow(account: Account) = accountTransactionsPagerSharedFlow(account).asSharedFlow()

    private var _twoFactorConfigCache = mutableMapOf<Network, TwoFactorConfig>()

    private var _blockStateFlow = mutableMapOf<Network, MutableStateFlow<Block>>()
    private fun blockStateFlow(network: Network) = _blockStateFlow.getOrPut(network) { MutableStateFlow(Block(height = 0)) }
    fun blockFlow(network: Network): StateFlow<Block> = blockStateFlow(network).asStateFlow()
    fun blockHeight(network: Network) = blockStateFlow(network).value.height

    private var _settingsStateFlow = mutableMapOf<Network, MutableStateFlow<Settings?>>()
    private fun settingsStateFlow(network: Network) = _settingsStateFlow.getOrPut(network) { MutableStateFlow(null) }
    fun settingsFlow(network: Network) = settingsStateFlow(network).asStateFlow()

    private var _twoFactorResetStateFlow = mutableMapOf<Network, MutableStateFlow<TwoFactorReset?>>()
    private fun twoFactorResetStateFlow(network: Network) = _twoFactorResetStateFlow.getOrPut(network) { MutableStateFlow(null) }
    fun twoFactorResetFlow(network: Network) = twoFactorResetStateFlow(network).asStateFlow()

    private var _networkEventsStateFlow = mutableMapOf<Network, MutableStateFlow<NetworkEvent?>>()
    private fun networkEventsStateFlow(network: Network) = _networkEventsStateFlow.getOrPut(network) { MutableStateFlow(null) }
    fun networkEventsFlow(network: Network) = networkEventsStateFlow(network).asStateFlow()

    private var _failedNetworksStateFlow: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    val failedNetworksFlow get() = _failedNetworksStateFlow.asStateFlow()

    private var _systemMessageStateFlow : MutableStateFlow<List<Pair<Network, String>>> = MutableStateFlow(listOf())
    val systemMessageFlow get() = _systemMessageStateFlow.asStateFlow()

    private var _allAccountsStateFlow = MutableStateFlow<List<Account>>(listOf())
    val allAccountsFlow : StateFlow<List<Account>> get() = _allAccountsStateFlow.asStateFlow()
    val allAccounts : List<Account> get() = _allAccountsStateFlow.value

    private var _accountsStateFlow = MutableStateFlow<List<Account>>(listOf())
    val accountsFlow : StateFlow<List<Account>> get() = _accountsStateFlow.asStateFlow()
    val accounts : List<Account> get() = _accountsStateFlow.value

    private var _accountAssetStateFlow = MutableStateFlow<List<AccountAsset>>(listOf())
    val accountAssetFlow : StateFlow<List<AccountAsset>> get() = _accountAssetStateFlow.asStateFlow()

    private val _torStatusSharedFlow = MutableStateFlow<TorEvent>(TorEvent(progress = 100))
    val torStatusFlow = _torStatusSharedFlow.asStateFlow()

    private val _tickerSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    val tickerFlow = _tickerSharedFlow.asSharedFlow()

    var defaultNetworkOrNull: Network? = null
        private set

    val defaultNetwork: Network
        get() = defaultNetworkOrNull!!

    val mainAssetNetwork
        get() = bitcoin ?: defaultNetwork

    val bitcoin
        get() = bitcoinSinglesig ?: bitcoinMultisig

    val bitcoinSinglesig
        get() =  gdkSessions.firstNotNullOfOrNull { it.key.takeIf { network -> network.isElectrum && network.isBitcoin } }

    val bitcoinMultisig
        get() = gdkSessions.firstNotNullOfOrNull { it.key.takeIf { network -> network.isMultisig && network.isBitcoin } }

    val liquid
        get() = liquidSinglesig ?: liquidMultisig

    val liquidSinglesig
        get() = gdkSessions.firstNotNullOfOrNull { it.key.takeIf { network -> network.isElectrum && network.isLiquid } }

    val liquidMultisig
        get() = gdkSessions.firstNotNullOfOrNull { it.key.takeIf { network -> network.isMultisig && network.isLiquid } }

    val activeBitcoin get() = activeBitcoinSinglesig ?: activeBitcoinMultisig
    val activeBitcoinSinglesig get() = bitcoinSinglesig?.takeIf { hasActiveNetwork(it) }
    val activeBitcoinMultisig get() = bitcoinMultisig?.takeIf { hasActiveNetwork(it) }
    val activeLiquid get() = activeLiquidSinglesig ?: activeLiquidMultisig
    val activeLiquidSinglesig get() = liquidSinglesig?.takeIf { hasActiveNetwork(it) }
    val activeLiquidMultisig get() = liquidMultisig?.takeIf { hasActiveNetwork(it) }

    val activeMultisig get() = listOfNotNull(activeBitcoinMultisig, activeLiquidMultisig)

    val activeSinglesig get() = listOfNotNull(activeBitcoinSinglesig, activeLiquidSinglesig)

    val gdkSessions = mutableMapOf<Network, GASession>()
    val activeSessions = mutableSetOf<Network>()

    fun hasActiveNetwork(network: Network?) = activeSessions.contains(network)

    private val activeGdkSessions: Map<Network, GASession>
        get() = gdkSessions.filter { activeSessions.contains(it.key) }

    private fun gdkSession(network: Network) = gdkSessions.getOrPut(network){
        gdkBridge.createSession()
    }

    val isHardwareWallet: Boolean
        get() = device != null

    val hwWallet: HWWallet?
        get() = device?.hwWallet

    var device: Device? = null
        private set

    var ephemeralWallet: Wallet? = null

    // Consider as initialized if network is set
    val isNetworkInitialized: Boolean
        get() = defaultNetworkOrNull != null

    var authenticationRequired = mutableMapOf<Network, Boolean>()

    val networks
        get() = gdkBridge.networks

    var isConnected : Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            sessionManager.fireConnectionChangeEvent()
        }
    }
        private set

    var walletHashId : String? = null
        private set

    var pendingTransaction: Pair<CreateTransactionParams, CreateTransaction>? = null

    val networkAssetManager: NetworkAssetManager get() = assetManager.getNetworkAssetManager(isMainnet)

    val hideAmounts: Boolean get() = settingsManager.getApplicationSettings().hideAmounts

    val starsOrNull: String? get() = "*****".takeIf { hideAmounts }

    private val userAgent by lazy {
        String.format("green_android_%s_%s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
    }

    private var walletActiveEventInvalidated = false

    init {
        _accountsAndBalanceUpdatedSharedFlow.onEach {
            var walletBalance = 0L

            accounts.forEach {
                walletBalance += accountAssets(it).policyAsset()
            }

            _walletTotalBalanceSharedFlow.value = walletBalance
        }.launchIn(scope)

        countly.remoteConfigUpdateEvent.onEach {
            updateEnrichedAssets()
        }.launchIn(scope + Dispatchers.IO)
    }

    private fun authHandler(network: Network, gaAuthHandler: GAAuthHandler): AuthHandler =
        AuthHandler(network = network, session = this, gdkBridge = gdkBridge, gaAuthHandler = gaAuthHandler)

    private fun updateEnrichedAssets() {
        if (isNetworkInitialized) {
            _enrichedAssetsFlow.value =
                countly.getRemoteConfigValueForAssets(if (isMainnet) LIQUID_ASSETS_KEY else LIQUID_ASSETS_TESTNET_KEY)
                    ?: mapOf<String, EnrichedAsset>().also {
                        cacheAssets(it.keys)
                    }
        }
    }

    fun getTwoFactorReset(network: Network): TwoFactorReset? = twoFactorResetFlow(network).value
    fun getSettings(network: Network? = null): Settings? {
        return settingsStateFlow(network ?: defaultNetwork).value ?: try {
            gdkBridge.getSettings(gdkSession(network ?: defaultNetwork))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun changeSettings(network: Network, settings: Settings) =
        authHandler(network, gdkBridge.changeSettings(gdkSession(network), settings)).resolve()

    fun changeGlobalSettings(settings: Settings){
        val exceptions = mutableListOf<Exception>()
        activeSessions.forEach { network ->
            getSettings(network)?.also { networkSettings ->
                try{
                    changeSettings(network, Settings.normalizeFromProminent(network = network, networkSettings = networkSettings, prominentSettings = settings))
                }catch (e: Exception){
                    e.printStackTrace()
                    exceptions.add(e)
                }
            }
        }

        if(exceptions.isNotEmpty()){
            throw Exception(exceptions.first().message)
        }
    }

    fun updateSettings(network: Network? = null){
        activeSessions.filter { network == null || it.network == network.id }.forEach {
            settingsStateFlow(it).value = gdkBridge.getSettings(gdkSession(it))
        }
    }

    private fun syncSettings(){
        // Prefer Multisig for initial sync as those networks are synced across devices
        val syncNetwork = activeBitcoinMultisig ?: activeLiquidMultisig ?: defaultNetwork
        getSettings(network = syncNetwork)?.also { prominentSettings ->
            try{
                changeGlobalSettings(prominentSettings)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    @Throws
    fun availableCurrencies() = gdkBridge.getAvailableCurrencies(gdkSession(defaultNetwork))

    fun prominentNetwork(isTestnet: Boolean) = if(isTestnet) networks.testnetBitcoinElectrum else networks.bitcoinElectrum
    fun prominentNetwork(wallet: Wallet, loginCredentials: LoginCredentials? = null) =
        if (loginCredentials != null && loginCredentials.network.isNotBlank()) networkBy(
            loginCredentials.network
        ) else if (wallet.isWatchOnly || wallet.isHardware) networkBy(wallet.activeNetwork) else prominentNetwork(wallet.isTestnet)

    fun networkBy(id: String) = gdkBridge.networks.getNetworkById(id)

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
        val spvEnabled = applicationSettings.spv && !Network.isLiquidMainnet(network.id)
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

    fun networks(isTestnet: Boolean, isHardware: Boolean): List<Network> {
        return if (isTestnet) {
            listOfNotNull(
                networks.testnetBitcoinElectrum,
                networks.testnetBitcoinGreen,
                networks.testnetLiquidElectrum.takeIf { !isHardware },
                networks.testnetLiquidGreen
            )
        } else {
            listOfNotNull(
                networks.bitcoinElectrum,
                networks.bitcoinGreen,
                networks.liquidElectrum.takeIf { !isHardware }, // Liquid electrum is not yet supported for hww
                networks.liquidGreen
            )
        }
    }

    private fun initGdkSessions(initNetworks: List<Network>?, isHardware: Boolean) {
        if (initNetworks.isNullOrEmpty()) {
            networks(isTestnet, isHardware).forEach {
                gdkSession(it)
            }
        } else {
            // init the provided networks
            initNetworks.forEach {
                gdkSession(it)
            }
        }
    }

    fun connect(network: Network, initNetworks: List<Network>? = null, isHardware: Boolean = false) {
        defaultNetworkOrNull = network

        disconnect()

        initGdkSessions(initNetworks = initNetworks, isHardware = isHardware)

        runBlocking {
            gdkSessions.map {
                scope.launch(Dispatchers.IO) {
                    try {
                        gdkBridge.connect(it.value, createConnectionParams(it.key))
                    } catch (e: Exception) {
                        _failedNetworksStateFlow.value = _failedNetworksStateFlow.value + it.key
                    }
                }
            }.joinAll()

            // Update the enriched assets
            updateEnrichedAssets()
        }
    }

    fun getProxySettings() = gdkBridge.getProxySettings(gdkSession(defaultNetwork))

    fun reconnectHint(hint: ReconnectHintParams) =
        scope.launch(context = Dispatchers.IO) {
            gdkSessions.forEach {
                gdkBridge.reconnectHint(it.value, hint)
            }
        }

    fun disconnect() {
        isConnected = false

        authenticationRequired.clear()

        _activeAccountStateFlow.value = null

        // Recreate subject so that can be sure we have fresh data, especially on shared sessions eg. HWW sessions
        _blockStateFlow = mutableMapOf()
        _settingsStateFlow = mutableMapOf()
        _twoFactorResetStateFlow = mutableMapOf()
        _networkEventsStateFlow = mutableMapOf()
        _failedNetworksStateFlow = MutableStateFlow(listOf())

        _allAccountsStateFlow = MutableStateFlow(listOf())
        _accountsStateFlow = MutableStateFlow(listOf())
        _accountAssetStateFlow = MutableStateFlow(listOf())
        _accountAssetsFlow = mutableMapOf()
        _systemMessageStateFlow = MutableStateFlow(listOf())
        _torStatusSharedFlow.value = TorEvent(progress = 100) // reset TOR status

        // Clear Cache
        _twoFactorConfigCache = mutableMapOf()

        _walletHasHistorySharedFlow.value = false

        // Clear total balance
        _walletTotalBalanceSharedFlow = MutableStateFlow(-1L)

        // Clear Assets
        _walletAssetsFlow = MutableStateFlow(AssetsLoading)

        // Clear Enriched Assets
        _enrichedAssetsFlow = MutableStateFlow(mapOf())

        // Clear Transactions
        _walletTransactionsStateFlow = MutableStateFlow(listOf(Transaction.LoadingTransaction))
        _accountTransactionsStateFlow = mutableMapOf()
        _accountTransactionsPagerSharedFlow = mutableMapOf()

        val gaSessionToBeDestroyed = gdkSessions.values.toList()

        // Create a new gaSession
        gdkSessions.clear()

        // Clear active sessions
        activeSessions.clear()

        // Destroy gaSession
        gaSessionToBeDestroyed.forEach {
            gdkBridge.destroySession(it)
        }
    }

    private fun resetNetwork(network: Network){
        // Remove as active network
        activeSessions.remove(network)

        gdkSessions.remove(network)?.also { gaSessionToBeDestroyed ->
            gdkBridge.destroySession(gaSessionToBeDestroyed)
        }

        // Init a new Session and connect
        try{
            gdkBridge.connect(gdkSession(network), createConnectionParams(network))
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun disconnectAsync() {
        // Disconnect only if needed
        if(isConnected) {
            isConnected = false

            scope.launch(context = Dispatchers.IO) {
                disconnect()

                // Destroy session if it's ephemeral
                ephemeralWallet?.also {
                    sessionManager.destroyEphemeralSession(gdkSession = this@GdkSession)
                }
            }
        }
    }

    override fun getHttpRequest(): HttpRequestHandler {
        return this
    }

    fun httpRequest(data: JsonElement) = gdkBridge.httpRequest(gdkSession(defaultNetwork), data)

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

    fun <T> initNetworkIfNeeded(network: Network, action: () -> T) : T{
        if(!activeSessions.contains(network)){

            try{
                gdkBridge.connect(gdkSession(network), createConnectionParams(network))
            }catch (e: Exception){
                // catch if already connected
            }

            val previousMultisig = activeMultisig.firstOrNull()

            val loginCredentialsParams = if(isHardwareWallet){
                LoginCredentialsParams.empty
            }else{
                LoginCredentialsParams.fromCredentials(getCredentials())
            }

            val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.hwWallet?.device)

            authHandler(network,
                gdkBridge.registerUser(
                    session = gdkSession(network),
                    deviceParams = deviceParams,
                    loginCredentialsParams = loginCredentialsParams)
            ).resolve()

            authHandler(
                network,
                gdkBridge.loginUser(
                    session = gdkSession(network),
                    deviceParams = deviceParams,
                    loginCredentialsParams = loginCredentialsParams
                )
            ).resolve()

            activeSessions.add(network)

            // Sync settings, use multisig if exists to sync PGP also
            (getSettings(network = previousMultisig) ?: getSettings())?.also {
                changeSettings(network, Settings.normalizeFromProminent(network = network, networkSettings = getSettings(network) ?: it, prominentSettings = it, pgpFromProminent = true))
            }

            // hard refresh accounts for the new network
            val networkAccounts = getAccounts(network, refresh = true)

            // Archive default account if it is unfunded
            val defaultAccount = networkAccounts.first()

            if(!defaultAccount.hasHistory(this)){
                updateAccount(
                    account = defaultAccount,
                    isHidden = true,
                    resetAccountName = defaultAccount.type.title()
                )
            }

            // update GreenSessions accounts (use cache)
            updateAccounts()

            // Update accounts & balances & transactions for the new network
            updateAccountsAndBalances(updateBalancesForAccounts = networkAccounts)
            updateWalletTransactions(updateForAccounts = networkAccounts)
        }

        return action.invoke()
    }

    fun tryFailedNetworks(hardwareWalletResolver: HardwareWalletResolver? = null){
        scope.launch(context = Dispatchers.IO){

            val loginCredentialsParams = if(isHardwareWallet){
                LoginCredentialsParams.empty
            }else{
                LoginCredentialsParams.fromCredentials(getCredentials())
            }

            val networks = _failedNetworksStateFlow.value
            val failedNetworkLogins = mutableListOf<Network>()

            _failedNetworksStateFlow.value = listOf()

            // Network with failed logins
            networks.forEach { network ->
                try{
                    logger.info { "Login into ${network.id}" }

                    try{
                        gdkBridge.connect(gdkSession(network), createConnectionParams(network))
                    }catch (e: Exception){
                        // catch if already connected
                    }

                    authHandler(
                        network,
                        gdkBridge.loginUser(gdkSession(network), loginCredentialsParams = loginCredentialsParams)
                    ).resolve(hardwareWalletResolver = hardwareWalletResolver)

                    activeSessions.add(network)
                }catch (e: Exception){
                    e.printStackTrace()

                    if(e.message != "id_login_failed"){
                        failedNetworkLogins.add(network)
                    }
                }
            }

            _failedNetworksStateFlow.value = failedNetworkLogins

            updateAccountsAndBalances()
            updateWalletTransactions()
        }
    }

    fun emergencyRestoreOfRecoveryPhrase(
        wallet: Wallet,
        pin: String,
        loginCredentials: LoginCredentials,
    ): Credentials {
        val network = prominentNetwork(wallet, loginCredentials)

        connect(network = network, initNetworks = listOf(network))

        return decryptCredentialsWithPin(
            network = network,
            decryptWithPinParams = DecryptWithPinParams(pin = pin, pinData = loginCredentials.pinData)
        )
    }

    fun loginWithPin(
        wallet: Wallet,
        pin: String,
        loginCredentials: LoginCredentials,
        isRestore: Boolean = false,
        initializeSession : Boolean = true,
    ): LoginData {
        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(wallet, loginCredentials),
            wallet = wallet,
            loginCredentialsParams = LoginCredentialsParams(pin = pin, pinData = loginCredentials.pinData),
            isRestore = isRestore,
            initializeSession = initializeSession
        )
    }

    fun loginWithMnemonic(
        isTestnet: Boolean,
        wallet: Wallet? = null,
        loginCredentialsParams: LoginCredentialsParams,
        initNetworks: List<Network>? = null,
        initializeSession: Boolean,
        isSmartDiscovery: Boolean,
        isCreate: Boolean,
        isRestore: Boolean,
    ): LoginData {
        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(isTestnet),
            wallet = wallet,
            loginCredentialsParams = loginCredentialsParams,
            initNetworks = initNetworks,
            isSmartDiscovery = isSmartDiscovery,
            isCreate = isCreate,
            isRestore = isRestore,
            initializeSession = initializeSession
        )
    }

    fun loginWatchOnly(wallet: Wallet, username: String, password: String) {
        loginWatchOnly(prominentNetwork(wallet), username, password)
    }

    fun loginWatchOnly(network: Network, username: String, password: String): LoginData {
        return loginWithLoginCredentials(
            prominentNetwork = network,
            initNetworks = listOf(network),
            loginCredentialsParams = LoginCredentialsParams(username = username, password = password),
            isRestore = false,
            initializeSession = true
        )
    }

    fun loginWithDevice(
        network: Network,
        device: Device,
        hardwareWalletResolver: HardwareWalletResolver,
        hwWalletBridge: HWWalletBridge,
    ): LoginData {
        val initNetworks = if(device.isTrezor){
            if(network.isTestnet){
                listOf(networks.testnetBitcoinElectrum, networks.testnetBitcoinGreen)
            }else{
                listOf(networks.bitcoinElectrum, networks.bitcoinGreen)
            }
        } else {
            // Jade or Ledger
            if(network.isTestnet){
                listOf(networks.testnetBitcoinElectrum, networks.testnetBitcoinGreen, networks.testnetLiquidGreen)
            }else{
                listOf(networks.bitcoinElectrum, networks.bitcoinGreen, networks.liquidGreen)
            }.let {
                if(device.isLedger){
                    // Ledger can operate only into a single network but both policies are supported
                    it.filter { it.isBitcoin == network.isBitcoin }
                }else{
                    it
                }
            }
        }

        return loginWithLoginCredentials(
            prominentNetwork = network,
            initNetworks = initNetworks,
            loginCredentialsParams = LoginCredentialsParams.empty,
            device = device,
            isSmartDiscovery = true,
            hardwareWalletResolver = hardwareWalletResolver,
            hwWalletBridge = hwWalletBridge,
        )
    }

    private fun loginWithLoginCredentials(
        prominentNetwork: Network,
        initNetworks: List<Network>? = null,
        wallet: Wallet? = null,
        loginCredentialsParams: LoginCredentialsParams,
        device: Device? = null,
        isCreate: Boolean = false,
        isRestore: Boolean = false,
        isSmartDiscovery: Boolean = false,
        initializeSession: Boolean = true,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        hwWalletBridge: HWWalletBridge? = null,
    ): LoginData {
        this.isWatchOnly = !loginCredentialsParams.username.isNullOrBlank()
        this.device = device

        blockNotificationHandling = true
        walletActiveEventInvalidated = true

        connect(
            network = prominentNetwork,
            initNetworks = initNetworks,
            isHardware = hardwareWalletResolver != null
        )

        device?.deviceState?.onEach {
            // Device went offline
            if(it == Device.DeviceState.DISCONNECTED){
                disconnectAsync()
            }
        }?.launchIn(scope)

        val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.hwWallet?.device)

        val initAccount = wallet?.activeAccount
        val initNetwork = wallet?.activeNetwork

        // Get enabled singlesig networks (multisig can be identified by login_user)
        val enabledGdkSessions = gdkSessions.toSortedMap { n1, n2 -> // Sort prominent first
            if(n1 == prominentNetwork) {
                -1
            }else if(n2 == prominentNetwork) {
                1
            }else{
                n1.id.compareTo(n2.id)
            }
        }

        @Suppress("NAME_SHADOWING")
        // If it's a pin login, get the credentials from the prominent network
        val loginCredentialsParams =
            if (loginCredentialsParams.pin.isNullOrBlank()) loginCredentialsParams else decryptCredentialsWithPin(
                prominentNetwork,
                DecryptWithPinParams.fromLoginCredentials(loginCredentialsParams)
            ).let {
                LoginCredentialsParams.fromCredentials(it)
            }

        val failedNetworkLogins = mutableListOf<Network>()

        var prominentException: Exception? = null

        return enabledGdkSessions.map { gdkSession ->
            scope.async(start = CoroutineStart.DEFAULT) {
                val isProminent = gdkSession.key == prominentNetwork
                val network = gdkSession.key
                try {
                    val hasGdkCache = if(isHardwareWallet || loginCredentialsParams.mnemonic.isNotBlank()){
                        try{
                            gdkBridge.hasGdkCache(
                                getWalletIdentifier(
                                    network = network,
                                    loginCredentialsParams = hwWallet?.let {
                                        LoginCredentialsParams(
                                            masterXpub = it.getXpubs(
                                                network,
                                                hwWalletBridge,
                                                listOf(listOf())
                                            ).first()
                                        )
                                    } ?: loginCredentialsParams
                                )
                            )
                        }catch (e: Exception){
                            e.printStackTrace()
                            false
                        }
                    }else {
                        false
                    }

                    if (gdkSession.key.isSinglesig && !isProminent && !isRestore && !isSmartDiscovery && !hasGdkCache){
                        logger.info { "Skip login in ${network.id}" }
                        return@async null
                    }

                    // On Create just login into Bitcoin network
                    if(isCreate && (gdkSession.key.isMultisig || gdkSession.key.isLiquid)){
                        logger.info { "Skip login in ${network.id}" }
                        return@async null
                    }

                    logger.info { "Login into ${network.id}" }

                    authHandler(
                        gdkSession.key,
                        gdkBridge.loginUser(
                            session = gdkSession.value,
                            deviceParams = deviceParams,
                            loginCredentialsParams = loginCredentialsParams
                        )
                    ).result<LoginData>(hardwareWalletResolver = hardwareWalletResolver).also { loginData ->
                        // Mark it as active
                        activeSessions.add(network)

                        // Do a refresh
                        if (network.isElectrum && initializeSession && (isRestore || isSmartDiscovery)) {
                             if(isRestore || !hasGdkCache){ // wait for https://gl.blockstream.io/blockstream/green/gdk/-/merge_requests/1034 //  !greenWallet.hasGdkCache(loginData)
                                logger.info { "BIP44 Discovery for ${network.id}" }

                                val networkAccounts = getAccounts(network = network, refresh = true)
                                val walletIsFunded = networkAccounts.find { account -> account.bip44Discovered == true } != null

                                if(walletIsFunded){
                                    // Archive no-history default account
                                    networkAccounts.first().also {
                                        if (it.pointer == 0L && !it.hasHistory(this@GdkSession)) {
                                            updateAccount(
                                                account = it,
                                                isHidden = true,
                                                resetAccountName = it.type.title()
                                            )
                                        }
                                    }
                                }else{
                                    if(isProminent){
                                        // If prominent archive default account
                                        networkAccounts.first().also{ defaultAccount ->
                                            updateAccount(
                                                account = defaultAccount,
                                                isHidden = true,
                                                resetAccountName = defaultAccount.type.title()
                                            )
                                        }
                                    }else{
                                        // Else disconnect and remove cache
                                        resetNetwork(network)

                                        // Remove GDK cache folder
                                        removeGdkCache(loginData)
                                    }
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                    if(isProminent) {
                        prominentException = e
                    }

                    // Can't proceed with login if the prominent network fails
                    if (e.message != "id_login_failed") {
                        // Mark network as not being able to login
                        failedNetworkLogins.add(gdkSession.key)
                    }
                    null
                }
            }

        }.let { deferred ->
            runBlocking {
                deferred.awaitAll().filterNotNull() // Run all in parallel
            }.let {
                // Use prominent return to identify if an error happened
                it.firstOrNull() ?: throw prominentException!! // Throw if prominent network fails
            }.also{
                _failedNetworksStateFlow.value = _failedNetworksStateFlow.value + failedNetworkLogins

                onLoginSuccess(
                    loginData = it,
                    initAccount = initAccount,
                    initNetwork = initNetwork,
                    initializeSession = initializeSession
                )
            }
        }
    }

    private fun reLogin(network: Network): LoginData {
        return authHandler(
            network,
            gdkBridge.loginUser(gdkSession(network), loginCredentialsParams = LoginCredentialsParams.empty)
        ).result<LoginData>().also {
            authenticationRequired.remove(network)

            getAccounts(network, refresh = true).forEach {
                getTransactions(account = it, isReset = false, isLoadMore = false)
            }

            updateAccountsAndBalances(updateBalancesForNetwork =  network)
            updateWalletTransactions(updateForNetwork = network)
        }
    }

    private fun onLoginSuccess(
        loginData: LoginData?,
        initNetwork: String?,
        initAccount: Long?,
        initializeSession: Boolean
    ) {
        isConnected = true
        walletHashId = if(isWatchOnly) loginData?.networkHashId else loginData?.walletHashId

        if(initializeSession) {
            initializeSessionData(initNetwork, initAccount)
        }

        // Allow initialization calls to have priority over notifications initiated updates (getWalletTransactions & updateAccountAndBalances)
        blockNotificationHandling = false
    }

    private fun initializeSessionData(initNetwork:String?, initAccount: Long?) {
        // Check if active account index was archived from 1) a different client (multisig) or 2) from cached Singlesig hww session
        // Expect refresh = true to be already called
        updateAccounts()

        _activeAccountStateFlow.value = accounts.find {
            it.networkId == initNetwork && it.pointer == initAccount && !it.hidden
        } ?: accounts.firstOrNull() ?: allAccounts.firstOrNull()

        // Update Liquid Assets from GDK before getting balances to sort them properly
        updateLiquidAssets()

        // Init singlesig exchange rates
        activeBitcoinSinglesig?.also {
            convertAmount(it.policyAsset, Convert(satoshi = 1L))
        }

        // Init singlesig exchange rates
        activeLiquidSinglesig?.also {
            convertAmount(it.policyAsset, Convert(satoshi = 1L), true)
        }

        if(!isWatchOnly) {
            // Cache 2fa config
            activeBitcoinMultisig?.also {
                getTwoFactorConfig(network = it, useCache = false)
            }

            // Cache 2fa config
            activeLiquidMultisig?.also {
                getTwoFactorConfig(network = it, useCache = false)
            }

            // Sync settings from prominent network to the rest
            syncSettings()
        }

        updateAccountsAndBalances(
            isInitialize = true,
            refresh = false,
        )

        updateSystemMessage()
        updateWalletTransactions()
    }

    fun updateLiquidAssets() {
        if (liquid != null) {
            networkAssetManager.updateAssetsIfNeeded(this)
        }
    }

    fun updateSystemMessage(){
        scope.launch(context = Dispatchers.IO) {
            _systemMessageStateFlow.value = gdkSessions.map {
                it.key to gdkBridge.getSystemMessage(it.value)
            }.filter { !it.second.isNullOrBlank() }
        }
    }

    fun ackSystemMessage(network: Network, message: String) = authHandler(
        network,
        gdkBridge.ackSystemMessage(gdkSession(network), message)
    ).resolve()

    fun setTransactionMemo(network: Network, txHash: String, memo: String): Boolean = try{
        gdkBridge.setTransactionMemo(gdkSession(network), txHash, memo)
        true
    }catch (e: Exception){
        e.printStackTrace()
        false
    }

    fun getWalletIdentifier(
        network: Network? = null,
        loginCredentialsParams: LoginCredentialsParams
    ) = gdkBridge.getWalletIdentifier(
        createConnectionParams(network ?: defaultNetwork),
        loginCredentialsParams
    )

    fun encryptWithPin(network: Network?, encryptWithPinParams: EncryptWithPinParams): EncryptWithPin {
        @Suppress("NAME_SHADOWING")
        val network = network ?: defaultNetwork

        return authHandler(
            network,
            gdkBridge.encryptWithPin(gdkSession(network), encryptWithPinParams)
        ).result<EncryptWithPin>().also {
            it.networkInjected = (network)
        }
    }

    fun decryptCredentialsWithPin(network: Network? = null, decryptWithPinParams: DecryptWithPinParams): Credentials {
        @Suppress("NAME_SHADOWING")
        val network = network ?: defaultNetwork

        return authHandler(
            network,
            gdkBridge.decryptWithPin(gdkSession(network), decryptWithPinParams)
        ).result<Credentials>()
    }

    fun getCredentials(params: CredentialsParams = CredentialsParams()): Credentials {
        val network = defaultNetwork.takeIf { hasActiveNetwork(defaultNetwork) } ?: activeSessions.first()

        return authHandler(network, gdkBridge.getCredentials(gdkSession(network), params))
            .result<Credentials>()
    }

    fun getReceiveAddress(account: Account) = authHandler(
        account.network,
        gdkBridge.getReceiveAddress(gdkSession(account.network),
            ReceiveAddressParams(account.pointer)
        )
    ).result<Address>()

    fun getPreviousAddresses(account: Account, lastPointer: Int?) = authHandler(
        account.network,
        gdkBridge.getPreviousAddress(gdkSession(account.network),
            PreviousAddressParams(account.pointer, lastPointer = lastPointer)
        )
    ).result<PreviousAddresses>()

    override fun refreshAssets(params: AssetsParams) = gdkBridge.refreshAssets(gdkSession(activeLiquid ?: liquid!!), params)
    override fun getAssets(params: GetAssetsParams) = gdkBridge.getAssets(gdkSession(activeLiquid ?: liquid!!), params)

    fun createAccount(
        network: Network,
        params: SubAccountParams,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): Account {
        return initNetworkIfNeeded(network) {
            authHandler(network, gdkBridge.createSubAccount(gdkSession(network), params))
                .result<Account>(
                    hardwareWalletResolver = hardwareWalletResolver
                ).also {
                    it.networkInjected = network
                }
        }.also {
            walletActiveEventInvalidated = true

            // Update account list
            updateAccounts()

            listOf(it).also {
                // Update newly created account
                updateAccountsAndBalances(updateBalancesForAccounts = it)
                updateWalletTransactions(updateForAccounts = it)
            }
        }
    }

    private fun getAccounts(refresh: Boolean = false): List<Account> = runBlocking {
        activeGdkSessions.map {
            scope.async(context = Dispatchers.IO) {
                getAccounts(it.key, refresh)
            }
        }.awaitAll().flatten().sortedWith(::sortAccounts)
    }

    fun getAccounts(network: Network, refresh: Boolean = false): List<Account> = initNetworkIfNeeded(network) {
        gdkSession(network).let {
            authHandler(network, gdkBridge.getSubAccounts(it, SubAccountsParams(refresh = refresh)))
                .result<Accounts>().accounts.onEach { account ->
                    account.networkInjected = network
                }
        }
    }

    fun getAccount(account: Account) = authHandler(
        account.network, gdkBridge.getSubAccount(gdkSession(account.network), account.pointer)
    ).result<Account>().also {
        it.networkInjected = account.network

        // Update active account if needed
        if(_activeAccountStateFlow.value?.id == it.id){
            _activeAccountStateFlow.value = it
        }
    }

    fun setActiveAccount(account: Account){
        // Get the account from the list of accounts just to be sure we have the latest account data
        _activeAccountStateFlow.value = accounts.find { it.id == account.id } ?: account
    }

    fun updateAccount(
        account: Account,
        isHidden: Boolean,
        resetAccountName: String? = null
    ): Account {
        gdkBridge.updateSubAccount(
            gdkSession(account.network), if (resetAccountName.isNullOrBlank()) {
                UpdateSubAccountParams(
                    hidden = isHidden,
                    subaccount = account.pointer
                )
            } else {
                UpdateSubAccountParams(
                    hidden = isHidden,
                    name = resetAccountName,
                    subaccount = account.pointer
                )
            }
        )
        // Update account list
        updateAccounts()

        return getAccount(account).also {
            listOf(it).also {
                // Update newly created account
                updateAccountsAndBalances(updateBalancesForAccounts = it)

                // Update wallet transactions
                updateWalletTransactions(updateForAccounts = it)
            }
        }
    }

    fun updateAccount(account: Account, name: String): Account {
        gdkBridge.updateSubAccount(
            gdkSession(account.network), UpdateSubAccountParams(
                name = name,
                subaccount = account.pointer
            )
        )

        updateAccounts()

        return getAccount(account).also {
            // Update wallet transactions so that the tx have the new injected account
            updateWalletTransactions()
        }
    }

    fun getFeeEstimates(network: Network) = try {
        gdkBridge.getFeeEstimates(gdkSession(network))
    } catch (e: Exception) {
        e.printStackTrace()
        FeeEstimation(fees = listOf(network.defaultFee))
    }

    fun getTransactions(network: Network, params: TransactionParams) = authHandler(network, gdkBridge.getTransactions(gdkSession(network), params))
        .result<Transactions>()

    private val accountsAndBalancesMutex = Mutex()
    fun updateAccountsAndBalances(
        isInitialize: Boolean = false,
        refresh: Boolean = false,
        updateBalancesForNetwork: Network? = null,
        updateBalancesForAccounts: Collection<Account>? = null
    ) {

        scope.launch(context = Dispatchers.IO) {

            try{
                accountsAndBalancesMutex.withLock {

                    // Update accounts
                    updateAccounts(refresh = refresh)

                    for (account in allAccounts) {
                        if((updateBalancesForAccounts == null && updateBalancesForNetwork == null) || updateBalancesForAccounts?.find { account.id == it.id } != null || account.network == updateBalancesForNetwork) {
                            getBalance(account = account, cacheAssets = isInitialize).also {
                                accountAssetsStateFlow(account).value = it
                            }
                        }
                    }

                    // Wallet Assets
                    val walletAssets = linkedMapOf<String, Long>()
                    accounts.forEach { account ->
                        accountAssetsFlow(account).value.forEach { (key, value) ->
                            walletAssets[key] = (walletAssets[key] ?: 0) + value
                        }
                    }

                    if(isInitialize) {
                        // Cache wallet assets (again) + Enriched assets + liquid asset if network exists
                        (walletAssets.keys +
                                (enrichedAssets.keys.takeIf { liquid != null } ?: emptyList()) +
                                listOfNotNull(liquid?.policyAsset))
                            .toSet().also {
                                cacheAssets(it)
                            }
                    }

                    walletAssets.toSortedMap(::sortAssets).also {
                        _walletAssetsFlow.value = it
                    }

                    val accountAndAssets = mutableListOf<AccountAsset>()
                    accounts.forEach { account ->
                        accountAssets(account).keys.forEach { assetId ->
                            accountAndAssets += AccountAsset(account, assetId)
                        }
                    }

                    // Mark it if necessary
                    if(!walletHasHistory){
                        if(walletAssets.size > 2 || walletAssets.values.sum() > 0L) {
                            _walletHasHistorySharedFlow.value = true
                        }
                    }

                    _accountAssetStateFlow.value = accountAndAssets.sortedWith(::sortAccountAssets)

                    _accountsAndBalanceUpdatedSharedFlow.emit(Unit)

                    walletActiveEventIfNeeded()
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private val transactionsMutex = Mutex()
    fun getTransactions(account: Account, isReset : Boolean, isLoadMore: Boolean) {
        scope.launch(context = Dispatchers.IO) {
            val transactionsPagerSharedFlow = accountTransactionsPagerSharedFlow(account)
            val transactionsStateFlow = accountTransactionsStateFlow(account)

            try {
                transactionsMutex.withLock {
                    val txSize = transactionsStateFlow.value.let {
                        if(it.size == 1 && it[0].isLoadingTransaction){
                            0
                        }else{
                            it.size
                        }
                    }

                    var offset = 0

                    if (isReset) {
                        // transactionsStateFlow.value = listOf(Transaction.LoadingTransaction)
                        offset = 0
                    } else if (isLoadMore) {
                        offset = txSize
                    }

                    val limit = if (isReset || isLoadMore) TRANSACTIONS_PER_PAGE else (txSize + TRANSACTIONS_PER_PAGE)

                    val transactions = getTransactions(account.network, TransactionParams(subaccount = account.pointer, offset = offset, limit = limit)).transactions.onEach { tx ->
                        tx.accountInjected = account
                    }

                    // Update transactions
                    transactionsStateFlow.value = if(isLoadMore){
                        transactionsStateFlow.value + transactions
                    }else{
                        transactions
                    }

                    // Update pager
                    if(isReset || isLoadMore){
                        transactionsPagerSharedFlow.emit(transactions.size == TRANSACTIONS_PER_PAGE)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                // Re-set the list to unblock endless loader
                transactionsPagerSharedFlow.emit(false)
            }
        }
    }

    private val walletTransactionsMutex = Mutex()
    private val walletTransactions = mutableMapOf<AccountId, List<Transaction>>()
    fun updateWalletTransactions(updateForNetwork: Network? = null, updateForAccounts: Collection<Account>? = null) {
        scope.launch(context = Dispatchers.IO) {
            try {
                walletTransactionsMutex.withLock {
                    // Clear walletTransactions to avoid keeping archived accounts
                    if (updateForAccounts == null && updateForNetwork == null) {
                        walletTransactions.clear()
                    }

                    allAccounts
                        .filter { account ->
                            ((updateForNetwork == null && updateForAccounts == null) || updateForAccounts?.find { account.id == it.id } != null || account.network == updateForNetwork)
                        }
                        .onEach { account ->
                            if(account.hidden){
                                // Clear transactions
                                walletTransactions.remove(account.id)
                            }else {
                                walletTransactions[account.id] = getTransactions(
                                    account.network,
                                    TransactionParams(subaccount = account.pointer, limit = WALLET_OVERVIEW_TRANSACTIONS)
                                ).transactions.onEach { tx ->
                                    tx.accountInjected = account
                                }
                            }
                        }

                    var walletTransactions = walletTransactions.values.flatten()

                    walletTransactions = walletTransactions.sortedWith(::sortTransactions).let {
                        it.subList(0, it.size.coerceAtMost(WALLET_OVERVIEW_TRANSACTIONS))
                    }

                    if(walletTransactions.isNotEmpty()){
                        _walletHasHistorySharedFlow.value = true
                    }
                    _walletTransactionsStateFlow.value = walletTransactions
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sortTransactions(t1: Transaction, t2: Transaction): Int =
        when {
            t1.blockHeight == 0L && t2.blockHeight > 0L -> -1
            t2.blockHeight == 0L && t1.blockHeight > 0L -> 1
            t1.blockHeight == t2.blockHeight && t1.createdAt == t2.createdAt -> { // if we send to the same account, display first the outgoing tx
                if (t1.isIn && t2.isOut) {
                    -1
                } else if (t2.isIn && t1.isOut) {
                    1
                } else {
                    t2.createdAt.compareTo(t1.createdAt)
                }
            }
            else -> t2.createdAt.compareTo(t1.createdAt)
        }

    fun sortUtxos(a1: Utxo, a2: Utxo): Int = sortAssets(a1.assetId, a2.assetId)


    fun sortAssets(a1: EnrichedAsset, a2: EnrichedAsset): Int {
        return sortAssets(a1.assetId, a2.assetId)
    }

    private fun sortAssets(a1: String, a2: String): Int = when {
            a1 == BTC_POLICY_ASSET && a2 == BTC_POLICY_ASSET -> 0
            a1 == BTC_POLICY_ASSET -> -1
            a2 == BTC_POLICY_ASSET -> 1
            a1.isPolicyAsset(this) && a2.isPolicyAsset(this) -> 0 // Liquid
            a1.isPolicyAsset(this) -> -1 // Liquid
            a2.isPolicyAsset(this) -> 1 // Liquid
            else -> {
                val asset1 = networkAssetManager.getAsset(a1, this)
                val icon1 = networkAssetManager.getAssetIcon(a1, this)

                val asset2 = networkAssetManager.getAsset(a2, this)
                val icon2 = networkAssetManager.getAssetIcon(a2, this)

                if ((icon1 == null) xor (icon2 == null)) {
                    if (icon1 != null) -1 else 1
                } else if ((asset1 == null) xor (asset2 == null)) {
                    if (asset1 != null) -1 else 1
                } else if (asset1 != null && asset2 != null) {
                    val weight1 = enrichedAssets[a1]?.weight ?: 0
                    val weight2 = enrichedAssets[a2]?.weight ?: 0

                    if(weight1 == weight2){
                        asset1.name.compareTo(asset2.name)
                    }else{
                        weight2.compareTo(weight1)
                    }
                } else {
                    a1.compareTo(a2)
                }
            }
        }

    private fun sortAccounts(a1: Account, a2: Account): Int = when {
        a1.isBitcoin xor a2.isBitcoin -> {
            if(a1.isBitcoin) -1 else 1
        }
        else -> {
            when{
                a1.isSinglesig xor a2.isSinglesig -> {
                    if(a1.isSinglesig) -1 else 1
                }
                else -> {
                    if(a1.isMultisig && a1.isAmp.xor(a2.isAmp)){
                        if(a1.isAmp && !a2.isAmp) 1 else -1
                    }else{
                        a1.pointer.compareTo(a2.pointer)
                    }
                }
            }

        }
    }

    private fun sortAccountAssets(a1: AccountAsset, a2: AccountAsset): Int = when {
        a1.account.isBitcoin && a2.account.isLiquid -> -1
        a1.account.isLiquid && a2.account.isBitcoin -> 1
        a1.assetId == a2.assetId -> sortAccounts(a1.account, a2.account)
        else -> {
            sortAssets(a1.assetId, a2.assetId)
        }
    }

    fun getBalance(account: Account, confirmations: Int = 0, cacheAssets: Boolean = false): Assets {
        val assets = authHandler(
            account.network, gdkBridge.getBalance(
                gdkSession(account.network), BalanceParams(
                    subaccount = account.pointer,
                    confirmations = confirmations
                )
            )
        ).result<Map<String, Long>>()

        if(cacheAssets) {
            // Cache assets before sorting them, as the sort function uses the asset metadata
            cacheAssets(assets.keys)
        }

        return assets.toSortedMap(::sortAssets)
    }

    private fun getBalance(network: Network, params: BalanceParams): Assets {
        authHandler(network, gdkBridge.getBalance(gdkSession(network), params)).resolve()
            .result<Map<String, Long>>().let { balanceMap ->
                return balanceMap.toSortedMap(::sortAssets)
            }
    }

    fun changeSettingsTwoFactor(network: Network, method: String, methodConfig: TwoFactorMethodConfig) =
        authHandler(network, gdkBridge.changeSettingsTwoFactor(
                gdkSession(network),
                method,
                methodConfig
            )
        )

    fun getTwoFactorConfig(network: Network, useCache: Boolean = false): TwoFactorConfig{
        if(!useCache || !_twoFactorConfigCache.contains(network)){
            _twoFactorConfigCache[network] = gdkBridge.getTwoFactorConfig(gdkSession(network))
        }

        return _twoFactorConfigCache[network]!!
    }

    fun getWatchOnlyUsername(network: Network) = gdkBridge.getWatchOnlyUsername(gdkSession(network))

    fun setWatchOnly(network: Network, username: String, password: String) = gdkBridge.setWatchOnly(
        gdkSession(network),
        username,
        password
    )

    fun twofactorReset(network: Network, email:String, isDispute: Boolean) =
        authHandler(network, gdkBridge.twofactorReset(gdkSession(network), email, isDispute))

    fun twofactorUndoReset(network: Network, email: String) =
        authHandler(network, gdkBridge.twofactorUndoReset(gdkSession(network), email))

    fun twofactorCancelReset(network: Network) =
        authHandler(network, gdkBridge.twofactorCancelReset(gdkSession(network)))

    fun twofactorChangeLimits(network: Network, limits: Limits) =
        authHandler(network, gdkBridge.twofactorChangeLimits(gdkSession(network), limits))

    fun sendNlocktimes(network: Network) = gdkBridge.sendNlocktimes(gdkSession(network))

    fun setCsvTime(network: Network, value: Int) =
        authHandler(network, gdkBridge.setCsvTime(gdkSession(network), value))

    private fun updateAccounts(refresh: Boolean = false) {
        getAccounts(refresh).also { fetchedAccounts ->
            _allAccountsStateFlow.value = fetchedAccounts
            _accountsStateFlow.value = fetchedAccounts.filter { !it.hidden }

            // Update active account to get fresh data, also prevent it from being archived and active
            _activeAccountStateFlow.value = accounts.find {
                activeAccountOrNull?.id == it.id
            } ?: accounts.firstOrNull() ?: allAccounts.firstOrNull()
        }
    }

    // asset_info in Convert object can be null for liquid assets that don't have asset metadata
    // if no asset is given, no conversion is needed (conversion will be identified as a btc value in gdk)
    fun convertAmount(network: Network, convert: Convert, isAsset: Boolean = false) = try {
        if(isAsset && convert.asset == null){
            Balance.fromAssetWithoutMetadata(convert)
        }else if(isAsset && convert.assetAmount != null){
            val jsonElement = buildJsonObject {
                put("asset_info", convert.asset!!.toJsonElement())
                put(convert.asset?.assetId ?: "", convert.assetAmount)
            }
            gdkBridge.convertAmount(gdkSession(network), convert, jsonElement)
        } else{
            gdkBridge.convertAmount(gdkSession(network), convert)
        }
    }catch (e: Exception){
        e.printStackTrace()
        null
    }

    fun convertAmount(assetId: String?, convert: Convert, isAsset: Boolean = false): Balance? {
        return convertAmount(assetId.networkForAsset(this@GdkSession), convert, isAsset)
    }

    fun getUnspentOutputs(network: Network, params: BalanceParams) = authHandler(
        network,
        gdkBridge.getUnspentOutputs(gdkSession(network), params)
    ).result<UnspentOutputs>().also {
        it.fillUtxosJsonElement()
    }

    fun getUnspentOutputs(account: Account, isBump: Boolean = false): UnspentOutputs {
        return getUnspentOutputs(account.network, BalanceParams(
            subaccount = account.pointer,
            confirmations = if(isBump) 1 else 0)
        )
    }

    fun getUnspentOutputs(accounts: List<Account>): UnspentOutputs {
        return accounts.map {
            getUnspentOutputs(it)
        }.reduce { unspentOutputs1, unspentOutputs2 ->
            unspentOutputs1 + unspentOutputs2
        }
    }

    fun createTransaction(network: Network, subAccount: SubAccount, unspentOutputs: UnspentOutputs, addresses: List<AddressParams>): CreateTransaction {
        val params = CreateTransactionParams(
            subaccount = subAccount.pointer,
            utxos = unspentOutputs.unspentOutputsAsJsonElement,
            addressees = addresses
        )

        return authHandler(
            network,
            gdkBridge.createTransaction(gdkSession(network), params)
        ).result<CreateTransaction>()
    }

    fun createTransaction(network: Network, params: GAJson<*>) = authHandler(
        network,
        gdkBridge.createTransaction(gdkSession(network), params)
    ).result<CreateTransaction>()

    fun createSwapTransaction(network: Network, params: CreateSwapParams, twoFactorResolver: TwoFactorResolver) = authHandler(
        network,
        gdkBridge.createSwapTransaction(gdkSession(network), params)
    ).result<CreateSwapTransaction>(twoFactorResolver = twoFactorResolver)

    fun completeSwapTransaction(network: Network, params: CompleteSwapParams, twoFactorResolver: TwoFactorResolver) = authHandler(
        network,
        gdkBridge.completeSwapTransaction(gdkSession(network), params)
    ).result<CreateTransaction>(twoFactorResolver = twoFactorResolver)

    fun updateCreateTransaction(network: Network, createTransaction: CreateTransaction) =
        authHandler(
            network,
            gdkBridge.updateTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>()

    fun signTransaction(network: Network, createTransaction: CreateTransaction) =
        authHandler(
            network,
            gdkBridge.signTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>()

    fun broadcastTransaction(network: Network, transaction: String) = gdkBridge.broadcastTransaction(gdkSession(network), transaction)

    fun sendTransaction(network: Network, createTransaction: CreateTransaction, twoFactorResolver: TwoFactorResolver) =
        authHandler(
            network,
            gdkBridge.sendTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>(twoFactorResolver = twoFactorResolver)

    private fun removeGdkCache(loginData: LoginData){
        gdkBridge.removeGdkCache(loginData)
    }

    fun recordException(throwable: Throwable) {
        countly.recordException(throwable)
    }

    private fun walletActiveEventIfNeeded(){
        if(walletActiveEventInvalidated) {
            countly.activeWallet(
                session = this,
                walletAssets = walletAssets,
                accountAssets = _accountAssetsFlow.mapValues { entry -> entry.value.value },
                accounts = allAccounts
            )
            walletActiveEventInvalidated = false
        }
    }

    fun onNewNotification(gaSession: GASession, notification: Notification) {

        val network = gdkSessions.firstNotNullOfOrNull { if(it.value == gaSession) it.key else null } ?: return

        logger.info { "onNewNotification ${network.id} \t $notification" }

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    // SingleSig after connect immediatelly sends a block with height 0
                    // it's not safe to call getTransactions so early
                    if(it.height > 0) {
                        blockStateFlow(network).value = it

                        if(!blockNotificationHandling) {
                            // Update transactions
                            accountsFlow.value.filter { it.network == network }.also { accounts ->
                                accounts.forEach {
                                    // Update account transactions
                                    getTransactions(
                                        account = it,
                                        isReset = false,
                                        isLoadMore = false
                                    )
                                }

                                updateAccountsAndBalances(updateBalancesForAccounts = accounts)

                                // Update wallet transactions
                                updateWalletTransactions(updateForAccounts = accounts)
                            }
                        }
                    }
                }
            }
            "settings" -> {
                notification.settings?.let {
                    settingsStateFlow(network).value = it
                }
            }
            "twofactor_reset" -> {
                notification.twoFactorReset?.let {
                    twoFactorResetStateFlow(network).value = it
                }
            }
            "tor" -> {
                // Get TOR notification only from the default network
                if(network == defaultNetwork) {
                    notification.tor?.let {
                        _torStatusSharedFlow.value = it
                    }
                }
            }
            "network" -> {
                notification.network?.let { event ->
                    if(isConnected){
                        if(event.isConnected && authenticationRequired[network] == true){
                            scope.launch(context = Dispatchers.IO){
                                reLogin(network)
                            }
                        }else if(!event.isConnected){
                            // mark re-authentication is required
                            authenticationRequired[network] = true
                        }
                    }

                    networkEventsStateFlow(network).value = event
                }
            }
            "ticker" -> {
                // Update UI maybe
                _tickerSharedFlow.tryEmit(Unit)
            }
            "transaction" -> {
                if (!blockNotificationHandling) {
                    notification.transaction?.let { event ->
                        event.subaccounts.mapNotNull { subAccount ->
                            accountsFlow.value.find {
                                it.network == network && it.pointer == subAccount
                            }
                        }.toSet().also { accounts ->
                            accounts.forEach {
                                // Update account transactions
                                getTransactions(account = it, isReset = false, isLoadMore = false)
                            }

                            updateAccountsAndBalances(updateBalancesForAccounts = accounts)

                            // Update wallet transactions
                            updateWalletTransactions(updateForAccounts = accounts)
                        }
                    }
                }
            }
        }
    }

    private fun cacheAssets(assetIds: Collection<String>) {
        assetIds.filter { it != BTC_POLICY_ASSET }.takeIf { it.isNotEmpty() }?.also {
            logger.info { "Cache assets: $it" }
            networkAssetManager.cacheAssets(it, this)
        }
    }

    fun hasAssetIcon(assetId : String) = networkAssetManager.hasAssetIcon(assetId)
    fun getAsset(assetId : String): Asset? = networkAssetManager.getAsset(assetId, this)
    fun getAssetDrawableOrNull(assetId : String): Drawable? = networkAssetManager.getAssetDrawableOrNull(assetId, this)
    fun getAssetDrawableOrDefault(assetId : String): Drawable = networkAssetManager.getAssetDrawableOrDefault(assetId, this)

    internal fun destroy() {
        disconnect()
        scope.cancel("Destroy")
    }

    companion object: KLogging(){
        const val WALLET_OVERVIEW_TRANSACTIONS = 10

        const val LIQUID_ASSETS_KEY = "liquid_assets"
        const val LIQUID_ASSETS_TESTNET_KEY = "liquid_assets_testnet"

        private val AssetLoading = "" to -1L
        val AssetsLoading: Assets = mapOf(AssetLoading)
    }
}

fun NetworkLayer.hasActiveNetwork(session: GdkSession): Boolean{
    return session.hasActiveNetwork(this.network(session))
}

fun NetworkLayer.network(session: GdkSession): Network? = if (this is NetworkPolicy) {
    if (this.isSinglesig) {
        if (this.isBitcoin) session.bitcoinSinglesig else session.liquidSinglesig
    } else if (this.isMultisig) {
        if (this.isBitcoin) session.bitcoinMultisig else session.liquidMultisig
    } else {
        TODO("Lightning not supported yet")
    }
} else {
    if (this.isBitcoin) session.bitcoin else session.liquid
}
fun NetworkLayer.policyAsset(session: GdkSession): String = if(this.isBitcoin) BTC_POLICY_ASSET else session.liquid?.policyAsset ?: ""

fun AccountAsset.asset(session: GdkSession) = session.getAsset(assetId)
fun AccountAsset.assetName(session: GdkSession) = assetId.getAssetName(session)
fun AccountAsset.assetTicker(session: GdkSession) = assetId.getAssetTicker(session)
fun AccountAsset.balance(session: GdkSession) = session.accountAssets(account).firstNotNullOfOrNull { if(it.key == assetId) it.value else null } ?: 0L