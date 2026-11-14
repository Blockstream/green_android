package com.blockstream.data.gdk

import com.blockstream.data.BITS_UNIT
import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.BTC_UNIT
import com.blockstream.data.CountlyBase
import com.blockstream.data.LN_BTC_POLICY_ASSET
import com.blockstream.data.MBTC_UNIT
import com.blockstream.data.SATOSHI_UNIT
import com.blockstream.data.UBTC_UNIT
import com.blockstream.data.backend.AccountBackend
import com.blockstream.data.backend.AmountConverter
import com.blockstream.data.backend.NetworkBackend
import com.blockstream.data.backend.combineBackends
import com.blockstream.data.backend.gdk.GdkAccountBackend
import com.blockstream.data.backend.gdk.GdkNetworkBackend
import com.blockstream.data.backend.gl.GlAccountBackend
import com.blockstream.data.backend.gl.GlNetworkBackend
import com.blockstream.data.backend.lwk.LwkNetworkBackend
import com.blockstream.data.backend.mapLoggedIn
import com.blockstream.data.data.AppConfig
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.LogoutReason
import com.blockstream.data.data.MultipleWatchOnlyCredentials
import com.blockstream.data.data.RichWatchOnly
import com.blockstream.data.database.Database
import com.blockstream.data.database.wallet.LoginCredentials
import com.blockstream.data.devices.DeviceBrand
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.devices.DeviceState
import com.blockstream.data.devices.GreenDevice
import com.blockstream.data.extensions.hasHistory
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.extensions.logException
import com.blockstream.data.extensions.networkForAsset
import com.blockstream.data.extensions.title
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Asset
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.gdk.data.Balance
import com.blockstream.data.gdk.data.BcurDecodedData
import com.blockstream.data.gdk.data.BcurEncodedData
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.CreateSwapTransaction
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.EncryptWithPin
import com.blockstream.data.gdk.data.FeeEstimation
import com.blockstream.data.gdk.data.LoginData
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.NetworkEvent
import com.blockstream.data.gdk.data.Notification
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.gdk.data.PreviousAddresses
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.Psbt
import com.blockstream.data.gdk.data.RsaVerify
import com.blockstream.data.gdk.data.Settings
import com.blockstream.data.gdk.data.SignMessage
import com.blockstream.data.gdk.data.TorEvent
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.TwoFactorConfig
import com.blockstream.data.gdk.data.TwoFactorMethodConfig
import com.blockstream.data.gdk.data.TwoFactorReset
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.data.WalletEvents
import com.blockstream.data.gdk.data.sort
import com.blockstream.data.gdk.device.GdkHardwareWallet
import com.blockstream.data.gdk.device.HardwareWalletInteraction
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.BcurDecodeParams
import com.blockstream.data.gdk.params.BcurEncodeParams
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.CompleteSwapParams
import com.blockstream.data.gdk.params.ConnectionParams
import com.blockstream.data.gdk.params.Convert
import com.blockstream.data.gdk.params.CreateSwapParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.CredentialsParams
import com.blockstream.data.gdk.params.CsvParams
import com.blockstream.data.gdk.params.DecryptWithPinParams
import com.blockstream.data.gdk.params.DeviceParams
import com.blockstream.data.gdk.params.EncryptWithPinParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.gdk.params.Limits
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.gdk.params.RsaVerifyParams
import com.blockstream.data.gdk.params.SignMessageParams
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.data.lightning.ConnectStatus
import com.blockstream.data.lightning.GreenlightMnemonicAndCredentials
import com.blockstream.data.lightning.LightningEvent
import com.blockstream.data.lightning.LightningFees
import com.blockstream.data.lightning.LightningInputType
import com.blockstream.data.lightning.LightningManager
import com.blockstream.data.lightning.LightningReceivePayment
import com.blockstream.data.lightning.LightningSdk
import com.blockstream.data.lwk.Lwk
import com.blockstream.data.lwk.LwkManager
import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.data.managers.AssetManager
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.data.utils.randomChars
import com.blockstream.data.utils.server
import com.blockstream.data.utils.toHex
import com.blockstream.jade.HttpRequestHandler
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/* Handles multiple GDK sessions per network */
class GdkSession constructor(
    private val userAgent: String,
    private val appConfig: AppConfig,
    private val sessionManager: SessionManager,
    private val lightningManager: LightningManager,
    private val lwkManager: LwkManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val walletSettingsManager: WalletSettingsManager,
    private val database: Database,
    private val gdk: Gdk,
    private val wally: Wally,
    private val countly: CountlyBase
) : HttpRequestHandler, AssetsProvider {
    private fun createScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) =
        CoroutineScope(SupervisorJob() + dispatcher + logException(countly))

    private val scope = createScope(Dispatchers.Default)
    private val parentJob = SupervisorJob()

    val isTestnet: Boolean // = false
        get() = defaultNetworkOrNull?.isTestnet == true

    val isMainnet: Boolean
        get() = !isTestnet

    private val _isWatchOnly: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isWatchOnly: StateFlow<Boolean> = _isWatchOnly
    val isWatchOnlyValue: Boolean
        get() = isWatchOnly.value

    var isHwWatchOnly: Boolean = false
        private set
    var isNoBlobWatchOnly: Boolean = false
        private set
    var isRichWatchOnly: Boolean = false
        private set
    var isCoreDescriptorWatchOnly: Boolean = false
        private set
    var isAirgapped: Boolean = false
        private set

    val isHwWatchOnlyWithNoDevice: Boolean
        get() = isHwWatchOnly && device == null

    val canSendTransaction: Boolean
        get() = !isWatchOnlyValue || (isAirgapped && (isCoreDescriptorWatchOnly && !defaultNetwork.isLiquid)) || isRichWatchOnly

    //  Disable notification handling until all networks are initialized
    private var _disableNotificationHandling = false

    private val _eventsSharedFlow = MutableSharedFlow<WalletEvents>()
    val eventsSharedFlow = _eventsSharedFlow.asSharedFlow()

    private var _walletTotalBalanceSharedFlow = MutableStateFlow(-1L)
    private val _accountsAndBalanceUpdatedSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    private var _failedNetworksStateFlow: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    private val _isLightningLoadingStateFlow = MutableStateFlow(false)

    val networkBackendsStateFlow : StateFlow<Map<Network, NetworkBackend>>
        field = MutableStateFlow<Map<Network, NetworkBackend>>(emptyMap())
    private val _lastInvoicePaid = MutableStateFlow<Pair<String, Long?>?>(null)
    private val _torStatusSharedFlow = MutableStateFlow<TorEvent>(TorEvent(progress = 100))


    var logoutReason: LogoutReason? = null
        private set

    private fun blockStateFlow(network: Network) = networkBackend(network).blockStateFlow

    val walletTotalBalance get() = _walletTotalBalanceSharedFlow.asStateFlow()
    val walletTotalBalanceDenominationStateFlow = MutableStateFlow<Denomination>(Denomination.BTC)

    // Fallback when a network backend is deregistered (eg. Lightning disabled).
    private val emptyAssetsStateFlow: StateFlow<Assets> = MutableStateFlow(Assets(emptyMap()))

    fun accountAssets(account: Account): StateFlow<Assets> =
        accountBackendOrNull(account)?.assets ?: emptyAssetsStateFlow

    val accountsAndBalanceUpdated get() = _accountsAndBalanceUpdatedSharedFlow.asSharedFlow()

    val failedNetworks get() = _failedNetworksStateFlow.asStateFlow()

    val isLightningLoading get() = _isLightningLoadingStateFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val systemMessage: StateFlow<List<Pair<Network, String>>> =
        networkBackendsStateFlow
            .combineBackends<GdkNetworkBackend, Pair<Network, String>> { backend ->
                backend.systemMessage.map { msg ->
                    msg?.takeIf { it.isNotBlank() }
                        ?.let { listOf(backend.network to it) }
                        ?: emptyList()
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allAccounts: StateFlow<List<Account>> =
        networkBackendsStateFlow
            .combineBackends<NetworkBackend, Account> { it.accounts }
            .map { accounts -> accounts.sorted() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val accounts: StateFlow<List<Account>> =
        allAccounts
            .map { list -> list.filter { !it.hidden } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val expired2FA: StateFlow<List<Account>> =
        networkBackendsStateFlow
            .combineBackends<GdkNetworkBackend, Account> { it.expired2FA }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val lastInvoicePaid = _lastInvoicePaid.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountAsset: StateFlow<List<AccountAsset>> =
        accounts
            .flatMapLatest { accs ->
                if (accs.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(accs.map { acc -> accountBackend(acc).assets.map { acc to it } }) { it.toList() }
                }
            }
            .mapLatest { pairs ->
                pairs.flatMap { (acc, assets) -> assets.toAccountAsset(acc, this@GdkSession) }.sort(this)
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val torStatusFlow = _torStatusSharedFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val networkErrors: Flow<Pair<Network, NetworkEvent>> =
        networkBackendsStateFlow.flatMapLatest { backends ->
            backends.values.filterIsInstance<GdkNetworkBackend>().map { backend ->
                backend.networkEvents
                    .filter { event ->
                        backend.network.isSinglesig && !event.isConnected
                                && settingsManager.appSettings.electrumNode
                                && settingsManager.appSettings.getPersonalElectrumServer(backend.network).isNotBlank()
                    }
                    .map { backend.network to it }
            }.merge()
        }

    fun block(network: Network): StateFlow<Block> = blockStateFlow(network)

    fun settings(network: Network? = null): StateFlow<Settings?> =
        (network ?: defaultNetworkOrNull)?.let { gdkNetworkBackendOrNull(it)?.settings } ?: NULL_SETTINGS

    fun twoFactorConfig(network: Network = defaultNetwork): StateFlow<TwoFactorConfig?> =
        gdkNetworkBackend(network).twoFactorConfig

    fun twoFactorReset(network: Network): StateFlow<TwoFactorReset?> =
        gdkNetworkBackend(network).twoFactorReset

    var defaultNetworkOrNull: Network? = null
        private set

    val defaultNetwork: Network
        get() = defaultNetworkOrNull!!

    val lightning: Network by lazy { networks.lightning }

    val bitcoin
        get() = bitcoinSinglesig ?: bitcoinMultisig

    val bitcoinSinglesig
        get() = networkBackends.firstNotNullOfOrNull { it.key.takeIf { network -> network.isElectrum && network.isBitcoin } }

    val bitcoinMultisig
        get() = networkBackends.firstNotNullOfOrNull { it.key.takeIf { network -> network.isMultisig && network.isBitcoin } }

    val liquid
        get() = liquidSinglesig ?: liquidMultisig

    val liquidSinglesig
        get() = networkBackends.firstNotNullOfOrNull { it.key.takeIf { network -> network.isElectrum && network.isLiquid } }

    val liquidMultisig
        get() = networkBackends.firstNotNullOfOrNull { it.key.takeIf { network -> network.isMultisig && network.isLiquid } }

    val liquidAmp2
        get() = networkBackends.firstNotNullOfOrNull { it.key.takeIf { network -> network.isAmp2 } }

    val activeBitcoin get() = activeBitcoinSinglesig ?: activeBitcoinMultisig
    val activeBitcoinSinglesig get() = bitcoinSinglesig?.takeIf { hasActiveNetwork(it) }
    val activeBitcoinMultisig get() = bitcoinMultisig?.takeIf { hasActiveNetwork(it) }
    val activeLiquid get() = activeLiquidSinglesig ?: activeLiquidMultisig
    val activeLiquidSinglesig get() = liquidSinglesig?.takeIf { hasActiveNetwork(it) }
    val activeLiquidMultisig get() = liquidMultisig?.takeIf { hasActiveNetwork(it) }

    val activeMultisig get() = listOfNotNull(activeBitcoinMultisig, activeLiquidMultisig)

    private val networkBackendsMutex = Mutex()

    val networkBackends: Map<Network, NetworkBackend> get() = networkBackendsStateFlow.value
    val gdkNetworkBackends: List<GdkNetworkBackend>
        get() = networkBackendsStateFlow.value.values.filterIsInstance<GdkNetworkBackend>()

    val loggedInGdkNetworkBackends: List<GdkNetworkBackend>
        get() = gdkNetworkBackends.filter {
            it.isLoggedIn
        }

    val activeNetworks: Set<Network>
        get() = networkBackends.keys

    val activeGdkNetworks: Set<Network>
        get() = networkBackends.mapNotNull {
            if (it.value is GdkNetworkBackend && it.value.isConnected) {
                it.key
            } else null
        }.toSet()

    fun hasActiveNetwork(network: Network) = activeNetworks.contains(network) && networkBackend(network).isLoggedIn

    var hasLightning: Boolean = false
        private set

    val hasAmpAccount: Boolean
        get() = accounts.value.find { it.type == AccountType.AMP2_ACCOUNT } != null

    val hasAmpLegacyAccount: Boolean
        get() = accounts.value.find { it.type == AccountType.AMP_LEGACY_ACCOUNT } != null

    private var _lightningAccount: Account? = null

    val lightningAccount: Account
        get() = runBlocking {
            glNetworkBackend().account
        }

    val isHardwareWallet: Boolean
        get() = device != null

    val gdkHwWallet: GdkHardwareWallet?
        get() = device?.gdkHardwareWallet

    var device: GreenDevice? = null
        private set

    var deviceModel: DeviceModel? = null
        private set

    var ephemeralWallet: GreenWallet? = null
        private set

    // Consider as initialized if network is set
    val isNetworkInitialized: Boolean
        get() = defaultNetworkOrNull != null

    val networks
        get() = gdk.networks()

    private val _isConnectedState = MutableStateFlow(false)

    val isConnectedState = _isConnectedState.asStateFlow()
    val isConnected
        get() = isConnectedState.value

    var xPubHashId: String? = null
        private set

    var lightningNodeId: String? = null
        get() = lightningSdkOrNull?.nodeInfoStateFlow?.value?.id.takeIf { it.isNotBlank() } ?: field
        private set

    var pendingTransactionParams: CreateTransactionParams? = null
    var pendingTransaction: PendingTransaction? = null

    val networkAssetManager: NetworkAssetManager
        get() = assetManager.getNetworkAssetManager(defaultNetworkOrNull?.let { isMainnet } ?: true)

    val hideAmounts: Boolean get() = settingsManager.appSettings.hideAmounts

    val starsOrNull: String? get() = "*****".takeIf { hideAmounts }

    private var _accountEmptiedEvent: Account? = null
    private var _walletActiveEventInvalidated = true

    var lightningSdkOrNull: LightningSdk? = null
        private set

    val lightningSdk
        get() = lightningSdkOrNull!!

    /** Null until LWK is initialised (swaps disabled, wallet still connecting, etc.).
     *  Use this for read-only probes (`inspectPaymentInstruction`, `quote`, `refreshSwapInfo`)
     *  that can run before the swap path is committed. */
    var lwkOrNull: Lwk? = null
        private set

    /** Force-unwrapped accessor for action paths (`createNormalSubmarineSwap`, `restorePreparePay`,
     *  `btcToLbtc`, …). The contract is "the caller already verified swaps are enabled and the
     *  flow has reached an action step", so a null here is a programming error and NPE is fine. */
    val lwk: Lwk
        get() = lwkOrNull!!

    private val _tempAllowedServers = mutableListOf<String>()

    init {
        _accountsAndBalanceUpdatedSharedFlow.onEach {
            var walletBalance = 0L

            accounts.value.forEach {
                walletBalance += this.accountAssets(it).value.policyAsset
            }

            _walletTotalBalanceSharedFlow.value = walletBalance
        }.launchIn(scope)

        networkAssetManager.countlyAssetsFlow.onEach {
            updateEnrichedAssets()
        }.launchIn(scope)

        isConnectedState.drop(1).onEach {
            sessionManager.fireConnectionChangeEvent()
        }.launchIn(scope)
    }

    fun watchOnlyDeviceConnect(device: GreenDevice) {
        this.device = device
    }

    private suspend fun updateEnrichedAssets() {
        if (isConnected && (!isNoBlobWatchOnly || isHwWatchOnly)) {
            networkAssetManager.countlyAssetsFlow.value.also {
                cacheAssets(it.map { it.assetId })
            }
        }
    }

    fun setEphemeralWallet(wallet: GreenWallet) {
        ephemeralWallet = wallet
    }

    fun walletExistsAndIsUnlocked(network: Network?) = network?.let { tryCatchNull { getTwoFactorReset(network)?.isActive != true } } ?: false
    fun getTwoFactorReset(network: Network): TwoFactorReset? = twoFactorReset(network).value

    suspend fun getSettings(network: Network? = null): Settings? {
        return (network?.let { it.takeIf { !it.isLightning } ?: bitcoin } ?: defaultNetworkOrNull)?.let { network ->
            gdkNetworkBackend(network).settings.value ?: try {
                updateSettings(network = network)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun updateSettings(network: Network? = null): Settings? {
        loggedInGdkNetworkBackends.filter { network == null || it.network == network }.forEach { backend ->
            // Side effect: backend.getSettings() updates backend.settings flow internally.
            backend.getSettings()
            if (backend.network.isMultisig) {
                updateTwoFactorConfig(network = backend.network)
            }
        }

        return (network?.let { it.takeIf { !it.isLightning } ?: bitcoin } ?: defaultNetworkOrNull)?.let {
            gdkNetworkBackend(it).settings.value
        }
    }

    suspend fun getTwoFactorConfig(network: Network): TwoFactorConfig? {
        return gdkNetworkBackend(network).twoFactorConfig.value ?: try {
            updateTwoFactorConfig(network = network, useCache = false)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun updateTwoFactorConfig(network: Network, useCache: Boolean = false): TwoFactorConfig? {
        if (!network.is2faNetwork) return null

        val cached = if (useCache) gdkNetworkBackend(network).twoFactorConfig.value else null
        if (cached != null) return cached

        return try {
            // Side effect: backend.getTwoFactorConfig() updates twoFactorConfig and twoFactorReset flows.
            gdkNetworkBackend(network).getTwoFactorConfig()
        } catch (e: Exception) {
            e.printStackTrace()
            countly.recordException(e)
            null
        }
    }

    suspend fun changeSettings(network: Network, settings: Settings) =
        gdkNetworkBackend(network).changeSettings(settings)

    suspend fun changeGlobalSettings(settings: Settings) {
        val exceptions = mutableListOf<Exception>()
        loggedInGdkNetworkBackends.forEach { backend ->
            getSettings(backend.network)?.also { networkSettings ->

                if (walletExistsAndIsUnlocked(backend.network)) {
                    try {
                        changeSettings(
                            backend.network,
                            Settings.normalizeFromProminent(
                                networkSettings = networkSettings,
                                prominentSettings = settings
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        exceptions.add(e)
                    }
                }
            }
        }

        updateSettings()

        if (exceptions.isNotEmpty()) {
            throw Exception(exceptions.first().message)
        }
    }

    private suspend fun syncSettings() {
        // Prefer Multisig for initial sync as those networks are synced across devices
        // In case of Lightning Shorcut get settings from parent wallet
        val syncNetwork = activeBitcoinMultisig ?: activeLiquidMultisig ?: defaultNetwork
        val prominentSettings = getSettings(network = syncNetwork)
        prominentSettings?.also {
            try {
                changeGlobalSettings(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(Exception::class)
    suspend fun availableCurrencies() = gdkNetworkBackend(defaultNetwork).getAvailableCurrencies()

    fun prominentNetwork(isTestnet: Boolean) = if (isTestnet) networks.testnetBitcoinElectrum else networks.bitcoinElectrum
    fun prominentNetwork(wallet: GreenWallet, loginCredentials: LoginCredentials? = null) =
        if (loginCredentials != null && loginCredentials.network.isNotBlank()) networkBy(
            loginCredentials.network
        ) else if (wallet.isWatchOnly || wallet.isHardware) networkBy(wallet.activeNetwork) else prominentNetwork(wallet.isTestnet)

    fun networkBy(id: String) = gdk.networks().getNetworkById(id)

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
        val applicationSettings = settingsManager.appSettings

        var electrumUrl: String? = null

        if (network.isElectrum && applicationSettings.electrumNode) {
            electrumUrl = applicationSettings.getPersonalElectrumServer(network).takeIf { it.isNotBlank() }
        }

        val useTor = applicationSettings.tor

        return ConnectionParams(
            networkName = network.id,
            useTor = useTor,
            userAgent = userAgent,
            proxy = if (applicationSettings.proxyEnabled) applicationSettings.proxyUrl ?: "" else "",
            gapLimit = if (network.isSinglesig && applicationSettings.customGapLimitEnabled) applicationSettings.electrumServerGapLimit?.coerceAtLeast(1) else null,
            electrumTls = if (electrumUrl.isNotBlank()) applicationSettings.personalElectrumServerTls else true,
            electrumUrl = electrumUrl,
            electrumOnionUrl = ConnectionParams.electrumOnionUrl(electrumUrl = electrumUrl, useTor = useTor),
            // blobServerUrl = "wss://green-blobserver.staging.blockstream.com/ws".takeIf { appInfo.isDevelopment && network.isSinglesig && network.isTestnet },
            // blobServerOnionUrl = null,
        ).also {
            logger.d { "Connection Params: $it" }
        }
    }

    // Use it only for connected sessions
    fun supportsLightning() = supportsLightning(isWatchOnly = isWatchOnlyValue, device = device)

    val lightningFailedToInitialize
        get() = lightningManager.failedToInitialize

    private fun supportsLightning(isWatchOnly: Boolean, device: GreenDevice?): Boolean {
        return ephemeralWallet == null && appConfig.lightningFeatureEnabled && !isWatchOnly && (device == null || device.deviceModel?.supportsLightningMnemonicDerivation == true) && lightningManager.isAvailable
    }

    fun networks(isTestnet: Boolean, isWatchOnly: Boolean, device: GreenDevice?): List<Network> {
        return if (isTestnet) {
            listOfNotNull(
                networks.testnetBitcoinElectrum,
                networks.testnetBitcoinGreen,
                networks.testnetLiquidElectrum,
                networks.testnetLiquidGreen,
                networks.liquidTestnetAmp2.takeIf { device == null && !isWatchOnly }
            )
        } else {
            listOfNotNull(
                networks.bitcoinElectrum,
                networks.bitcoinGreen,
                networks.liquidElectrum,
                networks.liquidGreen,
                networks.liquidAmp2.takeIf { device == null && !isWatchOnly }
            )
        }
    }

    private suspend fun initNetworkBackends(initNetworks: List<Network>?) {
        val networks = if (initNetworks.isNullOrEmpty()) {
            networks(isTestnet = isTestnet, isWatchOnly = isWatchOnlyValue, device = device)
        } else {
            // init the provided networks
            initNetworks
        }

        networkBackendsMutex.withLock {
            val current = networkBackendsStateFlow.value
            val newBackends = networks.filter { it !in current }.associateWith { network ->
                when {
                    network.isLightning ->
                        throw Exception("GlNetworkBackend should have been initialized first")

                    useLwkFor(network) ->
                        LwkNetworkBackend(
                            dataDir = gdk.dataDir,
                            database = database,
                            gdk = gdk,
                            network = network,
                            networkAssetManager = networkAssetManager,
                        )

                    else ->
                        GdkNetworkBackend(
                            gdk = gdk,
                            gdkHwWallet = gdkHwWallet,
                            network = network,
                            networkAssetManager = networkAssetManager
                        )
                }
            }
            networkBackendsStateFlow.update { it + newBackends }
        }
    }

    suspend fun connect(network: Network, initNetworks: List<Network>? = null): List<Network> {
        defaultNetworkOrNull = network

        disconnect()

        initNetworkBackends(initNetworks = initNetworks)

        // Parallel
        return networkBackendsMutex.withLock {
            networkBackends.values.map { networkBackend ->
                scope.async(start = CoroutineStart.DEFAULT) {
                    try {
                        networkBackend.network.also { network ->
                            networkBackend.connect(createConnectionParams(network))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _failedNetworksStateFlow.value += networkBackend.network
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getProxySettings() = gdkNetworkBackend(defaultNetwork).getProxySettings()

    suspend fun disconnect() {
        logger.d { "Disconnect" }

        _isConnectedState.value = false
        xPubHashId = null
        lightningNodeId = null

        // Stop all jobs
        parentJob.cancelChildren()

        // Recreate subject so that can be sure we have fresh data, especially on shared sessions eg. HWW sessions
        _failedNetworksStateFlow.value = listOf()

        _torStatusSharedFlow.value = TorEvent(progress = 100) // reset TOR status

        // Clear HW derived lightning mnemonic
        _derivedHwLightningMnemonic = null
        _derivedHwBoltzMnemonic = null

        // Clear total balance
        _walletTotalBalanceSharedFlow.value = -1L
        walletTotalBalanceDenominationStateFlow.value = Denomination.BTC

        _tempAllowedServers.clear()

        _walletActiveEventInvalidated = true
        _accountEmptiedEvent = null

        // Clear Lightning
        hasLightning = false
        _lightningAccount = null
        _isLightningLoadingStateFlow.value = false

        lightningManager.release(lightningSdkOrNull)
        lightningSdkOrNull = null

        lwkManager.release(lwkOrNull)
        lwkOrNull = null

        networkBackendsMutex.withLock {
            val backends = networkBackends.values

            networkBackendsStateFlow.value = emptyMap()

            backends.forEach {
                tryCatch {
                    it.disconnect()
                }
            }
        }
    }

    fun disconnectAsync(reason: LogoutReason = LogoutReason.USER_ACTION): Boolean {
        // Disconnect only if needed
        if (isConnected) {
            logoutReason = reason
            _isConnectedState.value = false

            scope.launch(context = logException(countly)) {
                disconnect()

                // Destroy session if it's ephemeral
                ephemeralWallet?.also {
                    sessionManager.destroyEphemeralSession(gdkSession = this@GdkSession)
                }

                // Disconnect last device connection
                device?.also { device ->
                    if (sessionManager.getConnectedHardwareWalletSessions()
                            .none { it.device?.connectionIdentifier == device.connectionIdentifier }
                    ) {
                        device.disconnect()
                    }
                }

                device = null
            }

            return true
        }

        return false
    }

    private suspend fun prepareHttpRequest() {
        logger.i { "Prepare HTTP Request Provider" }
        disconnect()

        networks.bitcoinElectrum.also {
            runBlocking {
                connect(network = it, initNetworks = listOf(it))
            }
        }
    }

    override suspend fun httpRequest(
        method: String,
        urls: List<String>?,
        data: String?,
        accept: String?,
        certs: List<String>?
    ): JsonElement {

        val details = buildJsonObject {
            put("method", method)

            if (urls != null) {

                putJsonArray("urls") {
                    urls.forEach {
                        this.add(it)
                    }
                }
            }
            // Optional (POST) data, 'accept' strings, and additional certificates.
            if (data != null) {
                put("data", data)
            }

            if (accept != null) {
                put("accept", accept)
            }

            if (certs != null) {
                putJsonArray("root_certificates") {
                    certs.forEach {
                        add(it)
                    }
                }
            }
        }

        // Call httpRequest passing the assembled json parameters
        return httpRequest(details)
    }

    override suspend fun httpRequest(details: JsonElement): JsonElement {
        if (!isNetworkInitialized) {
            prepareHttpRequest()
        }

        val urlValidator = requireNotNull(sessionManager.httpRequestUrlValidator) { "httpRequestUrlValidator must be registered before device HTTP requests" }

        val urls = details.jsonObject["urls"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: listOf()

        val isUrlSafe = urls.filter { it.isNotBlank() }.all { url ->
            BlockstreamWhitelistedUrls.any { blockstreamUrl ->
                url.startsWith(blockstreamUrl)
            }
        }

        val servers = urls.map {
            it.server()
        }

        if (!settingsManager.appSettings.tor && urls.filter { it.isNotBlank() }.all { it.contains(".onion") }) {
            if (urlValidator.torWarning()) {
                // reconnect to enable tor
                prepareHttpRequest()
            }
        }

        if (!isUrlSafe && !(settingsManager.isAllowCustomPinServer(urls) || _tempAllowedServers.containsAll(servers))) {
            if (urlValidator.unsafeUrlWarning(urls)) {
                _tempAllowedServers.addAll(servers)
            } else {
                return buildJsonObject {
                    putJsonObject("body") {
                        putJsonObject("error") {
                            put("code", -237)
                            put("message", "id_action_canceled")
                        }
                    }
                }
            }
        }

        return gdkNetworkBackend(defaultNetwork).httpRequest(details).also {
            if (urls.find { it.contains("/set_pin") } != null) {
                countly.jadeInitialize()
            }
        }
    }

    private suspend fun initLightningSdk(lightningMnemonic: String?) {
        if (isHardwareWallet) {
            _derivedHwLightningMnemonic = lightningMnemonic
        }

        val lightningLoginData = getWalletIdentifier(
            network = defaultNetwork,
            loginCredentialsParams = lightningMnemonic?.let { LoginCredentialsParams(mnemonic = it) },
            hwInteraction = null
        )

        lightningSdkOrNull = lightningManager.getLightningBridge(lightningLoginData)

        networkBackendsMutex.withLock {
            networkBackendsStateFlow.update {
                it + (lightning to GlNetworkBackend(
                    sdk = lightningSdk,
                    network = lightning,
                    assetsProvider = this,
                    networkAssetManager = networkAssetManager
                ))
            }
        }
    }

    suspend fun initLightningIfNeeded(mnemonic: String?) {
        if (lightningSdkOrNull == null && supportsLightning()) {
            // Init SDK
            initLightningSdk(mnemonic)
        }

        if (!hasLightning) {

            val connectStatus = connectToGreenlight(
                mnemonicAndCredentials = GreenlightMnemonicAndCredentials(
                    mnemonic = mnemonic ?: deriveLightningMnemonic(), credentials = null
                ), restoreOnly = false
            )

            if (connectStatus == ConnectStatus.Failed) {
                throw Exception("Something went wrong while initiating your Lightning account")
            }

            // Update accounts & balances & transactions for the new network
            updateAccountsAndBalances(updateBalancesForNetwork = lightning)
        }
    }

    suspend fun initLwkIfNeeded(
        wallet: GreenWallet,
        bitcoinAddress: String? = null,
        liquidAddress: String? = null,
        mnemonic: String? = null
    ) {
        if (lwkOrNull == null) {
            val lwk = lwkManager.getLwk(wallet = wallet).also {
                lwkOrNull = it
            }

            lwk.connect(
                xPubHashId = wallet.xPubHashId,
                mnemonic = mnemonic ?: deriveBoltzMnemonic(),
                bitcoinAddress = bitcoinAddress,
                liquidAddress = liquidAddress
            )

            lwk.invoicePaidSharedFlow.onEach {
                _lastInvoicePaid.value = it
            }.launchIn(scope)
        }
    }

    suspend fun <T> initNetworkIfNeeded(
        network: Network,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        action: suspend () -> T
    ): T {
        if (!network.isLightning && !hasActiveNetwork(network)) {

            val loginCredentialsParams = if (isHardwareWallet) {
                LoginCredentialsParams.empty
            } else {
                LoginCredentialsParams.fromCredentials(getCredentials())
            }

            if (useLwkFor(network)) {
                lwkNetworkBackend(network).also {
                    it.connect(createConnectionParams(network))
//                    it.login(loginCredentialsParams = loginCredentialsParams)
                }
                return action()
            }

            val backend = gdkNetworkBackend(network)

            backend.connect(createConnectionParams(network))

            val previousMultisig = activeMultisig.firstOrNull()

            val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

            // Register new Multisig or No-op on Singlesig
            backend.register(
                deviceParams = deviceParams,
                loginCredentialsParams = loginCredentialsParams,
                hardwareWalletResolver = hardwareWalletResolver
            )

            backend.login(
                deviceParams = deviceParams,
                loginCredentialsParams = loginCredentialsParams,
                hardwareWalletResolver = hardwareWalletResolver
            )

            // Sync settings, use multisig if exists to sync PGP also
            (getSettings(network = previousMultisig) ?: getSettings())?.also {
                changeSettings(
                    network,
                    Settings.normalizeFromProminent(
                        networkSettings = getSettings(network) ?: it,
                        prominentSettings = it,
                        pgpFromProminent = true
                    )
                )
            }

            if (network.isMultisig) {
                updateTwoFactorConfig(network = network, useCache = false)
                updateWatchOnlyUsername(network = network)
            }

            // hard refresh accounts for the new network
            val networkAccounts = getAccounts(network, refresh = true)

            // Archive default account if it is unfunded
            val defaultAccount = networkAccounts.first()

            if (!defaultAccount.hasHistory(this)) {
                updateAccount(
                    account = defaultAccount,
                    isHidden = true,
                    resetAccountName = defaultAccount.type.title()
                )
            }

            // Update accounts & balances & transactions for the new network
            updateAccountsAndBalances(updateBalancesForAccounts = networkAccounts)
        }

        return action.invoke()
    }

    fun tryFailedNetworks(hardwareWalletResolver: HardwareWalletResolver? = null) {
        scope.launch(context = logException(countly)) {

            val loginCredentialsParams = if (isHardwareWallet) {
                LoginCredentialsParams.empty
            } else {
                LoginCredentialsParams.fromCredentials(getCredentials())
            }

            val networks = _failedNetworksStateFlow.value
            val failedNetworkLogins = mutableListOf<Network>()

            _failedNetworksStateFlow.value = listOf()

            // Network with failed logins
            networks.forEach { network ->
                try {
                    logger.i { "Login into ${network.id}" }

                    if (network.isLightning) {
                        // Connect SDK
                        connectToGreenlight(
                            mnemonicAndCredentials = GreenlightMnemonicAndCredentials(
                                mnemonic = deriveLightningMnemonic(),
                                credentials = byteArrayOf()
                            ), restoreOnly = false
                        )
                    } else {

                        val backend = gdkNetworkBackend(network)

                        backend.connect(createConnectionParams(network))

                        val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

                        backend.login(
                            deviceParams = deviceParams,
                            loginCredentialsParams = loginCredentialsParams,
                            hardwareWalletResolver = hardwareWalletResolver
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (e.message != "id_login_failed") {
                        failedNetworkLogins.add(network)
                    }
                }
            }

            _failedNetworksStateFlow.value = failedNetworkLogins

            updateAccountsAndBalances()
        }
    }

    suspend fun emergencyRestoreOfRecoveryPhrase(
        wallet: GreenWallet,
        pin: String,
        loginCredentials: LoginCredentials,
    ): Credentials {
        val network = prominentNetwork(wallet, loginCredentials)

        connect(network = network, initNetworks = listOf(network))

        return decryptCredentialsWithPin(
            network = network,
            decryptWithPinParams = DecryptWithPinParams(pin = pin, pinData = loginCredentials.pin_data)
        )
    }

    suspend fun loginWithWallet(
        wallet: GreenWallet,
        pin: String? = null,
        mnemonic: String? = null,
        loginCredentials: LoginCredentials,
        greenlightMnemonicAndCredentials: GreenlightMnemonicAndCredentials?,
        isRestore: Boolean = false,
        initializeSession: Boolean = true,
    ): LoginData {
        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(wallet, loginCredentials),
            wallet = wallet,
            walletLoginCredentialsParams = pin?.let {
                LoginCredentialsParams(
                    pin = it,
                    pinData = loginCredentials.pin_data
                )
            } ?: mnemonic?.let {
                LoginCredentialsParams(
                    mnemonic = it,
                    pinData = loginCredentials.pin_data
                )
            }!!,
            greenlightMnemonicAndCredentials = greenlightMnemonicAndCredentials,
            isRestore = isRestore,
            initializeSession = initializeSession
        )
    }

    suspend fun loginWithMnemonic(
        isTestnet: Boolean,
        wallet: GreenWallet? = null,
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
            walletLoginCredentialsParams = loginCredentialsParams,
            initNetworks = initNetworks,
            isSmartDiscovery = isSmartDiscovery,
            isCreate = isCreate,
            isRestore = isRestore,
            initializeSession = initializeSession
        )
    }

    suspend fun loginWatchOnly(
        wallet: GreenWallet,
        loginCredentials: LoginCredentials? = null,
        watchOnlyCredentials: MultipleWatchOnlyCredentials,
        greenlightMnemonicAndCredentials: GreenlightMnemonicAndCredentials? = null,
        derivedBoltzMnemonic: String? = null,
    ) {
        loginWatchOnly(
            network = prominentNetwork(wallet, loginCredentials),
            wallet = wallet,
            watchOnlyCredentials = watchOnlyCredentials,
            greenlightMnemonicAndCredentials = greenlightMnemonicAndCredentials,
            derivedBoltzMnemonic = derivedBoltzMnemonic
        )
    }

    suspend fun loginWatchOnly(
        network: Network,
        wallet: GreenWallet?,
        watchOnlyCredentials: MultipleWatchOnlyCredentials,
        greenlightMnemonicAndCredentials: GreenlightMnemonicAndCredentials? = null,
        derivedBoltzMnemonic: String? = null,
    ): LoginData {
        return loginWatchOnly(
            network = network,
            wallet = wallet,
            loginCredentialsParams = watchOnlyCredentials.toLoginCredentials(),
            greenlightMnemonicAndCredentials = greenlightMnemonicAndCredentials,
            derivedBoltzMnemonic = derivedBoltzMnemonic
        )
    }

    // RWO Login
    suspend fun loginRichWatchOnly(wallet: GreenWallet, loginCredentials: LoginCredentials, richWatchOnly: List<RichWatchOnly>): LoginData {
        val networks = richWatchOnly.map {
            networkBy(it.network)
        }

        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(wallet = wallet, loginCredentials = loginCredentials),
            initNetworks = networks,
            wallet = wallet,
            walletLoginCredentialsParams = richWatchOnly.first().toLoginCredentialsParams(),
            richWatchOnly = richWatchOnly
        )
    }

    // WO Login
    private suspend fun loginWatchOnly(
        network: Network, wallet: GreenWallet?,
        loginCredentialsParams: LoginCredentialsParams,
        greenlightMnemonicAndCredentials: GreenlightMnemonicAndCredentials? = null,
        derivedBoltzMnemonic: String? = null
    ): LoginData {
        val initNetworks = loginCredentialsParams.multipleWatchOnlyCredentials?.credentials?.keys?.map {
            networkBy(it)
        } ?: listOf(network)

        return loginWithLoginCredentials(
            prominentNetwork = network,
            initNetworks = initNetworks,
            wallet = wallet,
            walletLoginCredentialsParams = loginCredentialsParams,
            greenlightMnemonicAndCredentials = greenlightMnemonicAndCredentials,
            derivedBoltzMnemonic = derivedBoltzMnemonic
        )
    }

    suspend fun loginWithDevice(
        wallet: GreenWallet,
        device: GreenDevice,
        greenlightMnemonicAndCredentials: GreenlightMnemonicAndCredentials?,
        derivedBoltzMnemonic: String?,
        hardwareWalletResolver: HardwareWalletResolver,
        isSmartDiscovery: Boolean,
        hwInteraction: HardwareWalletInteraction? = null,
    ): LoginData {
        // If last used network is Lightning, change to bitcoin as the ln network can't be used for login
        val lastUsedNetwork = (wallet.activeNetwork
            .takeIf { !Network.isLightning(it) } ?: Network.ElectrumMainnet)
            .let {
                networks.getNetworkById(it)
            }

        val supportedNetworks = networks(isTestnet = wallet.isTestnet, isWatchOnly = false, device = device)

        val initNetworks = if (device.deviceBrand.isTrezor) {
            supportedNetworks.filter { it.isBitcoin }
        } else if (device.deviceBrand.isLedger) {
            // Ledger can operate only into a single network but both policies are supported
            supportedNetworks.filter { it.isBitcoin == lastUsedNetwork.isBitcoin && !(it.isSinglesig && it.isLiquid) }
        } else {
            supportedNetworks
        }

        return loginWithLoginCredentials(
            prominentNetwork = initNetworks.first(),
            initNetworks = initNetworks,
            wallet = wallet,
            walletLoginCredentialsParams = LoginCredentialsParams.empty,
            greenlightMnemonicAndCredentials = greenlightMnemonicAndCredentials,
            derivedBoltzMnemonic = derivedBoltzMnemonic,
            device = device,
            isSmartDiscovery = isSmartDiscovery,
            hardwareWalletResolver = hardwareWalletResolver,
            hwInteraction = hwInteraction,
        )
    }

    private suspend fun loginWithLoginCredentials(
        prominentNetwork: Network,
        initNetworks: List<Network>? = null,
        wallet: GreenWallet? = null,
        walletLoginCredentialsParams: LoginCredentialsParams,
        richWatchOnly: List<RichWatchOnly>? = null,
        greenlightMnemonicAndCredentials: GreenlightMnemonicAndCredentials? = null,
        derivedBoltzMnemonic: String? = null,
        device: GreenDevice? = null,
        isCreate: Boolean = false,
        isRestore: Boolean = false,
        isSmartDiscovery: Boolean = false,
        initializeSession: Boolean = true,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        hwInteraction: HardwareWalletInteraction? = null,
    ): LoginData {

        // TODO move all to StateFlow
        // Warning, ordering matters for SecurityScreen
        isHwWatchOnly = walletLoginCredentialsParams.multipleWatchOnlyCredentials?.isHwWatchOnly() == true
        _isWatchOnly.value = walletLoginCredentialsParams.isWatchOnly

        isNoBlobWatchOnly = isWatchOnlyValue && richWatchOnly == null
        isRichWatchOnly = isWatchOnlyValue && richWatchOnly != null
        isCoreDescriptorWatchOnly =
            isWatchOnlyValue && (walletLoginCredentialsParams.coreDescriptors != null || walletLoginCredentialsParams.multipleWatchOnlyCredentials?.isCoreDescriptors() == true)
        isAirgapped = isWatchOnlyValue && wallet?.isHardware ?: false

        setupDeviceToSession(device)

        this.deviceModel =
            device?.deviceModel ?: wallet?.deviceIdentifiers?.firstOrNull()?.model ?: wallet?.deviceIdentifiers?.firstOrNull()?.brand?.let {
                when (it) {
                    DeviceBrand.Blockstream -> DeviceModel.BlockstreamGeneric
                    DeviceBrand.Ledger -> DeviceModel.LedgerGeneric
                    DeviceBrand.Trezor -> DeviceModel.TrezorGeneric
                    DeviceBrand.Generic -> DeviceModel.Generic
                }
            }

        _disableNotificationHandling = true
        _walletActiveEventInvalidated = true

        logger.d { "loginWithLoginCredentials prominentNetwork: ${prominentNetwork.id} initNetworks: ${initNetworks?.joinToString(",") { it.id }} " }

        val connectedNetworks = connect(
            network = prominentNetwork,
            initNetworks = initNetworks,
        )

        val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

        // Get enabled singlesig networks (multisig can be identified by login_user)
        val sortedBackends = networkBackends.values.filter {
            !it.network.isLightning
        }.sortedWith { a, b ->
            when {
                a.network == prominentNetwork -> -1
                b.network == prominentNetwork -> 1
                else -> a.network.id.compareTo(b.network.id)
            }
        }

        // If it's a pin login, check if the prominent network is connected
        if (walletLoginCredentialsParams.pin.isNotBlank() && !connectedNetworks.contains(prominentNetwork)) {
            throw Exception("id_connection_failed")
        }

        @Suppress("NAME_SHADOWING")
        // If it's a pin login, get the credentials from the prominent network
        val loginCredentialsParams = if (walletLoginCredentialsParams.pin.isNullOrBlank()) {
            walletLoginCredentialsParams
        } else {
            decryptCredentialsWithPin(
                network = prominentNetwork,
                decryptWithPinParams = DecryptWithPinParams.fromLoginCredentials(walletLoginCredentialsParams)
            ).let {
                LoginCredentialsParams.fromCredentials(it)
            }
        }

        val failedNetworkLogins = mutableListOf<Network>()

        val exceptions = mutableListOf<Exception>()

        hasLightning = greenlightMnemonicAndCredentials != null

        return (sortedBackends.map { backend ->
            scope.async(start = CoroutineStart.LAZY) {
                val network = backend.network

                val networkLoginCredentialsParams =
                    richWatchOnly?.find { it.network == network.id }?.toLoginCredentialsParams()
                        ?: loginCredentialsParams.hwWatchOnlyCredentialsToLoginCredentialsParams(network.id)
                        ?: loginCredentialsParams

                try {
                    // On Create just login into Singlesig network
                    if (isCreate && network.isMultisig) {
                        logger.i { "Skip login in ${network.id}" }
                        return@async null
                    }

                    logger.i { "Login into ${network.id}" }

                    if (useLwkFor(network)) {
                        return@async lwkNetworkBackend(network).login(
                            loginCredentialsParams = networkLoginCredentialsParams
                        )
                    }

                    val backend = gdkNetworkBackend(network)

                    backend.login(
                        deviceParams = deviceParams,
                        loginCredentialsParams = networkLoginCredentialsParams,
                        hardwareWalletResolver = hardwareWalletResolver
                    ).also {
                        // Do a refresh
                        if (network.isElectrum && initializeSession && (isRestore || isSmartDiscovery)) {

                            val hasGdkCache = if (isHardwareWallet || networkLoginCredentialsParams.mnemonic.isNotBlank()) {
                                try {
                                    gdk.hasGdkCache(
                                        getWalletIdentifier(
                                            network = network,
                                            loginCredentialsParams = networkLoginCredentialsParams,
                                            hwInteraction = hwInteraction
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    false
                                }
                            } else {
                                false
                            }

                            if (isRestore || !hasGdkCache || isHardwareWallet) {
                                logger.i { "BIP44 Discovery for ${network.id}" }

                                val networkAccounts = getAccounts(network = network, refresh = true)
                                val walletIsFunded = networkAccounts.find { account -> account.bip44Discovered == true } != null

                                if (walletIsFunded) {
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
                                } else if (!hasGdkCache) { // Newly discovered Wallet

                                    // Archive GDK default account
                                    networkAccounts.first().also { defaultAccount ->
                                        updateAccount(
                                            account = defaultAccount,
                                            isHidden = true,
                                            resetAccountName = defaultAccount.type.title()
                                        )
                                    }

                                    // Create Singlesig account
                                    val accountType = AccountType.BIP84_SEGWIT

                                    createAccount(
                                        network = network,
                                        params = SubAccountParams(
                                            name = accountType.toString(),
                                            type = accountType,
                                        )
                                    )
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                    if (e.message != "id_login_failed") {
                        // Mark network as not being able to login
                        failedNetworkLogins.add(backend.network)
                    }

                    // Add all exceptions
                    exceptions.add(e)

                    null
                }
            }
        } + listOfNotNull(
            tryCatch {
                if (isTestnet) return@tryCatch null

                if (liquid == null) return@tryCatch null

                if (isHardwareWallet && derivedBoltzMnemonic == null) return@tryCatch null

                scope.async(
                    start = CoroutineStart.LAZY
                ) {
                    tryCatch {
                        wallet?.also {
                            val boltzMnemonic =
                                derivedBoltzMnemonic
                                    ?: deriveBoltzMnemonic(credentials = Credentials.fromLoginCredentialsParam(loginCredentialsParams))

                            initLwkIfNeeded(wallet = wallet, mnemonic = boltzMnemonic)
                        }
                    }
                    null
                }
            },
            if (!supportsLightning() && !(isHwWatchOnly && greenlightMnemonicAndCredentials != null)) null else scope.async(
                start = CoroutineStart.LAZY
            ) {

                if (isHardwareWallet && greenlightMnemonicAndCredentials == null) {
                    return@async null
                }

                val mnemonicAndCredentials = greenlightMnemonicAndCredentials ?: GreenlightMnemonicAndCredentials(
                    mnemonic = deriveLightningMnemonic(Credentials.fromLoginCredentialsParam(loginCredentialsParams)),
                    credentials = null
                )

                // Init SDK
                initLightningSdk(mnemonicAndCredentials.mnemonic)

                // SmartDiscovery only for SW wallets, on HW ln mnemonic is not available
                if (hasLightning || ((isRestore || (isSmartDiscovery && !isHardwareWallet)) && settingsManager.isLightningAvailable())) {
                    _isLightningLoadingStateFlow.value = true

                    // Make it async to speed up login process
                    val job = scope.async {
                        try {
                            val xPubHashId = wallet?.xPubHashId
                                ?.takeIf { isHwWatchOnly }
                                ?: getWalletIdentifier(
                                    network = prominentNetwork,
                                    loginCredentialsParams = loginCredentialsParams,
                                    hwInteraction = hwInteraction
                                ).xpubHashId

                            // Connect SDK
                            connectToGreenlight(
                                mnemonicAndCredentials = mnemonicAndCredentials,
                                parentXpubHashId = xPubHashId,
                                restoreOnly = isRestore || isSmartDiscovery,
                                quickResponse = isRestore
                            )

                            updateAccountsAndBalances(updateBalancesForNetwork = lightning)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (!isRestore && !isSmartDiscovery) {
                                _failedNetworksStateFlow.value += listOfNotNull(lightning)
                            }
                        } finally {
                            _isLightningLoadingStateFlow.value = false
                        }
                    }

                    // If restore, await for the login to be completed to be able to store credentials
                    if (isRestore) {
                        job.await()
                    }
                }

                null
            })).let { list ->
            list.awaitAll()
                .filterNotNull()
                .let {
                    it.firstOrNull() ?: throw exceptions.first() // Throw if all networks failed
                }.also {
                    _failedNetworksStateFlow.value += failedNetworkLogins
                    onLoginSuccess(
                        loginData = it,
                        wallet = wallet,
                        initializeSession = initializeSession
                    )
                }
        }
    }

    suspend fun watchOnlyToFullSession(device: GreenDevice, gdkSession: GdkSession) {
        var networkBackendsToClose: List<NetworkBackend>?

        networkBackendsMutex.withLock {
            networkBackendsToClose = networkBackends.values.toList()
            networkBackendsStateFlow.value = gdkSession.networkBackends
        }

        networkBackendsToClose?.forEach {
            tryCatch {
                it.disconnect()
            }
        }

        isHwWatchOnly = false
        isNoBlobWatchOnly = false
        isRichWatchOnly = false
        isCoreDescriptorWatchOnly = false
        isAirgapped = false
        _isWatchOnly.value = false

        setupDeviceToSession(device)

        updateAccountsAndBalances()
    }

    private fun setupDeviceToSession(device: GreenDevice?) {
        this.device = device
        this.deviceModel = device?.deviceModel

        device?.deviceState?.onEach {
            // Device went offline
            if (it == DeviceState.DISCONNECTED) {
                disconnectAsync(reason = LogoutReason.DEVICE_DISCONNECTED)
            }
        }?.launchIn(scope)
    }

    private suspend fun connectToGreenlight(
        mnemonicAndCredentials: GreenlightMnemonicAndCredentials,
        parentXpubHashId: String? = null,
        restoreOnly: Boolean = true,
        quickResponse: Boolean = false
    ): ConnectStatus {
        logger.i { "Login into ${lightning.id}" }

        countly.loginLightningStart()

        val backend = glNetworkBackend()

        val connectStatus = backend.login(
            mnemonicAndCredentials = mnemonicAndCredentials,
            parentXpubHashId = parentXpubHashId ?: xPubHashId,
            isRestore = restoreOnly,
            quickResponse = quickResponse,
        )

        countly.loginLightningStop()

        when(connectStatus) {
            ConnectStatus.Connect -> {
               hasLightning = true

                backend.eventSharedFlow.onEach {
                    onLightningEvent(it)
                }.launchIn(scope = scope + parentJob)
            }
            ConnectStatus.Failed -> {
                if(!restoreOnly){
                    _failedNetworksStateFlow.value += listOfNotNull(lightning)
                }
            }
        }

        return connectStatus
    }

    private suspend fun onLoginSuccess(
        loginData: LoginData,
        wallet: GreenWallet?,
        initializeSession: Boolean
    ) {
        _isConnectedState.value = true

        // Watchonly wallet login can't produce the mnemonics xpub.
        // So in case of a HW WO use the one stored in the Wallet to allow HW to be connected with the same xpub
        xPubHashId = when {
            isHwWatchOnly -> wallet?.xPubHashId
            isNoBlobWatchOnly && !isHwWatchOnly -> loginData.networkHashId
            else -> null
        } ?: loginData.xpubHashId

        lightningNodeId = wallet?.id?.let { walletSettingsManager.getLightningNodeId(walletId = wallet.id) }

        if (initializeSession) {
            countly.activeWalletStart()
            initializeSessionData(wallet)
        }

        // Allow initialization calls to have priority over notifications initiated updates (getWalletTransactions & updateAccountAndBalances)
        _disableNotificationHandling = false
    }

    private suspend fun initializeSessionData(wallet: GreenWallet?) {
        // Update Liquid Assets from GDK before getting balances to sort them properly
        updateLiquidAssets()

        // Update the enriched assets
        updateEnrichedAssets()

        // Change wallet balance denomination
        walletTotalBalanceDenominationStateFlow.value =
            if (wallet?.id?.let { walletSettingsManager.isTotalBalanceInFiat(walletId = it) } == true) Denomination.defaultOrFiat(
                session = this,
                isFiat = true
            ) else Denomination.BTC

        if (!isWatchOnlyValue) {
            // Sync settings from prominent network to the rest
            syncSettings()

            // Continue login even if for some reason 2FA fails
            try {
                // Cache 2fa config
                activeBitcoinMultisig?.also {
                    updateTwoFactorConfig(network = it, useCache = false)
                    updateWatchOnlyUsername(network = it)
                }

                // Cache 2fa config
                activeLiquidMultisig?.also {
                    updateTwoFactorConfig(network = it, useCache = false)
                    updateWatchOnlyUsername(network = it)
                }
            } catch (e: Exception) {
                countly.recordException(e)
            }
        }

        // RWO: update accounts, if this is not done here, newly created rwo wallet won't scan for accounts
        updateAccountsAndBalances(
            isInitialize = true,
            refresh = isRichWatchOnly,
        )

        updateSystemMessage()

        // The block notification that also triggers this arrives before the 2FA config is cached.
        scanExpired2FA()
    }

    private suspend fun scanExpired2FA() {
        loggedInGdkNetworkBackends.filter { it.network.is2faNetwork }.forEach { backend ->
            tryCatch { backend.scanExpired2FA() }
        }
    }

    fun updateLiquidAssets() {
        if (liquid != null) {
            networkAssetManager.updateAssetsIfNeeded(this)
        }
    }

    fun updateSystemMessage() {
        scope.launch(logException(countly)) {
            // Side effect: each backend.getSystemMessage() updates its systemMessage flow.
            // The session's systemMessage derives reactively from those flows.
            activeGdkNetworks.forEach { network ->
                gdkNetworkBackend(network).getSystemMessage()
            }
        }
    }

    suspend fun ackSystemMessage(network: Network, message: String) = gdkNetworkBackend(network).ackSystemMessage(message)

    suspend fun setTransactionMemo(transaction: Transaction, memo: String) {
        gdkNetworkBackend(transaction.account.network).setTransactionMemo(transaction.txHash, memo)
    }

    suspend fun getWalletIdentifier(
        network: Network,
        loginCredentialsParams: LoginCredentialsParams? = null,
        gdkHwWallet: GdkHardwareWallet? = null,
        hwInteraction: HardwareWalletInteraction? = null,
    ) = gdk.getWalletIdentifier(
        connectionParams = createConnectionParams(network),
        loginCredentialsParams = loginCredentialsParams?.takeIf { !it.mnemonic.isNullOrBlank() }
            ?: getDeviceMasterXpub(
                network = network,
                gdkHwWallet = gdkHwWallet,
                hwInteraction = hwInteraction
            )?.let { LoginCredentialsParams(masterXpub = it) }
            ?: loginCredentialsParams
            ?: getCredentials().let { LoginCredentialsParams.fromCredentials(it) }
    )

    suspend fun getDeviceMasterXpub(
        network: Network,
        gdkHwWallet: GdkHardwareWallet? = null,
        hwInteraction: HardwareWalletInteraction? = null
    ): String? {
        return (gdkHwWallet ?: this.gdkHwWallet)?.getXpubs(network, listOf(listOf()), hwInteraction)
            ?.firstOrNull()
    }

    fun getWalletFingerprint(
        network: Network,
        gdkHwWallet: GdkHardwareWallet? = null,
        hwInteraction: HardwareWalletInteraction? = null,
    ): String? {
        return (gdkHwWallet ?: this.gdkHwWallet)?.let {
            wally.bip32Fingerprint(
                it.getXpubs(network, listOf(listOf()), hwInteraction).first()
            )
        }
    }

    suspend fun encryptWithPin(network: Network?, encryptWithPinParams: EncryptWithPinParams): EncryptWithPin {
        return gdkNetworkBackend(network ?: defaultNetwork).encryptWithPin(encryptWithPinParams)
    }

    private suspend fun decryptCredentialsWithPin(network: Network, decryptWithPinParams: DecryptWithPinParams): Credentials =
        gdkNetworkBackend(network).decryptCredentialsWithPin(decryptWithPinParams)

    suspend fun getCredentials(params: CredentialsParams = CredentialsParams()): Credentials {
        val network = defaultNetwork.takeIf { hasActiveNetwork(defaultNetwork) } ?: activeGdkNetworks.first()
        return gdkNetworkBackend(network).getCredentials(params)
    }

    private var _derivedHwLightningMnemonic: String? = null
    suspend fun deriveLightningMnemonic(credentials: Credentials? = null): String {
        if (isHardwareWallet && credentials == null) {
            return _derivedHwLightningMnemonic ?: throw Exception("HWW can't derive lightning mnemonic")
        }

        return (credentials ?: getCredentials()).let {
            if (isHardwareWallet) {
                it.mnemonic!! // Already derived
            } else {
                wally.bip85FromMnemonic(
                    mnemonic = it.mnemonic!!,
                    passphrase = it.bip39Passphrase,
                    index = 0,
                    isTestnet = isTestnet
                ) ?: throw Exception("Couldn't derive lightning mnemonic")
            }
        }
    }

    private var _derivedHwBoltzMnemonic: String? = null
    suspend fun deriveBoltzMnemonic(credentials: Credentials? = null): String {
        if (isHardwareWallet && credentials == null) {
            return _derivedHwBoltzMnemonic ?: throw Exception("HWW can't derive lightning mnemonic")
        }

        return (credentials ?: getCredentials()).let {
            if (isHardwareWallet) {
                it.mnemonic!! // Already derived
            } else {
                wally.bip85FromMnemonic(
                    mnemonic = it.mnemonic!!,
                    passphrase = it.bip39Passphrase,
                    index = Lwk.BOLTZ_BIP85_INDEX,
                    isTestnet = isTestnet
                ) ?: throw Exception("Couldn't derive Boltz mnemonic")
            }
        }
    }

    private val isLwkEnabled: Boolean = false

    // no support for hardware wallet
    private fun useLwkFor(network: Network): Boolean = network.isAmp2 ||
        isLwkEnabled && network.isLiquid && network.isSinglesig && !isHardwareWallet

    fun networkBackend(network: Network): NetworkBackend =
        networkBackends[network] ?: throw Exception("${network.id} not initialized")

    fun networkBackendOrNull(network: Network): NetworkBackend? = networkBackends[network]

    fun accountBackendOrNull(account: Account): AccountBackend? =
        networkBackendOrNull(account.network)?.accountBackend(account)

    private inline fun <reified T : NetworkBackend> backendOrNull(network: Network): T? =
        networkBackends[network] as? T

    private inline fun <reified T : NetworkBackend> backend(network: Network): T {
        return networkBackends[network].let {
            it as? T ?: throw Exception("Expected ${T::class.simpleName} for ${network.id}")
        }
    }

    private fun gdkNetworkBackendOrNull(network: Network): GdkNetworkBackend? = backendOrNull(network)

    private fun gdkNetworkBackend(network: Network): GdkNetworkBackend = backend(network)

    private fun glNetworkBackend(): GlNetworkBackend = backend(lightning)

    private fun lwkNetworkBackend(network: Network): LwkNetworkBackend = backend(network)

    fun accountBackend(account: Account): AccountBackend = networkBackend(account.network).accountBackend(account)

    private fun gdkAccountBackend(account: Account): GdkAccountBackend = networkBackend(account.network).accountBackend(account) as? GdkAccountBackend ?: throw Exception("Expected GdkAccountBackend for ${account.id}}")

    suspend fun createLightningInvoice(satoshi: Long, description: String): LightningReceivePayment {
        return (accountBackend(lightningAccount) as GlAccountBackend).createInvoice(satoshi, description)
    }

    suspend fun getPreviousAddresses(account: Account, lastPointer: Int?): PreviousAddresses =
        gdkAccountBackend(account).getPreviousAddresses(lastPointer)

    override suspend fun refreshAssets(params: AssetsParams) {
        (activeLiquid ?: liquid)?.also { (networkBackend(it) as AssetsProvider).refreshAssets(params) }
    }

    override suspend fun getAssets(params: GetAssetsParams) =
        (activeLiquid ?: liquid)?.let { (networkBackend(it) as AssetsProvider).getAssets(params) }

    fun setupDefaultAccounts(archiveDefaultAccounts: Boolean = true): Job {
        return scope.launch {
            // Create Singlesig Segwit accounts
            val accountType = AccountType.BIP84_SEGWIT

            // Create Bitcoin & Liquid accounts if do not exists
            listOfNotNull(bitcoinSinglesig, liquidSinglesig).forEach { network ->
                if (accounts.value.find { it.type == accountType && it.network.id == network.id } == null) {
                    logger.d { "Creating ${network.name} account" }
                    createAccount(
                        network = network,
                        params = SubAccountParams(
                            name = accountType.toString(),
                            type = accountType,
                        )
                    )
                }
            }

            // Be sure to update all accounts so that we properly calculate balances
            updateAccountsAndBalances().join()

            if (archiveDefaultAccounts) {
                // Archive default gdk legacy accounts with no history
                accounts.value.filter {
                    (it.type == AccountType.BIP44_LEGACY || it.type == AccountType.BIP49_SEGWIT_WRAPPED) && !it.hasHistory(
                        this@GdkSession
                    )
                }.forEach { account ->
                    logger.d { "Archive ${account.name}" }
                    updateAccount(
                        account = account, isHidden = true, resetAccountName = account.type.title()
                    )
                }
            }
        }
    }

    suspend fun createAccount(
        network: Network,
        params: SubAccountParams,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): Account {
        return initNetworkIfNeeded(network, hardwareWalletResolver) {
            networkBackend(network).createAccount(params = params, hardwareWalletResolver = hardwareWalletResolver)
        }.also {
            _walletActiveEventInvalidated = true

            listOf(it).also {
                // Update newly created account
                updateAccountsAndBalances(updateBalancesForAccounts = it)
            }
        }
    }

    suspend fun getAccounts(network: Network? = null, refresh: Boolean = false): List<Account> {
        return (network?.let { mapOf(it to networkBackend(network)) } ?: networkBackends).mapLoggedIn { backend ->
            backend.getAccounts(refresh = refresh)
        }.flatten().sorted()
    }

    suspend fun getAccount(account: Account) = networkBackend(account.network).getAccount(account)

    suspend fun getAccountDescriptors(account: Account): String? = accountBackend(account).getOutputDescriptors()

    suspend fun removeAccount(account: Account) {

        if (account.isLightning) {
            hasLightning = false

            networkBackendsMutex.withLock {
                networkBackendsStateFlow.update { backends ->
                    backends.toMutableMap().also {
                        it.remove(lightning)
                    }
                }
            }

            updateAccountsAndBalances()
        }
    }

    suspend fun updateAccount(
        account: Account,
        isHidden: Boolean,
        userInitiated: Boolean = false,
        resetAccountName: String? = null
    ): Account {
        accountBackend(account).updateAccount(
            hidden = isHidden,
            name = resetAccountName?.takeIf { it.isNotBlank() }
        )

        if (userInitiated && isHidden) {
            _eventsSharedFlow.emit(WalletEvents.ARCHIVED_ACCOUNT)
        }

        return getAccount(account).also {
            listOf(it).also {
                // Update newly created account
                updateAccountsAndBalances(updateBalancesForAccounts = it)
            }
        }
    }

    suspend fun updateAccount(account: Account, name: String): Account {
        accountBackend(account).updateAccount(name = name)

        return getAccount(account)
    }

    suspend fun getLightningFeeEstimation(): LightningFees? {
        // return lightningSdk.onchainFeeRates()
        val fees = getFeeEstimates(lightning).fees
        fun rate(index: Int): ULong = ((fees.getOrNull(index) ?: 0L) / 1000).toULong()
        return LightningFees(
            fastestFee = rate(FeeBlockHigh),
            halfHourFee = rate(3),
            hourFee = rate(FeeBlockMedium),
            economyFee = rate(FeeBlockLow),
            minimumFee = rate(0),
        )
    }

    suspend fun getFeeEstimates(network: Network): FeeEstimation = tryCatch(context = Dispatchers.Default) {
        val feeNetwork = if (network.isLightning) {
            bitcoin ?: network
        } else network

        networkBackend(feeNetwork).getFeeEstimates()
    } ?: FeeEstimation(fees = mutableListOf(network.defaultFee))

    private val refreshMutex = Mutex()
    fun refresh() {
        scope.launch(context = logException(countly)) {
            refreshMutex.withLock {
                updateAccountsAndBalances(refresh = true)

                updateLiquidAssets()

                scanExpired2FA()
            }
        }
    }

    private val accountsAndBalancesMutex = Mutex()
    fun updateAccountsAndBalances(
        isInitialize: Boolean = false,
        refresh: Boolean = false,
        updateBalancesForNetwork: Network? = null,
        updateBalancesForAccounts: Collection<Account>? = null
    ): Job {

        return scope.launch(context = logException(countly)) {

            try {
                accountsAndBalancesMutex.withLock {

                    // Update accounts
                    updateAccounts(refresh = refresh, autoUnarchiveAccounts = true)

                    for (account in this@GdkSession.allAccounts.value) {
                        if ((updateBalancesForAccounts == null && updateBalancesForNetwork == null) || updateBalancesForAccounts?.find { account.id == it.id } != null || account.network == updateBalancesForNetwork) {
                            getBalance(account = account, cacheAssets = isInitialize)
                        }
                    }

                    _accountsAndBalanceUpdatedSharedFlow.emit(Unit)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                countly.recordException(e)
            } finally {
                accountEmptiedEventIfNeeded()
                walletActiveEventIfNeeded()
            }
        }
    }

    suspend fun getBalance(account: Account, confirmations: Int = 0, cacheAssets: Boolean = false): Assets {
        val assets = accountBackend(account).getBalance(confirmations = confirmations)

        if (cacheAssets) {
            // Cache assets before sorting them, as the sort function uses the asset metadata
            cacheAssets(assetIds = assets.keys)
        }
        return Assets(assets)
    }

    suspend fun changeSettingsTwoFactor(
        network: Network,
        method: String,
        methodConfig: TwoFactorMethodConfig,
        twoFactorResolver: TwoFactorResolver
    ) {
        gdkNetworkBackend(network).changeSettingsTwoFactor(method, methodConfig, twoFactorResolver)
        updateTwoFactorConfig(network)
    }

    suspend fun setWatchOnly(
        network: Network,
        username: String,
        password: String
    ): LoginData {
        logger.d { "setWatchOnly: ${network.id} user: '$username' pass: '$password'" }

        return gdkNetworkBackend(network).createWatchOnly(username, password).also {
            updateWatchOnlyUsername(network)
        }
    }

    private suspend fun createWatchOnly(
        networks: List<Network>,
        username: String,
        password: String
    ): List<RichWatchOnly> {
        return networks.filter { !it.isLightning }.mapNotNull { network ->
            try {
                RichWatchOnly(
                    network = network.id,
                    username = username,
                    password = password,
                    watchOnlyData = setWatchOnly(network, username, password).watchOnlyData
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun updateRichWatchOnly(rwo: List<RichWatchOnly>): List<RichWatchOnly> {
        return rwo + (activeGdkNetworks.filter { network ->
            !network.isLightning && rwo.find { it.network == network.id } == null
        }.let {
            createWatchOnly(it, randomChars(16), randomChars(32))
        })
    }

    suspend fun deleteWatchOnly(network: Network) {
        setWatchOnly(network = network, username = "", password = "")
    }

    fun watchOnlyUsername(network: Network): StateFlow<String?> =
        gdkNetworkBackend(network).watchOnlyUsername

    private suspend fun updateWatchOnlyUsername(network: Network? = null) {
        if (isWatchOnlyValue) return

        (network?.let { listOfNotNull(network) } ?: activeMultisig).filter {
            walletExistsAndIsUnlocked(it)
        }.onEach {
            try {
                // Side effect: backend.getWatchOnlyUsername() updates its watchOnlyUsername flow.
                gdkNetworkBackend(it).getWatchOnlyUsername()
            } catch (e: Exception) {
                countly.recordException(e)
            }
        }
    }

    suspend fun twoFactorReset(
        network: Network,
        email: String,
        isDispute: Boolean,
        twoFactorResolver: TwoFactorResolver
    ): TwoFactorReset {
        logger.d { "TwoFactorReset ${network.id} email:$email isDispute:$isDispute" }
        return gdkNetworkBackend(network).twoFactorReset(email, isDispute, twoFactorResolver).also {
            // Should we disconnect
            // disconnectAsync(LogoutReason.USER_ACTION)
            updateSettings(network)
        }
    }

    suspend fun twoFactorUndoReset(network: Network, email: String, twoFactorResolver: TwoFactorResolver) {
        logger.d { "TwoFactorUndoReset ${network.id} email:$email" }
        gdkNetworkBackend(network).twoFactorUndoReset(email, twoFactorResolver)
        // Should we disconnect
        // disconnectAsync(LogoutReason.USER_ACTION)
        updateSettings(network)
    }

    suspend fun twoFactorCancelReset(network: Network, twoFactorResolver: TwoFactorResolver) {
        logger.d { "TwoFactorCancelReset ${network.id}" }
        gdkNetworkBackend(network).twoFactorCancelReset(twoFactorResolver)
        // Should we disconnect
        // disconnectAsync(LogoutReason.USER_ACTION)
        updateSettings(network)
    }

    suspend fun twoFactorChangeLimits(network: Network, limits: Limits, twoFactorResolver: TwoFactorResolver): Limits {
        return gdkNetworkBackend(network).twoFactorChangeLimits(limits, twoFactorResolver).also {
            updateTwoFactorConfig(network)
        }
    }

    suspend fun sendNlocktimes(network: Network) = gdkNetworkBackend(network).sendNlocktimes()

    suspend fun setCsvTime(network: Network, value: CsvParams, twoFactorResolver: TwoFactorResolver) {
        gdkNetworkBackend(network).setCsvTime(value = value, twoFactorResolver = twoFactorResolver).also {
            updateSettings(network)
        }
    }

    private suspend fun updateAccounts(refresh: Boolean = false, autoUnarchiveAccounts: Boolean = false): List<Account> {
        val fetchedAccounts = getAccounts(refresh = refresh)

        if (autoUnarchiveAccounts) {
            var accountsChanged = false
            // Unarchive accounts if they are hidden and funded
            fetchedAccounts.filter { it.hidden && it.isFunded(this) }.forEach {
                accountsChanged = true
                updateAccount(it, isHidden = false)
            }

            // Refetch as some accounts could be unhidden
            val visibleAccounts = getAccounts().filter { !it.hidden }

            if (visibleAccounts.none { it.isBitcoin }) {
                fetchedAccounts.firstOrNull { it.isBitcoin }?.also {
                    accountsChanged = true
                    updateAccount(it, isHidden = false)
                }
            }

            if (visibleAccounts.none { it.isLiquid }) {
                fetchedAccounts.firstOrNull { it.isLiquid }?.also {
                    accountsChanged = true
                    updateAccount(it, isHidden = false)
                }
            }

            if (accountsChanged) {
                return updateAccounts()
            }
        }

        return fetchedAccounts
    }

    suspend fun isPolicyAsset(assetId: String?): Boolean {
        return networkBackends.values.any {
            it.isPolicyAsset(assetId)
        }
    }

    // asset_info in Convert object can be null for liquid assets that don't have asset metadata
    // if no asset is given, no conversion is needed (conversion will be identified as a btc value in gdk)
    // onlyInAcceptableRange return MIN, MAX values so that the error pop in different gdk call
    suspend fun convert(
        assetId: String? = null,
        asString: String? = null,
        asLong: Long? = null,
        denomination: String? = null,
        onlyInAcceptableRange: Boolean = true
    ): Balance? = withContext(context = Dispatchers.Default) {

        val network =
            assetId.networkForAsset(this@GdkSession)?.takeIf { !it.isLightning } ?: defaultNetworkOrNull ?: return@withContext null
        val isPolicyAsset = assetId.isPolicyAsset(this@GdkSession)
        val asset = assetId?.let { getAsset(it) }

        val isFiatDenomination =
            denomination != null && denomination != BTC_UNIT && denomination != MBTC_UNIT && denomination != UBTC_UNIT && denomination != BITS_UNIT && denomination != SATOSHI_UNIT

        val convert = if (isPolicyAsset || (asString == null && asset != null)) {
            Convert.create(
                isPolicyAsset = isPolicyAsset,
                asset = asset,
                asString = asString,
                asLong = asLong,
                unit = denomination ?: BTC_UNIT
            ).toJsonElement()
        } else if (asset != null) {
            if (isFiatDenomination) {
                buildJsonObject {
                    put("asset_info", asset.toJsonElement())
                    put("fiat", asString)
                }
            } else {
                buildJsonObject {
                    put("asset_info", asset.toJsonElement())
                    put(assetId, asString)
                }
            }
        } else {
            return@withContext Balance.fromAssetWithoutMetadata(asLong ?: asString?.toLongOrNull() ?: 0)
        }

        logger.d { "GDK_CONVERT call: assetId=$assetId, network=${network.id}, isPolicyAsset=$isPolicyAsset, params=$convert" }

        val balance = try {
            val result = (networkBackend(network) as AmountConverter).convertAmount(convert)
            logger.d { "GDK_CONVERT result: assetId=$assetId, network=${network.id}, response=${result.toString().take(300)}" }
            Balance.fromJsonElement(
                jsonElement = result,
                assetId = assetId
            )
        } catch (e: Exception) {
            logger.d { "GDK_CONVERT error: assetId=$assetId, network=${network.id}, error=${e.message}" }
            e.printStackTrace()
            if (!onlyInAcceptableRange) {
                when (e.message) {
                    "id_amount_above_maximum_allowed" -> {
                        Balance(satoshi = Long.MAX_VALUE)
                    }

                    "id_amount_below_minimum_allowed" -> {
                        Balance(satoshi = -Long.MAX_VALUE)
                    }

                    else -> {
                        null
                    }
                }
            } else {
                null
            }
        }

        balance?.asset = asset

        return@withContext balance
    }

    suspend fun getUnspentOutputs(
        account: Account,
        isBump: Boolean = false,
        isExpired: Boolean = false
    ): UnspentOutputs = accountBackend(account).getUnspentOutputs(
        isBump = isBump,
        isExpired = isExpired,
        expiredAt = block(account.network).value.height
    )

    suspend fun getUnspentOutputs(network: Network, privateKey: String): UnspentOutputs {
        return gdkNetworkBackend(network).getUnspentOutputsForPrivateKey(
            params = UnspentOutputsPrivateKeyParams(
                privateKey = privateKey
            )
        )
    }

    suspend fun createTransaction(account: Account, params: CreateTransactionParams): CreateTransaction =
        accountBackend(account).createTransaction(params = params)

    suspend fun createRedepositTransaction(account: Account, params: CreateTransactionParams): CreateTransaction =
        gdkNetworkBackend(account.network).createRedepositTransaction(params)

    suspend fun createSwapTransaction(
        network: Network,
        params: CreateSwapParams,
        twoFactorResolver: TwoFactorResolver
    ): CreateSwapTransaction =
        gdkNetworkBackend(network).createSwapTransaction(params, twoFactorResolver)

    suspend fun completeSwapTransaction(
        network: Network,
        params: CompleteSwapParams,
        twoFactorResolver: TwoFactorResolver
    ): CreateTransaction =
        gdkNetworkBackend(network).completeSwapTransaction(params, twoFactorResolver)

    suspend fun rsaVerify(params: RsaVerifyParams): RsaVerify {
        // TODO clean it
        if (!isNetworkInitialized) {
            prepareHttpRequest()
        }

        return gdkNetworkBackend(defaultNetwork).rsaVerify(params)
    }

    suspend fun signMessage(
        network: Network,
        params: SignMessageParams,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): SignMessage = gdkNetworkBackend(network).signMessage(params, hardwareWalletResolver)

    suspend fun blindTransaction(network: Network, createTransaction: CreateTransaction): CreateTransaction =
        gdkNetworkBackend(network).blindTransaction(createTransaction)

    suspend fun signTransaction(account: Account, createTransaction: CreateTransaction): CreateTransaction = if (account.network.isLightning) {
        createTransaction // no need to sign on gdk side
    } else {
        accountBackend(account).signTransaction(createTransaction)
    }

    suspend fun psbtFromJson(network: Network, transaction: CreateTransaction): Psbt =
        gdkNetworkBackend(network).psbtFromJson(transaction)

    suspend fun psbtIsBase64(psbt: String): Boolean = wally.psbtIsBase64(psbt)

    suspend fun psbtIsBinary(psbt: ByteArray): Boolean = wally.psbtIsBinary(psbt)

    suspend fun psbtToV0(psbt: String): String = wally.psbtToV0(psbt)

    suspend fun sendLightningTransaction(params: CreateTransaction, comment: String?): ProcessedTransactionDetails =
        glNetworkBackend().sendLightningTransaction(params = params, comment = comment).also {
            _walletActiveEventInvalidated = true
        }

    suspend fun broadcastTransaction(
        network: Network,
        broadcastTransaction: BroadcastTransactionParams
    ): ProcessedTransactionDetails = networkBackend(network).broadcastTransaction(broadcastTransaction).also {
        _walletActiveEventInvalidated = true
    }

    suspend fun sendTransaction(
        account: Account,
        signedTransaction: JsonElement,
        isSendAll: Boolean,
        isBump: Boolean,
        twoFactorResolver: TwoFactorResolver
    ): ProcessedTransactionDetails = (if (account.network.isLightning) {
        throw Exception("Use sendLightningTransaction")
    } else {
        (accountBackend(account) as GdkAccountBackend).sendTransaction(
            signedTransaction = signedTransaction,
            twoFactorResolver = twoFactorResolver
        ).also {
            if (isSendAll) {
                _accountEmptiedEvent = account
            }
        }
    }).also {
        // no Send All or Bump transaction
        if (!isSendAll && !isBump) {
            _eventsSharedFlow.emit(WalletEvents.APP_REVIEW)
        }
    }

    private suspend fun accountEmptiedEventIfNeeded() {
        _accountEmptiedEvent?.also { account ->

            val walletHasFunds = accounts.value.any {
                accountBackend(it).assets.value.hasFunds
            }

            countly.accountEmptied(
                session = this@GdkSession,
                walletHasFunds = walletHasFunds,
                accountsFunded = allAccounts.value.count { accountBackend(it).assets.value.hasFunds },
                accounts = this@GdkSession.accounts.value,
                account = account
            )
            _accountEmptiedEvent = null
        }
    }

    fun emptyLightningAccount() {
        _accountEmptiedEvent = _lightningAccount
    }

    private suspend fun walletActiveEventIfNeeded() {
        if (_walletActiveEventInvalidated) {

            val walletHasFunds = accounts.value.any {
                accountBackend(it).assets.value.hasFunds
            }

            countly.activeWalletEnd(
                session = this,
                walletHasFunds = walletHasFunds,
                accountsFunded = allAccounts.value.count { accountBackend(it).assets.value.hasFunds },
                accounts = this.accounts.value
            )
            _walletActiveEventInvalidated = false
        }
    }

    private fun onLightningEvent(event: LightningEvent) {
        when (event) {
            is LightningEvent.Synced -> {
                // Synced is not used in glsdk yet
                updateAccountsAndBalances(updateBalancesForAccounts = listOf(lightningAccount))
            }

            is LightningEvent.NewBlock -> {

            }

            is LightningEvent.InvoicePaid -> {
                // Added here getTransactions call as Synced is not used in glsdk yet
                updateAccountsAndBalances(updateBalancesForAccounts = listOf(lightningAccount))
                _lastInvoicePaid.value = event.paymentHash to event.paymentAmountSatoshi
            }
        }
    }

suspend fun onNewNotification(gaSession: GASession, notification: Notification) = tryCatch{

        val (network, backend) = gdkNetworkBackends.firstNotNullOfOrNull { backend ->
            backend.takeIf { backend.gaSession == gaSession }?.let { it.network to it }
        } ?: return@tryCatch

        logger.d { "onNewNotification ${network.id} \t $notification" }

        // Pass notification to the network backend
        backend.onNewNotification(notification)

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    // SingleSig after connect immediately sends a block with height 0
                    // it's not safe to call getTransactions so early
                    if (it.height > 0) {

                        if (!_disableNotificationHandling) {
                            // Update transactions
                            accounts.value.filter { it.network == network }.also { accounts ->
                                updateAccountsAndBalances(updateBalancesForAccounts = accounts)
                            }
                        }
                    }
                }
            }

            "tor" -> {
                // Get TOR notification only from the default network
                if (network == defaultNetwork) {
                    notification.tor?.let {
                        _torStatusSharedFlow.value = it
                    }
                }
            }

            "subaccount" -> {
                if (!_disableNotificationHandling && notification.subaccount?.isSynced == true) {
                    updateAccountsAndBalances()
                }
            }

            "transaction" -> {
                if (!_disableNotificationHandling) {
                    notification.transaction?.let { event ->

                        event.subaccounts.mapNotNull { subAccount ->
                            accounts.value.find {
                                it.network == network && it.pointer == subAccount
                            }
                        }.toSet().also { accounts ->
                            updateAccountsAndBalances(updateBalancesForAccounts = accounts)
                        }
                    }
                }
            }
        }
    }

    private suspend fun cacheAssets(assetIds: Collection<String>) {
        assetIds.filter { it != BTC_POLICY_ASSET && it != LN_BTC_POLICY_ASSET }.takeIf { it.isNotEmpty() }?.also {
            logger.d { "Cache assets: $it" }
            networkAssetManager.cacheAssets(it, this)
        }
    }

    fun hasAssetIcon(assetId: String) = networkAssetManager.hasAssetIcon(assetId)
    suspend fun getAsset(assetId: String): Asset? = networkAssetManager.getAsset(assetId, this)

    private fun createEcPrivateKey(): ByteArray {
        var privateKey: ByteArray
        do {
            privateKey = gdk.getRandomBytes(wally.ecPrivateKeyLen)
        } while (!wally.ecPrivateKeyVerify(privateKey))

        return privateKey
    }

    suspend fun jadePsbtRequest(psbt: String): BcurEncodedData {

        val params = BcurEncodeParams(
            urType = "crypto-psbt",
            data = psbt
        )

        return bcurEncode(params)
    }

    suspend fun jadePinRequest(payload: String): BcurEncodedData {

        val params = BcurEncodeParams(
            urType = "jade-pin",
            data = payload
        )

        return bcurEncode(params)
    }

    suspend fun jadeBip8539Request(index: Long): Pair<ByteArray, BcurEncodedData> {
        val privateKey = createEcPrivateKey()

        val params = BcurEncodeParams(
            urType = "jade-bip8539-request",
            numWords = 12,
            index = index,
            privateKey = privateKey.toHex()
        )

        return privateKey to bcurEncode(params)
    }

    fun jadeBip8539Reply(privateKey: ByteArray, publicKey: ByteArray, encrypted: ByteArray): String? {
        return wally.bip85FromJade(privateKey, publicKey, "bip85_bip39_entropy", encrypted)
    }

    suspend fun bcurEncode(params: BcurEncodeParams): BcurEncodedData {
        val network = defaultNetworkOrNull ?: networks.bitcoinElectrum

        if (!isConnected) {
            connect(network = network, initNetworks = listOf(network))
        }

        return gdkNetworkBackend(defaultNetworkOrNull ?: networks.bitcoinElectrum).bcurEncode(
            params = params
        )
    }

    suspend fun bcurDecode(params: BcurDecodeParams, bcurResolver: BcurResolver): BcurDecodedData {
        val network = defaultNetworkOrNull ?: networks.bitcoinElectrum

        if (!isConnected) {
            connect(network = network, initNetworks = listOf(network))
        }

        return gdkNetworkBackend(defaultNetworkOrNull ?: networks.bitcoinElectrum).bcurDecode(
            params = params, bcurResolver = bcurResolver
        )
    }

    suspend fun parseInput(input: String): Pair<Network, LightningInputType?>? =
        withContext(context = Dispatchers.Default) {
            networkBackends.mapNotNull {
                if (it.value.isAddressValid(input)) {
                    it.key to (it.value as? GlNetworkBackend)?.addressType(input)
                } else {
                    null
                }
            }.firstOrNull() ?: run {
                // Fall back to LWK for BOLT12 detection (and BOLT11/LNURL if Breez missed them).
                // Propagate BIP-353-specific errors instead of swallowing so the user sees
                // why resolution failed.
                try {
                    when (lwkOrNull?.inspectPaymentInstruction(input)) {
                        is PaymentInstruction.Bolt12,
                        is PaymentInstruction.Bolt11,
                        is PaymentInstruction.LnUrl -> lightning to null

                        null -> null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e.message == "id_failed_to_resolve_bip353_payment_request") throw e
                    e.printStackTrace()
                    null
                }
            }
        }

    fun supportId(): String = (allAccounts.value.filter {
        it.isMultisig && it.pointer == 0L
    }.map { "${it.network.bip21Prefix}:${it.receivingId}" } + listOfNotNull(
        lightning.bip21Prefix.let { bip21Prefix ->
            lightningNodeId?.let { "$bip21Prefix:$it" }
        }

    )).joinToString(",")

    internal fun destroy(disconnect: Boolean = true) {
        if (disconnect) {
            disconnectAsync()
        }
        scope.cancel("Destroy")
    }

    companion object : Loggable() {
        const val DEFAULT_TRANSACTIONS_LIMIT = 10

        // Default for settings(network) when both the requested network and defaultNetworkOrNull are null (pre-login).
        private val NULL_SETTINGS: StateFlow<Settings?> = MutableStateFlow(null)

        val BlockstreamWhitelistedUrls = listOf(
            "https://j8d.io/",
            "https://jadepin.blockstream.com/",
            "http://mrrxtq6tjpbnbm7vh5jt6mpjctn7ggyfy5wegvbeff3x7jrznqawlmid.onion/", // onion jadepin
            "https://jadefw.blockstream.com/",
            "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion/" // onion jadefw
        )
    }
}

