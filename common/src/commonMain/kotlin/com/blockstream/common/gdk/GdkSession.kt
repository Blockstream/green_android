package com.blockstream.common.gdk

import breez_sdk.BreezEvent
import breez_sdk.InputType
import breez_sdk.InvoicePaidDetails
import breez_sdk.LnUrlPayResult
import breez_sdk.ReceivePaymentResponse
import breez_sdk.SwapInfo
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.CountlyBase
import com.blockstream.common.LN_BTC_POLICY_ASSET
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.CountlyAsset
import com.blockstream.common.data.DataState
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.ExceptionWithSupportData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.HwWatchOnlyCredentials
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.RichWatchOnly
import com.blockstream.common.data.SupportData
import com.blockstream.common.database.wallet.LoginCredentials
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.devices.DeviceState
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.needs2faActivation
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.extensions.title
import com.blockstream.common.extensions.toSortedLinkedHashMap
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Accounts
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.data.Addressee
import com.blockstream.common.gdk.data.Asset
import com.blockstream.common.gdk.data.Assets
import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.gdk.data.BcurDecodedData
import com.blockstream.common.gdk.data.BcurEncodedData
import com.blockstream.common.gdk.data.Block
import com.blockstream.common.gdk.data.CreateSwapTransaction
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.gdk.data.EncryptWithPin
import com.blockstream.common.gdk.data.FeeEstimation
import com.blockstream.common.gdk.data.LoginData
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.NetworkEvent
import com.blockstream.common.gdk.data.Notification
import com.blockstream.common.gdk.data.Output
import com.blockstream.common.gdk.data.PendingTransaction
import com.blockstream.common.gdk.data.PreviousAddresses
import com.blockstream.common.gdk.data.ProcessedTransactionDetails
import com.blockstream.common.gdk.data.Psbt
import com.blockstream.common.gdk.data.RsaVerify
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.SignMessage
import com.blockstream.common.gdk.data.TorEvent
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.data.Transactions
import com.blockstream.common.gdk.data.TwoFactorConfig
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.gdk.data.TwoFactorReset
import com.blockstream.common.gdk.data.UnspentOutputs
import com.blockstream.common.gdk.data.Utxo
import com.blockstream.common.gdk.data.ValidateAddressees
import com.blockstream.common.gdk.data.WalletEvents
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.BalanceParams
import com.blockstream.common.gdk.params.BcurDecodeParams
import com.blockstream.common.gdk.params.BcurEncodeParams
import com.blockstream.common.gdk.params.BroadcastTransactionParams
import com.blockstream.common.gdk.params.CompleteSwapParams
import com.blockstream.common.gdk.params.ConnectionParams
import com.blockstream.common.gdk.params.Convert
import com.blockstream.common.gdk.params.CreateSwapParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.gdk.params.CredentialsParams
import com.blockstream.common.gdk.params.CsvParams
import com.blockstream.common.gdk.params.DecryptWithPinParams
import com.blockstream.common.gdk.params.DeviceParams
import com.blockstream.common.gdk.params.EncryptWithPinParams
import com.blockstream.common.gdk.params.GetAssetsParams
import com.blockstream.common.gdk.params.Limits
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.gdk.params.PreviousAddressParams
import com.blockstream.common.gdk.params.ReceiveAddressParams
import com.blockstream.common.gdk.params.ReconnectHintParams
import com.blockstream.common.gdk.params.RsaVerifyParams
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.gdk.params.SubAccountsParams
import com.blockstream.common.gdk.params.TransactionParams
import com.blockstream.common.gdk.params.TransactionParams.Companion.TRANSACTIONS_PER_PAGE
import com.blockstream.common.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.common.gdk.params.UpdateSubAccountParams
import com.blockstream.common.gdk.params.ValidateAddresseesParams
import com.blockstream.common.interfaces.JadeHttpRequestUrlValidator
import com.blockstream.common.lightning.AppGreenlightCredentials
import com.blockstream.common.lightning.LightningBridge
import com.blockstream.common.lightning.LightningManager
import com.blockstream.common.lightning.expireIn
import com.blockstream.common.lightning.fromInvoice
import com.blockstream.common.lightning.fromLnUrlPay
import com.blockstream.common.lightning.fromPayment
import com.blockstream.common.lightning.fromReverseSwapInfo
import com.blockstream.common.lightning.fromSwapInfo
import com.blockstream.common.lightning.isExpired
import com.blockstream.common.lightning.maxSendableSatoshi
import com.blockstream.common.lightning.minSendableSatoshi
import com.blockstream.common.lightning.sendableSatoshi
import com.blockstream.common.managers.AssetManager
import com.blockstream.common.managers.AssetsProvider
import com.blockstream.common.managers.NetworkAssetManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.randomChars
import com.blockstream.common.utils.server
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toHex
import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.utils.Loggable
import com.blockstream.jade.HttpRequestHandler
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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
import kotlin.math.absoluteValue

typealias AccountId = String

/* Handles multiple GDK sessions per network */
class GdkSession constructor(
    private val userAgent: String,
    private val appInfo: AppInfo,
    private val appConfig: AppConfig,
    private val sessionManager: SessionManager,
    private val lightningManager: LightningManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val gdk: Gdk,
    private val wally: Wally,
    private val countly: CountlyBase
) : HttpRequestHandler, AssetsProvider {
    private fun createScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) =
        CoroutineScope(SupervisorJob() + dispatcher + logException(countly))

    private val scope = createScope(Dispatchers.Default)
    private val parentJob = SupervisorJob()

    var jadeHttpRequestUrlValidator: JadeHttpRequestUrlValidator? = null

    val logs: String
        get() = gdk.logs.toString()

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
    private var _activeAccountStateFlow: MutableStateFlow<Account?> = MutableStateFlow(null)
    private var _walletAssetsFlow: MutableStateFlow<DataState<Assets>> = MutableStateFlow(DataState.Loading)
    private var _enrichedAssetsFlow: MutableStateFlow<List<CountlyAsset>> = MutableStateFlow(listOf())
    private var _walletHasHistorySharedFlow = MutableStateFlow(false)
    private var _accountAssetsFlow = mutableMapOf<AccountId, MutableStateFlow<Assets>>()
    private val _accountsAndBalanceUpdatedSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    private var _walletTransactionsStateFlow: MutableStateFlow<DataState<List<Transaction>>> = MutableStateFlow(DataState.Loading)
    private var _accountTransactionsStateFlow = mutableMapOf<AccountId, MutableStateFlow<DataState<List<Transaction>>>>()
    private var _accountTransactionsPagerStateFlow = mutableMapOf<AccountId, MutableStateFlow<Boolean>>()
    private var _blockStateFlow = mutableMapOf<Network, MutableStateFlow<Block>>()
    private var _settingsStateFlow = mutableMapOf<Network, MutableStateFlow<Settings?>>()
    private var _twoFactorConfigStateFlow = mutableMapOf<Network, MutableStateFlow<TwoFactorConfig?>>()
    private var _twoFactorResetStateFlow = mutableMapOf<Network, MutableStateFlow<TwoFactorReset?>>()
    private var _networkEventsStateFlow = mutableMapOf<Network, MutableStateFlow<NetworkEvent?>>()
    private val _networkErrors: Channel<Pair<Network, NetworkEvent>> = Channel()
    private var _failedNetworksStateFlow: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    private var _systemMessageStateFlow: MutableStateFlow<List<Pair<Network, String>>> = MutableStateFlow(listOf())
    private var _allAccountsStateFlow = MutableStateFlow<List<Account>>(listOf())
    private var _accountsStateFlow = MutableStateFlow<List<Account>>(listOf())
    private var _expired2FAStateFlow = MutableStateFlow<List<Account>>(listOf())
    private val _lastInvoicePaid = MutableStateFlow<InvoicePaidDetails?>(null)
    private var _accountAssetStateFlow = MutableStateFlow<List<AccountAsset>>(listOf())
    private val _torStatusSharedFlow = MutableStateFlow<TorEvent>(TorEvent(progress = 100))
    private val _tickerSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    private val _zeroAccounts = MutableStateFlow<Boolean>(false)

    private val _multisigBitcoinWatchOnly = MutableStateFlow<String?>(null)
    private val _multisigLiquidWatchOnly = MutableStateFlow<String?>(null)

    var logoutReason: LogoutReason? = null
        private set

    private fun networkEventsStateFlow(network: Network) = _networkEventsStateFlow.getOrPut(network) { MutableStateFlow(null) }

    private fun twoFactorResetStateFlow(network: Network) = _twoFactorResetStateFlow.getOrPut(network) { MutableStateFlow(null) }

    private fun settingsStateFlow(network: Network) = _settingsStateFlow.getOrPut(network) { MutableStateFlow(null) }
    private fun twoFactorConfigStateFlow(network: Network) = _twoFactorConfigStateFlow.getOrPut(network) { MutableStateFlow(null) }

    private fun blockStateFlow(network: Network) = _blockStateFlow.getOrPut(network) { MutableStateFlow(Block(height = 0)) }

    private fun accountTransactionsPagerStateFlow(account: Account): MutableStateFlow<Boolean> {
        return _accountTransactionsPagerStateFlow.getOrPut(account.id) {
            MutableStateFlow(false)
        }
    }

    private fun accountAssetsStateFlow(account: Account): MutableStateFlow<Assets> {
        return _accountAssetsFlow.getOrPut(account.id) {
            MutableStateFlow(Assets())
        }
    }

    private fun accountTransactionsStateFlow(account: Account): MutableStateFlow<DataState<List<Transaction>>> {
        return _accountTransactionsStateFlow.getOrPut(account.id) {
            MutableStateFlow(DataState.Loading)
        }
    }

    val activeAccount get() = _activeAccountStateFlow.asStateFlow()

    val walletTotalBalance get() = _walletTotalBalanceSharedFlow.asStateFlow()
    val walletTotalBalanceDenominationStateFlow = MutableStateFlow<Denomination>(Denomination.BTC)

    val walletAssets: StateFlow<DataState<Assets>> get() = _walletAssetsFlow.asStateFlow()

    val enrichedAssets: StateFlow<List<CountlyAsset>> get() = _enrichedAssetsFlow.asStateFlow()

    val walletHasHistory get() = _walletHasHistorySharedFlow.value

    fun accountAssets(account: Account) = accountAssetsStateFlow(account).asStateFlow()

    val accountsAndBalanceUpdated get() = _accountsAndBalanceUpdatedSharedFlow.asSharedFlow()

    val walletTransactions get() = _walletTransactionsStateFlow.asStateFlow()

    val failedNetworks get() = _failedNetworksStateFlow.asStateFlow()

    val systemMessage get() = _systemMessageStateFlow.asStateFlow()

    val allAccounts: StateFlow<List<Account>> get() = _allAccountsStateFlow.asStateFlow()

    val accounts: StateFlow<List<Account>> get() = _accountsStateFlow.asStateFlow()

    val expired2FA: StateFlow<List<Account>> get() = _expired2FAStateFlow.asStateFlow()

    val lastInvoicePaid = _lastInvoicePaid.asStateFlow()

    val accountAsset: StateFlow<List<AccountAsset>> get() = _accountAssetStateFlow.asStateFlow()

    val torStatusFlow = _torStatusSharedFlow.asStateFlow()

    val tickerFlow = _tickerSharedFlow.asSharedFlow()

    val zeroAccounts = _zeroAccounts.asStateFlow()

    val multisigBitcoinWatchOnly = _multisigBitcoinWatchOnly.asStateFlow()
    val multisigLiquidWatchOnly = _multisigLiquidWatchOnly.asStateFlow()

    val networkErrors = _networkErrors.receiveAsFlow()
    fun getEnrichedAssets(id: String?) = _enrichedAssetsFlow.value.find { it.assetId == id }

    fun accountTransactions(account: Account) = accountTransactionsStateFlow(account).asStateFlow()

    fun accountTransactionsPager(account: Account) = accountTransactionsPagerStateFlow(account).asStateFlow()

    fun block(network: Network): StateFlow<Block> = blockStateFlow(network).asStateFlow()

    fun settings(network: Network? = null) =
        (network ?: defaultNetworkOrNull)?.let { settingsStateFlow(it).asStateFlow() } ?: MutableStateFlow(null)

    fun twoFactorConfig(network: Network = defaultNetwork) = twoFactorConfigStateFlow(network).asStateFlow()

    fun twoFactorReset(network: Network) = twoFactorResetStateFlow(network).asStateFlow()

    fun networkEvents(network: Network) = networkEventsStateFlow(network).asStateFlow()

    val hasLiquidAccount: Boolean get() = accounts.value.any { it.isLiquid }

    var defaultNetworkOrNull: Network? = null
        private set

    val defaultNetwork: Network
        get() = defaultNetworkOrNull!!

    val mainAssetNetwork
        get() = bitcoin ?: defaultNetwork

    var lightning: Network? = null

    val bitcoin
        get() = bitcoinSinglesig ?: bitcoinMultisig

    val bitcoinSinglesig
        get() = gdkSessions.firstNotNullOfOrNull { it.key.takeIf { network -> network.isElectrum && network.isBitcoin } }

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

    val gdkSessions: ConcurrentMutableMap<Network, GASession> = ConcurrentMutableMap()
    val activeSessions = mutableSetOf<Network>()

    fun hasActiveNetwork(network: Network?) = activeSessions.contains(network)

    private val activeGdkSessions: Map<Network, GASession>
        get() = gdkSessions.filter { activeSessions.contains(it.key) }

    private fun gdkSession(network: Network) = gdkSessions.getOrPut(network) {
        gdk.createSession()
    }

    var hasLightning: Boolean = false
        private set

    var isLightningShortcut: Boolean = false
        private set

    val hasAmpAccount: Boolean
        get() = accounts.value.find { it.type == AccountType.AMP_ACCOUNT } != null

    private var _lightningAccount: Account? = null

    val lightningAccount: Account
        get() {
            if (_lightningAccount == null) {
                _lightningAccount = Account(
                    gdkName = "Instant",
                    pointer = 0,
                    type = AccountType.LIGHTNING
                ).also {
                    it.setup(this, lightning!!)
                }
            }

            return _lightningAccount!!
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

    private var authenticationRequired = mutableMapOf<Network, Boolean>()

    val networks
        get() = gdk.networks()

    private val _isConnectedState = MutableStateFlow(false)

    @NativeCoroutinesIgnore
    val isConnectedState = _isConnectedState.asStateFlow()
    val isConnected
        get() = isConnectedState.value

    var xPubHashId: String? = null
        private set

    var pendingTransactionParams: CreateTransactionParams? = null
    var pendingTransaction: PendingTransaction? = null

    val networkAssetManager: NetworkAssetManager
        get() = assetManager.getNetworkAssetManager(defaultNetworkOrNull?.let { isMainnet } ?: true)

    val hideAmounts: Boolean get() = settingsManager.appSettings.hideAmounts

    val starsOrNull: String? get() = "*****".takeIf { hideAmounts }

    private var _accountEmptiedEvent: Account? = null
    private var _walletActiveEventInvalidated = true

    var lightningSdkOrNull: LightningBridge? = null
        private set

    val lightningSdk
        get() = lightningSdkOrNull!!

    private val _tempAllowedServers = mutableListOf<String>()

    init {
        _accountsAndBalanceUpdatedSharedFlow.onEach {
            var walletBalance = 0L

            accounts.value.forEach {
                walletBalance += this.accountAssets(it).value.policyAsset
            }

            _walletTotalBalanceSharedFlow.value = walletBalance
        }.launchIn(scope)

        countly.remoteConfigUpdateEvent.onEach {
            updateEnrichedAssets()
        }.launchIn(scope)

        isConnectedState.drop(1).onEach {
            sessionManager.fireConnectionChangeEvent()
        }.launchIn(scope)
    }

    fun watchOnlyDeviceConnect(device: GreenDevice) {
        this.device = device
    }

    private fun authHandler(network: Network, gaAuthHandler: GAAuthHandler): AuthHandler =
        AuthHandler(
            session = this,
            gaAuthHandler = gaAuthHandler,
            network = network,
            gdkHwWallet = gdkHwWallet,
            gdk = gdk
        )

    private fun updateEnrichedAssets() {
        if (isNetworkInitialized && (!isNoBlobWatchOnly || isHwWatchOnly)) {
            countly.getRemoteConfigValueForAssets(if (isMainnet) LIQUID_ASSETS_KEY else LIQUID_ASSETS_TESTNET_KEY)?.also {
                cacheAssets(it.map { it.assetId })
            }.also {
                _enrichedAssetsFlow.value = it ?: listOf()
            }
        }
    }

    fun reportLightningError(paymentHash: String) {
        lightningSdkOrNull?.also {
            it.reportIssue(paymentHash)
        }
    }

    fun setEphemeralWallet(wallet: GreenWallet) {
        ephemeralWallet = wallet
    }

    fun walletExistsAndIsUnlocked(network: Network?) = network?.let { getTwoFactorReset(network)?.isActive != true } ?: false
    fun getTwoFactorReset(network: Network): TwoFactorReset? = twoFactorReset(network).value
    fun getSettings(network: Network? = null): Settings? {
        return (network?.let { it.takeIf { !it.isLightning } ?: bitcoin } ?: defaultNetworkOrNull)?.let { network ->
            settingsStateFlow(network).value ?: try {
                updateSettings(network = network)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun updateSettings(network: Network? = null): Settings? {
        activeSessions.filter { network == null || it.network == network.id }.forEach {
            settingsStateFlow(it).value = gdk.getSettings(gdkSession(it))
            if (it.isMultisig) {
                updateTwoFactorConfig(network = it)
            }
        }

        return (network?.let { it.takeIf { !it.isLightning } ?: bitcoin } ?: defaultNetworkOrNull)?.let {
            settingsStateFlow(it).value
        }
    }

    fun getTwoFactorConfig(network: Network): TwoFactorConfig? {
        return twoFactorConfigStateFlow(network).value ?: try {
            updateTwoFactorConfig(network = network, useCache = false)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateTwoFactorConfig(network: Network, useCache: Boolean = false): TwoFactorConfig? {
        if (!network.isMultisig) return null

        var config = if (useCache) twoFactorConfigStateFlow(network).value else null

        if (config == null) {
            try {
                gdk.getTwoFactorConfig(gdkSession(network)).also {
                    twoFactorConfigStateFlow(network).value = it
                    twoFactorResetStateFlow(network).value = it.twoFactorReset
                    config = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                countly.recordException(e)
            }
        }

        return config
    }

    suspend fun changeSettings(network: Network, settings: Settings) =
        authHandler(network, gdk.changeSettings(gdkSession(network), settings)).resolve()

    suspend fun changeGlobalSettings(settings: Settings) {
        val exceptions = mutableListOf<Exception>()
        activeSessions.forEach { network ->
            getSettings(network)?.also { networkSettings ->

                if (walletExistsAndIsUnlocked(network)) {
                    try {
                        changeSettings(
                            network,
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
    fun availableCurrencies() = gdk.getAvailableCurrencies(gdkSession(defaultNetwork))

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

        if (network.isElectrum) {
            electrumUrl = applicationSettings.getPersonalElectrumServer(network).takeIf { it.isNotBlank() }
        }

        val useTor = applicationSettings.tor

        return ConnectionParams(
            networkName = network.id,
            useTor = useTor,
            userAgent = userAgent,
            proxy = applicationSettings.proxyUrl ?: "",
            gapLimit = if (network.isSinglesig) applicationSettings.electrumServerGapLimit?.coerceAtLeast(1) else null,
            electrumTls = if (electrumUrl.isNotBlank()) applicationSettings.personalElectrumServerTls else true,
            electrumUrl = electrumUrl,
            electrumOnionUrl = electrumUrl.takeIf { useTor },
            // blobServerUrl = "wss://green-blobserver.staging.blockstream.com/ws".takeIf { appInfo.isDevelopment && network.isSinglesig && network.isTestnet },
            // blobServerOnionUrl = null,
        ).also {
            logger.d { "Connection Params: $it" }
        }
    }

    // Use it only for connected sessions
    fun supportsLightning() = supportsLightning(isWatchOnly = isWatchOnlyValue, device = device)

    private fun supportsLightning(isWatchOnly: Boolean, device: GreenDevice?): Boolean {
        return appConfig.lightningFeatureEnabled && !isWatchOnly && (device == null || device.isJade)
    }

    fun networks(isTestnet: Boolean, isWatchOnly: Boolean, device: GreenDevice?): List<Network> {
        return if (isTestnet) {
            listOfNotNull(
                networks.testnetBitcoinElectrum,
                networks.testnetBitcoinGreen,
                networks.testnetLiquidElectrum,
                networks.testnetLiquidGreen
            )
        } else {
            listOfNotNull(
                networks.bitcoinElectrum,
                networks.bitcoinGreen,
                networks.liquidElectrum,
                networks.liquidGreen,
                networks.lightning.takeIf { supportsLightning(isWatchOnly, device) }, // Only SW οr Jade
            )
        }
    }

    private fun initGdkSessions(initNetworks: List<Network>?) {
        (if (initNetworks.isNullOrEmpty()) {
            networks(isTestnet = isTestnet, isWatchOnly = isWatchOnlyValue, device = device)
        } else {
            // init the provided networks
            initNetworks
        }).forEach {
            if (it.isLightning) {
                lightning = it
            } else {
                gdkSession(it)
            }
        }
    }

    suspend fun connect(network: Network, initNetworks: List<Network>? = null): List<Network> {
        defaultNetworkOrNull = network

        disconnect()

        initGdkSessions(initNetworks = initNetworks)

        return gdkSessions.map {
            scope.async(start = CoroutineStart.DEFAULT) {
                try {
                    gdk.connect(it.value, createConnectionParams(it.key))
                    it.key
                } catch (e: Exception) {
                    _failedNetworksStateFlow.value += it.key
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    fun getProxySettings() = gdk.getProxySettings(gdkSession(defaultNetwork))

    fun reconnectHint(hint: ReconnectHintParams) =
        scope.launch(context = logException(countly)) {
            gdkSessions.forEach {
                gdk.reconnectHint(it.value, hint)
            }
        }

    private val disconnectMutex = Mutex()
    suspend fun disconnect() {
        logger.d { "Disconnect" }

        _isConnectedState.value = false
        xPubHashId = null

        authenticationRequired.clear()

        _activeAccountStateFlow.value = null

        // Recreate subject so that can be sure we have fresh data, especially on shared sessions eg. HWW sessions
        _accountsStateFlow.value = listOf()
        _expired2FAStateFlow.value = listOf()
        _allAccountsStateFlow.value = listOf()
        _accountAssetStateFlow.value = listOf()
        _systemMessageStateFlow.value = listOf()
        _failedNetworksStateFlow.value = listOf()

        _blockStateFlow = mutableMapOf()
        _settingsStateFlow = mutableMapOf()
        _twoFactorConfigStateFlow = mutableMapOf()
        _twoFactorResetStateFlow = mutableMapOf()
        _networkEventsStateFlow = mutableMapOf()

        _accountAssetsFlow = mutableMapOf()
        _torStatusSharedFlow.value = TorEvent(progress = 100) // reset TOR status

        _multisigBitcoinWatchOnly.value = null
        _multisigLiquidWatchOnly.value = null

        // Clear HW derived lightning mnemonic
        _derivedHwLightningMnemonic = null

        _walletHasHistorySharedFlow.value = false

        // Clear total balance
        _walletTotalBalanceSharedFlow.value = -1L
        walletTotalBalanceDenominationStateFlow.value = Denomination.BTC

        // Clear Assets
        _walletAssetsFlow.value = DataState.Loading

        // Clear Enriched Assets
        _enrichedAssetsFlow.value = listOf()

        // Clear Transactions
        _walletTransactionsStateFlow.value = DataState.Loading
        _accountTransactionsStateFlow = mutableMapOf()
        _accountTransactionsPagerStateFlow = mutableMapOf()

        _tempAllowedServers.clear()

        _walletActiveEventInvalidated = true
        _accountEmptiedEvent = null

        val gaSessionToBeDestroyed = disconnectMutex.withLock { gdkSessions.values.toList() }

        // Create a new gaSession
        gdkSessions.clear()

        // Clear Lightning
        hasLightning = false
        lightning = null
        _lightningAccount = null

        lightningSdkOrNull?.release()
        lightningSdkOrNull = null

        // Stop all jobs
        parentJob.cancelChildren()

        // Clear active sessions
        activeSessions.clear()

        // Destroy gaSession
        gaSessionToBeDestroyed.forEach {
            logger.d { "Destroy GDK Session $it" }
            gdk.destroySession(it)
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

    private fun resetNetwork(network: Network) {
        // Remove as active network
        activeSessions.remove(network)

        gdkSessions.remove(network)?.also { gaSessionToBeDestroyed ->
            gdk.destroySession(gaSessionToBeDestroyed)
        }

        // Init a new Session and connect
        try {
            gdk.connect(gdkSession(network), createConnectionParams(network))
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        val urls = details.jsonObject["urls"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: listOf()

        jadeHttpRequestUrlValidator?.also { urlValidator ->
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
        }

        return gdk.httpRequest(gdkSession(defaultNetwork), details).also {
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
    }

    @NativeCoroutinesIgnore
    suspend fun initLightningIfNeeded(mnemonic: String?) {

        if (lightning != null) {
            if (lightningSdkOrNull == null && supportsLightning()) {
                // Init SDK
                initLightningSdk(mnemonic)
            }

            if (!hasLightning) {
                connectToGreenlight(mnemonic = mnemonic ?: deriveLightningMnemonic(), restoreOnly = false)

                if (!hasLightning) {
                    throw Exception("Something went wrong while initiating your Lightning account")
                }

                // update GreenSessions accounts (use cache)
                updateAccounts()

                // Update accounts & balances & transactions for the new network
                updateAccountsAndBalances(updateBalancesForNetwork = lightning)
                updateWalletTransactions(updateForNetwork = lightning)
            }
        }
    }

    suspend fun <T> initNetworkIfNeeded(
        network: Network,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        action: suspend () -> T
    ): T {
        if (!activeSessions.contains(network)) {

            try {
                gdk.connect(gdkSession(network), createConnectionParams(network))
            } catch (e: Exception) {
                // catch if already connected
            }

            val previousMultisig = activeMultisig.firstOrNull()

            val loginCredentialsParams = if (isHardwareWallet) {
                LoginCredentialsParams.empty
            } else {
                LoginCredentialsParams.fromCredentials(getCredentials())
            }

            val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

            authHandler(
                network,
                gdk.registerUser(
                    session = gdkSession(network),
                    deviceParams = deviceParams,
                    loginCredentialsParams = loginCredentialsParams
                )
            ).resolve(hardwareWalletResolver = hardwareWalletResolver)

            authHandler(
                network,
                gdk.loginUser(
                    session = gdkSession(network),
                    deviceParams = deviceParams,
                    loginCredentialsParams = loginCredentialsParams
                )
            ).resolve(hardwareWalletResolver = hardwareWalletResolver)

            activeSessions.add(network)

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

            // update GreenSessions accounts (use cache)
            updateAccounts()

            // Update accounts & balances & transactions for the new network
            updateAccountsAndBalances(updateBalancesForAccounts = networkAccounts)
            updateWalletTransactions(updateForAccounts = networkAccounts)
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
                        connectToGreenlight(mnemonic = deriveLightningMnemonic(), restoreOnly = false)
                    } else {
                        try {
                            gdk.connect(gdkSession(network), createConnectionParams(network))
                        } catch (e: Exception) {
                            // catch if already connected
                        }

                        val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

                        authHandler(
                            network,
                            gdk.loginUser(
                                session = gdkSession(network),
                                deviceParams = deviceParams,
                                loginCredentialsParams = loginCredentialsParams
                            )
                        ).resolve(hardwareWalletResolver = hardwareWalletResolver)
                    }

                    activeSessions.add(network)
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (e.message != "id_login_failed") {
                        failedNetworkLogins.add(network)
                    }
                }
            }

            _failedNetworksStateFlow.value = failedNetworkLogins

            updateAccountsAndBalances()
            updateWalletTransactions()
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
        appGreenlightCredentials: AppGreenlightCredentials?,
        isRestore: Boolean = false,
        initializeSession: Boolean = true,
    ): LoginData {
        val initNetworks = if (wallet.isLightning) {
            listOf(
                networks.bitcoinElectrum,
                networks.lightning
            )
        } else {
            null
        }

        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(wallet, loginCredentials),
            initNetworks = initNetworks,
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
            appGreenlightCredentials = appGreenlightCredentials,
            isRestore = isRestore,
            initializeSession = initializeSession
        )
    }

    suspend fun loginWithMnemonic(
        isTestnet: Boolean,
        wallet: GreenWallet? = null,
        loginCredentialsParams: LoginCredentialsParams,
        appGreenlightCredentials: AppGreenlightCredentials? = null,
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
            appGreenlightCredentials = appGreenlightCredentials,
            initNetworks = initNetworks,
            isSmartDiscovery = isSmartDiscovery,
            isCreate = isCreate,
            isRestore = isRestore,
            initializeSession = initializeSession
        )
    }

    suspend fun loginLightningShortcut(
        wallet: GreenWallet,
        mnemonic: String
    ): LoginData {
        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(false),
            initNetworks = listOf(prominentNetwork(false), networks.lightning),
            wallet = wallet,
            walletLoginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic),
        )
    }

    suspend fun loginWatchOnly(
        wallet: GreenWallet,
        loginCredentials: LoginCredentials? = null,
        watchOnlyCredentials: HwWatchOnlyCredentials
    ) {
        loginWatchOnly(network = prominentNetwork(wallet, loginCredentials), wallet = wallet, watchOnlyCredentials = watchOnlyCredentials)
    }

    suspend fun loginWatchOnly(network: Network, wallet: GreenWallet?, watchOnlyCredentials: HwWatchOnlyCredentials): LoginData {
        return loginWatchOnly(
            network = network,
            wallet = wallet,
            loginCredentialsParams = watchOnlyCredentials.toLoginCredentials()
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
    private suspend fun loginWatchOnly(network: Network, wallet: GreenWallet?, loginCredentialsParams: LoginCredentialsParams): LoginData {
        val initNetworks = loginCredentialsParams.hwWatchOnlyCredentials?.credentials?.keys?.map {
            networkBy(it)
        } ?: listOf(network)

        return loginWithLoginCredentials(
            prominentNetwork = network,
            initNetworks = initNetworks,
            wallet = wallet,
            walletLoginCredentialsParams = loginCredentialsParams
        )
    }

    suspend fun loginWithDevice(
        wallet: GreenWallet,
        device: GreenDevice,
        derivedLightningMnemonic: String?,
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
            derivedLightningMnemonic = derivedLightningMnemonic,
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
        appGreenlightCredentials: AppGreenlightCredentials? = null,
        derivedLightningMnemonic: String? = null,
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
        isHwWatchOnly = walletLoginCredentialsParams.hwWatchOnlyCredentials != null
        _isWatchOnly.value = walletLoginCredentialsParams.isWatchOnly

        isNoBlobWatchOnly = isWatchOnlyValue && richWatchOnly == null
        isRichWatchOnly = isWatchOnlyValue && richWatchOnly != null
        isCoreDescriptorWatchOnly =
            isWatchOnlyValue && (walletLoginCredentialsParams.coreDescriptors != null || walletLoginCredentialsParams.hwWatchOnlyCredentials != null)
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

        logger.d { "loginWithLoginCredentials connected: ${gdkSessions.keys.joinToString(",") { it.id }} " }

        val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

        val initAccount = wallet?.activeAccount
        val initNetwork = wallet?.activeNetwork

        // Get enabled singlesig networks (multisig can be identified by login_user)
        val enabledGdkSessions = gdkSessions.toSortedLinkedHashMap { n1, n2 -> // Sort prominent first
            if (n1 == prominentNetwork) {
                -1
            } else if (n2 == prominentNetwork) {
                1
            } else {
                n1.id.compareTo(n2.id)
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

        isLightningShortcut = wallet?.isLightning == true
        hasLightning = isLightningShortcut || appGreenlightCredentials != null || derivedLightningMnemonic != null

        return (enabledGdkSessions.mapNotNull { gdkSession ->
            scope.async(start = CoroutineStart.LAZY) {
                val isProminent = gdkSession.key == prominentNetwork
                val network = gdkSession.key

                val networkLoginCredentialsParams =
                    richWatchOnly?.find { it.network == network.id }?.toLoginCredentialsParams()
                        ?: loginCredentialsParams.hwWatchOnlyCredentialsToLoginCredentialsParams(network.id)
                        ?: loginCredentialsParams

                try {
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

                    if (gdkSession.key.isSinglesig && !isProminent && !isRestore && !isSmartDiscovery && !hasGdkCache && !isHwWatchOnly) {
                        logger.i { "Skip login in ${network.id}" }
                        return@async null
                    }

                    // On Create just login into Bitcoin network
                    if (isCreate && (gdkSession.key.isMultisig || gdkSession.key.isLiquid)) {
                        logger.i { "Skip login in ${network.id}" }
                        return@async null
                    }

                    logger.i { "Login into ${network.id}" }

                    authHandler(
                        gdkSession.key,
                        gdk.loginUser(
                            session = gdkSession.value,
                            deviceParams = deviceParams,
                            loginCredentialsParams = networkLoginCredentialsParams
                        )
                    ).result<LoginData>(hardwareWalletResolver = hardwareWalletResolver).also { loginData ->
                        // Mark it as active
                        activeSessions.add(network)

                        // Do a refresh
                        if (network.isElectrum && initializeSession && (isRestore || isSmartDiscovery)) {
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
                        failedNetworkLogins.add(gdkSession.key)
                    }

                    // Add all exceptions
                    exceptions.add(e)

                    null
                }
            }
        } + listOfNotNull(
            if (lightning == null || !supportsLightning()) null else scope.async(
                start = CoroutineStart.LAZY
            ) {

                if (isHardwareWallet && derivedLightningMnemonic == null) {
                    return@async null
                }

                val lightningMnemonic =
                    derivedLightningMnemonic ?: deriveLightningMnemonic(Credentials.fromLoginCredentialsParam(loginCredentialsParams))

                // Init SDK
                initLightningSdk(lightningMnemonic)

                // SmartDiscovery only for SW wallets, on HW ln mnemonic is not available
                if (hasLightning || ((isRestore || (isSmartDiscovery && !isHardwareWallet)) && settingsManager.isLightningEnabled())) {
                    // Make it async to speed up login process
                    val job = scope.async {
                        try {
                            val xPubHashId = if (isLightningShortcut) wallet?.xPubHashId else getWalletIdentifier(
                                network = prominentNetwork,
                                loginCredentialsParams = loginCredentialsParams,
                                hwInteraction = hwInteraction
                            ).xpubHashId

                            // Connect SDK
                            connectToGreenlight(
                                mnemonic = lightningMnemonic,
                                parentXpubHashId = xPubHashId,
                                restoreOnly = isRestore || isSmartDiscovery,
                                quickResponse = isRestore
                            )

                            if (isRestore) {
                                hasLightning = lightningSdk.isConnected
                            }

                            updateAccountsAndBalances(updateBalancesForNetwork = lightning)
                            updateWalletTransactions(updateForNetwork = lightning)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _failedNetworksStateFlow.value += listOfNotNull(lightning)
                        }
                    }

                    // If restore, await for the login to be completed to be able to store credentials
                    if (isRestore) {
                        job.await()
                    }
                }

                null
            })).let { list ->
            list.let { deferred ->
                if (isHardwareWallet) { // On hardware connection in series to avoid any race condition with the hw api
                    deferred.map { it.await() }
                } else {
                    deferred.awaitAll()
                }
            }
                .filterNotNull()
                .let {
                    it.firstOrNull() ?: throw exceptions.first() // Throw if all networks failed
                }.also {
                    _failedNetworksStateFlow.value += failedNetworkLogins
                    onLoginSuccess(
                        loginData = it,
                        wallet = wallet,
                        initAccount = initAccount,
                        initNetwork = initNetwork,
                        initializeSession = initializeSession
                    )
                }
        }
    }

    fun watchOnlyToFullSession(device: GreenDevice, gdkSession: GdkSession) {
        val gaSessionToBeDestroyed = mutableListOf<GASession>()

        gdkSession.gdkSessions.forEach {
            gdkSessions[it.key]?.also { s ->
                gaSessionToBeDestroyed += s
            }

            gdkSessions[it.key] = it.value
        }

        // Destroy gaSession
        gaSessionToBeDestroyed.forEach {
            gdk.destroySession(it)
        }

        isHwWatchOnly = false
        isNoBlobWatchOnly = false
        isRichWatchOnly = false
        isCoreDescriptorWatchOnly = false
        isAirgapped = false
        _isWatchOnly.value = false

        setupDeviceToSession(device)

        updateAccountsAndBalances()
        updateWalletTransactions()
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
        mnemonic: String,
        parentXpubHashId: String? = null,
        restoreOnly: Boolean = true,
        quickResponse: Boolean = false
    ) {
        logger.i { "Login into ${lightning?.id}" }

        countly.loginLightningStart()

        lightningSdk.connectToGreenlight(
            mnemonic = mnemonic,
            parentXpubHashId = parentXpubHashId ?: xPubHashId.takeIf { !isLightningShortcut },
            restoreOnly = restoreOnly,
            quickResponse = quickResponse
        ).also {
            hasLightning = it == true
            if (it == null) {
                _failedNetworksStateFlow.value += listOfNotNull(lightning)
            }
        }

        countly.loginLightningStop()

        lightningSdk.eventSharedFlow.onEach {
            onLightningEvent(it)
        }.launchIn(scope = scope + parentJob)
    }

    private suspend fun reLogin(network: Network): LoginData {
        return authHandler(
            network,
            gdk.loginUser(
                session = gdkSession(network),
                deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device),
                loginCredentialsParams = LoginCredentialsParams.empty
            )
        ).result<LoginData>().also {
            authenticationRequired.remove(network)

            getAccounts(network, refresh = true).forEach {
                getTransactions(account = it, isReset = false, isLoadMore = false)
            }

            updateAccountsAndBalances(updateBalancesForNetwork = network)
            updateWalletTransactions(updateForNetwork = network)
        }
    }

    private suspend fun onLoginSuccess(
        loginData: LoginData,
        wallet: GreenWallet?,
        initNetwork: String?,
        initAccount: Long?,
        initializeSession: Boolean
    ) {
        _isConnectedState.value = true
        xPubHashId = if (isNoBlobWatchOnly && !isHwWatchOnly) loginData.networkHashId else loginData.xpubHashId

        if (initializeSession) {
            countly.activeWalletStart()
            initializeSessionData(wallet, initNetwork, initAccount)
        }

        // Allow initialization calls to have priority over notifications initiated updates (getWalletTransactions & updateAccountAndBalances)
        _disableNotificationHandling = false
    }

    private suspend fun initializeSessionData(wallet: GreenWallet?, initNetwork: String?, initAccount: Long?) {
        // Check if active account index was archived from 1) a different client (multisig) or 2) from cached Singlesig hww session
        // Expect refresh = true to be already called
        updateAccounts(unarchiveFunded = true)

        _activeAccountStateFlow.value = accounts.value.find {
            it.networkId == initNetwork && it.pointer == initAccount && !it.hidden
        } ?: accounts.value.firstOrNull() ?: this.allAccounts.value.firstOrNull()

        // Update Liquid Assets from GDK before getting balances to sort them properly
        updateLiquidAssets()

        // Update the enriched assets
        updateEnrichedAssets()

        // Change wallet balance denomination
        walletTotalBalanceDenominationStateFlow.value =
            if (wallet?.extras?.totalBalanceInFiat == true) Denomination.defaultOrFiat(session = this, isFiat = true) else Denomination.BTC

        if (!isWatchOnlyValue && !isLightningShortcut) {
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
        updateWalletTransactions()
        scanExpired2FA()
    }

    fun updateLiquidAssets() {
        if (liquid != null) {
            networkAssetManager.updateAssetsIfNeeded(this)
        }
    }

    fun updateSystemMessage() {
        scope.launch(logException(countly)) {
            _systemMessageStateFlow.value = gdkSessions.map {
                it.key to (gdk.getSystemMessage(it.value) ?: "")
            }.filter { !it.second.isNullOrBlank() }
        }
    }

    suspend fun ackSystemMessage(network: Network, message: String) = authHandler(
        network,
        gdk.ackSystemMessage(gdkSession(network), message)
    ).resolve()

    fun setTransactionMemo(transaction: Transaction, memo: String) {
        gdk.setTransactionMemo(gdkSession(transaction.account.network), transaction.txHash, memo)

        // update transaction
        getTransactions(account = transaction.account, isReset = false, isLoadMore = false)
        updateWalletTransactions(updateForAccounts = listOf(transaction.account))
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
        @Suppress("NAME_SHADOWING")
        val network = network ?: defaultNetwork

        return authHandler(
            network,
            gdk.encryptWithPin(gdkSession(network), encryptWithPinParams)
        ).result<EncryptWithPin>().also {
            it.networkInjected = (network)
        }
    }

    private suspend fun decryptCredentialsWithPin(network: Network, decryptWithPinParams: DecryptWithPinParams): Credentials {
        return authHandler(
            network,
            gdk.decryptWithPin(gdkSession(network), decryptWithPinParams)
        ).result()
    }

    suspend fun getCredentials(params: CredentialsParams = CredentialsParams()): Credentials {
        val network = defaultNetwork.takeIf { hasActiveNetwork(defaultNetwork) } ?: activeSessions.first()
        return authHandler(network, gdk.getCredentials(gdkSession(network), params)).result()
    }

    private var _derivedHwLightningMnemonic: String? = null
    suspend fun deriveLightningMnemonic(credentials: Credentials? = null): String {
        if (isHardwareWallet && credentials == null) {
            return _derivedHwLightningMnemonic ?: throw Exception("HWW can't derive lightning mnemonic")
        }

        return (credentials ?: getCredentials()).let {
            if (isLightningShortcut || isHardwareWallet) {
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

    suspend fun getReceiveAddress(account: Account) =
        if (account.isLightning) Address(address = lightningSdk.receiveOnchain().bitcoinAddress) else authHandler(
            account.network,
            gdk.getReceiveAddress(
                gdkSession(account.network),
                ReceiveAddressParams(account.pointer)
            )
        ).result<Address>()

    suspend fun getReceiveAddressAsString(account: Account): String = getReceiveAddress(account).address

    // Combine with receive address
    fun receiveOnchain(): SwapInfo {
        return lightningSdk.receiveOnchain()
    }

    fun createLightningInvoice(satoshi: Long, description: String): ReceivePaymentResponse {
        return lightningSdk.createInvoice(satoshi, description)
    }

    suspend fun getPreviousAddresses(account: Account, lastPointer: Int?) = authHandler(
        account.network,
        gdk.getPreviousAddress(
            gdkSession(account.network),
            PreviousAddressParams(account.pointer, lastPointer = lastPointer)
        )
    ).result<PreviousAddresses>()

    override fun refreshAssets(params: AssetsParams) {
        (activeLiquid ?: liquid)?.also { gdk.refreshAssets(gdkSession(it), params) }
    }

    override fun getAssets(params: GetAssetsParams) = (activeLiquid ?: liquid)?.let { gdk.getAssets(gdkSession(it), params) }

    fun setupDefaultAccounts(): Job {
        return scope.launch {
            // Archive default gdk accounts with no history
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

            // Set active account as the first funded account or the first with history
            (accounts.value.find { !it.isLightning && it.isFunded(this@GdkSession) }
                ?: accounts.value.find { !it.isLightning && it.hasHistory(this@GdkSession) })?.also {
                setActiveAccount(it)
            }
        }
    }

    suspend fun createAccount(
        network: Network,
        params: SubAccountParams,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): Account {
        return initNetworkIfNeeded(network, hardwareWalletResolver) {
            authHandler(network, gdk.createSubAccount(gdkSession(network), params))
                .result<Account>(
                    hardwareWalletResolver = hardwareWalletResolver
                ).also {
                    it.setup(this, network)
                }
        }.also {
            _walletActiveEventInvalidated = true

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
        (if (isLightningShortcut) {
            listOf()
        } else {
            activeGdkSessions.map {
                scope.async {
                    getAccounts(it.key, refresh)
                }
            }.awaitAll().flatten()
        }).let {
            if (hasLightning) it + lightningAccount else it
        }.sorted()
    }

    suspend private fun getAccounts(network: Network, refresh: Boolean = false): List<Account> = initNetworkIfNeeded(network) {
        gdkSession(network).let {
            // Watch-only can't discover new accounts
            authHandler(network, gdk.getSubAccounts(it, SubAccountsParams(refresh = if (isNoBlobWatchOnly) false else refresh)))
                .result<Accounts>().accounts.onEach { account ->
                    account.setup(this, network)
                }
        }
    }

    suspend fun getAccount(account: Account) = authHandler(
        account.network, gdk.getSubAccount(gdkSession(account.network), account.pointer)
    ).result<Account>().also {
        it.setup(this, account.network)

        // Update active account if needed
        if (_activeAccountStateFlow.value?.id == it.id) {
            _activeAccountStateFlow.value = it
        }
    }

    fun setActiveAccount(account: Account) {
        // Get the account from the list of accounts just to be sure we have the latest account data
        _activeAccountStateFlow.value = accounts.value.find { it.id == account.id } ?: account
    }

    suspend fun removeAccount(account: Account) {
        if (account.isLightning) {
            hasLightning = false

            lightningSdk.stop()

            // Update accounts
            updateAccounts()

            updateAccountsAndBalances()
            updateWalletTransactions()
        }
    }

    suspend fun updateAccount(
        account: Account,
        isHidden: Boolean,
        userInitiated: Boolean = false,
        resetAccountName: String? = null
    ): Account {
        // Disable account editing for lightning accounts
        if (account.isLightning) return account

        authHandler(
            account.network,
            gdk.updateSubAccount(
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
        ).resolve()
        // Update account list
        updateAccounts()

        if (userInitiated && isHidden) {
            _eventsSharedFlow.emit(WalletEvents.ARCHIVED_ACCOUNT)
        }

        return getAccount(account).also {
            listOf(it).also {
                // Update newly created account
                updateAccountsAndBalances(updateBalancesForAccounts = it)

                // Update wallet transactions
                updateWalletTransactions(updateForAccounts = it)
            }
        }
    }

    suspend fun updateAccount(account: Account, name: String): Account {
        authHandler(
            account.network,
            gdk.updateSubAccount(
                gdkSession(account.network), UpdateSubAccountParams(
                    name = name,
                    subaccount = account.pointer
                )
            )
        ).resolve()

        updateAccounts()

        return getAccount(account).also {
            // Update wallet transactions so that the tx have the new injected account
            updateWalletTransactions()
        }
    }

    suspend fun getFeeEstimates(network: Network): FeeEstimation = tryCatch(context = Dispatchers.Default) {
        if (network.isLightning) {
            lightningSdk.recommendedFees()?.let { fees ->
                (
                        listOf(fees.minimumFee) +
                                List(FeeBlockTarget[0] - 0) { fees.fastestFee } +
                                List(FeeBlockTarget[1] - FeeBlockTarget[0]) { fees.halfHourFee } +
                                List(FeeBlockTarget[2] - FeeBlockTarget[1]) { fees.hourFee }
                        ).map { it.toLong() }.let {
                        FeeEstimation(it)
                    }
            }
        } else {
            gdk.getFeeEstimates(gdkSession(network)).let {
                // Temp fix
                if (network.isSinglesig && network.isLiquid) {
                    FeeEstimation(it.fees.map { fee ->
                        fee.coerceAtLeast(100L)
                    })
                } else {
                    it
                }
            }.also {
                logger.d { "FeeEstimation: ${network.id} $it" }
            }
        }
    } ?: FeeEstimation(fees = mutableListOf(network.defaultFee))

    suspend fun getTransactions(account: Account, params: TransactionParams = TransactionParams(subaccount = 0)) =
        (if (account.network.isLightning) {
            getLightningTransactions()
        } else {
            authHandler(account.network, gdk.getTransactions(gdkSession(account.network), params))
                .result<Transactions>()
        }).also {
            it.transactions.onEach { tx ->
                tx.accountInjected = account
                tx.confirmationsMaxInjected = tx.getConfirmationsMax(session = this)
            }
        }

    private val refreshMutex = Mutex()
    fun refresh(account: Account? = null) {
        scope.launch(context = logException(countly)) {
            refreshMutex.withLock {
                if (account == null || account.isLightning) {
                    syncLightning()
                }

                if (account == null) {
                    updateAccountsAndBalances(refresh = true)
                    updateWalletTransactions()
                } else {
                    getTransactions(account = account, isReset = false, isLoadMore = false)
                    updateAccountsAndBalances(refresh = true, updateBalancesForAccounts = listOf(account))
                }

                updateLiquidAssets()
            }
        }
    }

    private fun syncLightning() {
        lightningSdkOrNull?.sync()
    }

    private fun getLightningTransactions() = lightningSdkOrNull?.getTransactions()?.map {
        Transaction.fromPayment(it)
    }.let {
        Transactions(transactions = it ?: listOf())
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
                    updateAccounts(refresh = refresh, unarchiveFunded = true)

                    for (account in this@GdkSession.allAccounts.value) {
                        if ((updateBalancesForAccounts == null && updateBalancesForNetwork == null) || updateBalancesForAccounts?.find { account.id == it.id } != null || account.network == updateBalancesForNetwork) {
                            getBalance(account = account, cacheAssets = isInitialize).also {
                                accountAssetsStateFlow(account).value = it
                            }
                        }
                    }

                    // Wallet Assets
                    val walletAssets = linkedMapOf<String, Long>()

                    accounts.value.filter { it.hasHistory(this@GdkSession) }.forEach { account ->
                        this@GdkSession.accountAssets(account).value.assets.forEach { (key, value) ->
                            walletAssets[key] = (walletAssets[key] ?: 0) + value
                        }
                    }

                    if (isInitialize) {
                        // Cache wallet assets (again) + Enriched assets + liquid asset if network exists
                        (walletAssets.keys +
                                (enrichedAssets.value.takeIf { liquid != null }?.map { it.assetId } ?: emptyList()) +
                                listOfNotNull(liquid?.policyAsset))
                            .toSet().also {
                                cacheAssets(it)
                            }
                    }

                    // Mark it if necessary
                    if (!walletHasHistory) {
                        if (walletAssets.size > 2 || walletAssets.values.sum() > 0L) {
                            _walletHasHistorySharedFlow.value = true
                        }
                    }

                    walletAssets.toSortedLinkedHashMap(::sortAssets).also {
                        _walletAssetsFlow.value = DataState.Success(Assets(it))
                    }

                    val accountAndAssets = accounts.value.flatMap {
                        this@GdkSession.accountAssets(it).value.toAccountAsset(it, this@GdkSession)
                    }

                    _accountAssetStateFlow.value = accountAndAssets.sortedWith(::sortAccountAssets)

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

    private val transactionsMutex = Mutex()
    fun getTransactions(account: Account, isReset: Boolean, isLoadMore: Boolean) {
        scope.launch(context = logException(countly)) {
            val transactionsPagerStateFlow = accountTransactionsPagerStateFlow(account)
            val transactionsStateFlow = accountTransactionsStateFlow(account)

            try {
                transactionsPagerStateFlow.value = false

                transactionsMutex.withLock {
                    val txSize = transactionsStateFlow.value.let {
                        it.data()?.size ?: 0
                    }

                    var offset = 0

                    if (isReset) {
                        // transactionsStateFlow.value = listOf(Transaction.LoadingTransaction)
                        offset = 0
                    } else if (isLoadMore) {
                        offset = txSize
                    }

                    val limit = if (isReset || isLoadMore) TRANSACTIONS_PER_PAGE else (txSize + TRANSACTIONS_PER_PAGE)

                    val transactions = getTransactions(
                        account,
                        TransactionParams(subaccount = account.pointer, offset = offset, limit = limit)
                    ).transactions

                    // Update transactions
                    transactionsStateFlow.value = DataState.Success(
                        if (isLoadMore) {
                            (transactionsStateFlow.value.data() ?: listOf()) + transactions
                        } else {
                            transactions
                        }
                    )

                    // Update pager
                    if (isReset || isLoadMore) {
                        transactionsPagerStateFlow.value = if (account.isLightning) false else transactions.size == TRANSACTIONS_PER_PAGE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                // Re-set the list to unblock endless loader
                transactionsPagerStateFlow.value = false
            }
        }
    }

    private val walletTransactionsMutex = Mutex()
    private val _walletTransactions = mutableMapOf<AccountId, List<Transaction>>()
    fun updateWalletTransactions(updateForNetwork: Network? = null, updateForAccounts: Collection<Account>? = null) {
        scope.launch(context = logException(countly)) {
            try {
                walletTransactionsMutex.withLock {
                    // Clear walletTransactions to avoid keeping archived accounts
                    if (updateForAccounts == null && updateForNetwork == null) {
                        _walletTransactions.clear()
                    }

                    allAccounts.value
                        .filter { account ->
                            ((updateForNetwork == null && updateForAccounts == null) || updateForAccounts?.find { account.id == it.id } != null || account.network == updateForNetwork)
                        }
                        .onEach { account ->
                            if (account.hidden) {
                                // Clear transactions
                                _walletTransactions.remove(account.id)
                            } else {
                                _walletTransactions[account.id] = getTransactions(
                                    account,
                                    TransactionParams(subaccount = account.pointer, limit = WALLET_OVERVIEW_TRANSACTIONS)
                                ).transactions
                            }
                        }

                    var walletTransactions = _walletTransactions.values.flatten()

                    walletTransactions = walletTransactions.sortedWith(::sortTransactions).let {
                        it.subList(0, it.size.coerceAtMost(WALLET_OVERVIEW_TRANSACTIONS))
                    }

                    // Add Swap transactions
                    if (hasLightning) {
                        walletTransactions = lightningSdk.swapInfoStateFlow.value.map {
                            Transaction.fromSwapInfo(lightningAccount, it.first, it.second)
                        } + lightningSdk.reverseSwapInfoStateFlow.value.map {
                            Transaction.fromReverseSwapInfo(lightningAccount, it)
                        } + walletTransactions
                    }

                    if (walletTransactions.isNotEmpty()) {
                        _walletHasHistorySharedFlow.value = true
                    }
                    _walletTransactionsStateFlow.value = DataState.Success(walletTransactions)
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
            t1.blockHeight == t2.blockHeight && t1.createdAtTs == t2.createdAtTs -> { // if we send to the same account, display first the outgoing tx
                if (t1.isIn && t2.isOut) {
                    -1
                } else if (t2.isIn && t1.isOut) {
                    1
                } else {
                    t2.createdAtTs.compareTo(t1.createdAtTs)
                }
            }

            else -> t2.createdAtTs.compareTo(t1.createdAtTs)
        }

    fun sortUtxos(a1: Utxo, a2: Utxo): Int = sortAssets(a1.assetId, a2.assetId)

    private fun sortAssets(a1: String, a2: String): Int {

        return when {
            a1 == a2 -> 0
            a1 == BTC_POLICY_ASSET -> -1
            a2 == BTC_POLICY_ASSET -> 1
            a1 == LN_BTC_POLICY_ASSET -> -1
            a2 == LN_BTC_POLICY_ASSET -> 1
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
                    val weight1 = getEnrichedAssets(a1)?.weight ?: 0
                    val weight2 = getEnrichedAssets(a2)?.weight ?: 0

                    if (weight1 == weight2) {
                        asset1.name.compareTo(asset2.name)
                    } else {
                        weight2.compareTo(weight1)
                    }
                } else {
                    a1.compareTo(a2)
                }
            }
        }
    }

    fun sortEnrichedAssets(a1: EnrichedAsset, a2: EnrichedAsset): Int {
        val w1 = a1.sortWeight(this)
        val w2 = a2.sortWeight(this)

        if (w1 != w2) {
            return w2.compareTo(w1)
        }

        return a1.name(this).fallbackString().compareTo(a2.name(this).fallbackString())
    }

    private fun sortAccountAssets(a1: AccountAsset, a2: AccountAsset): Int = when {
        a1.account.isBitcoin && a2.account.isLiquid -> -1
        a1.account.isLiquid && a2.account.isBitcoin -> 1
        a1.asset.assetId == a2.asset.assetId -> a1.account.compareTo(a2.account)
        else -> {
            sortAssets(a1.asset.assetId, a2.asset.assetId)
        }
    }

    suspend fun getBalance(account: Account, confirmations: Int = 0, cacheAssets: Boolean = false): Assets {
        val assets: LinkedHashMap<String, Long>? = if (account.isLightning) {
            lightningSdkOrNull?.balance()?.let { linkedMapOf(LN_BTC_POLICY_ASSET to it) }
        } else {
            authHandler(
                account.network, gdk.getBalance(
                    gdkSession(account.network), BalanceParams(
                        subaccount = account.pointer,
                        confirmations = confirmations
                    )
                )
            ).result()
        }

        if (cacheAssets) {
            // Cache assets before sorting them, as the sort function uses the asset metadata
            assets?.also { cacheAssets(it.keys) }
        }

        return Assets(assets?.toSortedLinkedHashMap(::sortAssets))
    }

    suspend fun changeSettingsTwoFactor(
        network: Network,
        method: String,
        methodConfig: TwoFactorMethodConfig,
        twoFactorResolver: TwoFactorResolver
    ) {
        authHandler(
            network, gdk.changeSettingsTwoFactor(gdkSession(network), method, methodConfig)
        ).resolve(twoFactorResolver = twoFactorResolver).also {
            updateTwoFactorConfig(network)
        }
    }

    private fun getWatchOnlyUsername(network: Network) = gdk.getWatchOnlyUsername(gdkSession(network))

    suspend fun setWatchOnly(
        network: Network,
        username: String,
        password: String
    ): LoginData {
        logger.d { "setWatchOnly: ${network.id} user: '$username' pass: '$password'" }

        return authHandler(
            network,
            gdk.registerUser(
                session = gdkSession(network),
                deviceParams = DeviceParams.Empty,
                loginCredentialsParams = LoginCredentialsParams(
                    username = username,
                    password = password
                )
            )
        ).result<LoginData>().also {
            updateWatchOnlyUsername(network)
        }
    }

    suspend private fun createWatchOnly(
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
        return rwo + (activeSessions.filter { network ->
            !network.isLightning && rwo.find { it.network == network.id } == null
        }.let {
            createWatchOnly(it, randomChars(16), randomChars(32))
        })
    }

    suspend fun deleteWatchOnly(network: Network) {
        setWatchOnly(network = network, username = "", password = "")
    }

    fun watchOnlyUsername(network: Network): StateFlow<String?> {
        return (if (network.isBitcoin) {
            _multisigBitcoinWatchOnly
        } else {
            _multisigLiquidWatchOnly
        }).asStateFlow()
    }

    private fun updateWatchOnlyUsername(network: Network? = null) {
        if (isWatchOnlyValue) return

        (network?.let { listOfNotNull(network) } ?: activeMultisig).filter {
            walletExistsAndIsUnlocked(it)
        }.onEach {
            try {
                val username = getWatchOnlyUsername(it)
                if (it.isBitcoin) {
                    _multisigBitcoinWatchOnly.value = username
                } else {
                    _multisigLiquidWatchOnly.value = username
                }
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
        return authHandler(
            network,
            gdk.twoFactorReset(gdkSession(network), email, isDispute)
        ).result<TwoFactorReset>(twoFactorResolver = twoFactorResolver).also {
            // Should we disconnect
            // disconnectAsync(LogoutReason.USER_ACTION)
            updateSettings(network)
        }
    }

    suspend fun twoFactorUndoReset(network: Network, email: String, twoFactorResolver: TwoFactorResolver) {
        logger.d { "TwoFactorUndoReset ${network.id} email:$email" }
        authHandler(network, gdk.twoFactorUndoReset(gdkSession(network), email)).resolve(
            twoFactorResolver = twoFactorResolver
        ).also {
            // Should we disconnect
            // disconnectAsync(LogoutReason.USER_ACTION)
            updateSettings(network)
        }
    }

    suspend fun twoFactorCancelReset(network: Network, twoFactorResolver: TwoFactorResolver) {
        logger.d { "TwoFactorCancelReset ${network.id}" }
        authHandler(network, gdk.twoFactorCancelReset(gdkSession(network))).resolve(
            twoFactorResolver = twoFactorResolver
        ).also {
            // Should we disconnect
            // disconnectAsync(LogoutReason.USER_ACTION)
            updateSettings(network)
        }
    }

    suspend fun twoFactorChangeLimits(network: Network, limits: Limits, twoFactorResolver: TwoFactorResolver): Limits {
        return authHandler(network, gdk.twoFactorChangeLimits(gdkSession(network), limits)).result<Limits>(
            twoFactorResolver = twoFactorResolver
        ).also {
            updateTwoFactorConfig(network)
        }
    }

    fun sendNlocktimes(network: Network) = gdk.sendNlocktimes(gdkSession(network))

    suspend fun setCsvTime(network: Network, value: CsvParams, twoFactorResolver: TwoFactorResolver) {
        authHandler(
            network,
            gdk.setCsvTime(gdkSession(network), value)
        ).resolve(twoFactorResolver = twoFactorResolver).also {
            updateSettings(network)
        }
    }

    private suspend fun updateAccounts(refresh: Boolean = false, unarchiveFunded: Boolean = false) {
        getAccounts(refresh).also { fetchedAccounts ->
            _allAccountsStateFlow.value = fetchedAccounts

            fetchedAccounts.filter { !it.hidden }.also {
                _accountsStateFlow.value = it
                _zeroAccounts.value = it.isEmpty() && failedNetworks.value.isEmpty()
            }

            if (unarchiveFunded) {
                // Unarchive accounts if they are hidden and funded
                fetchedAccounts.filter { it.hidden && it.isFunded(this) }.forEach {
                    updateAccount(it, isHidden = false)
                }
            }

            // Update active account to get fresh data, also prevent it from being archived and active
            _activeAccountStateFlow.value = accounts.value.find {
                activeAccount.value?.id == it.id
            } ?: accounts.value.firstOrNull() ?: this.allAccounts.value.firstOrNull()
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

        val network = assetId.networkForAsset(this@GdkSession)?.takeIf { !it.isLightning } ?: defaultNetwork
        val isPolicyAsset = assetId.isPolicyAsset(this@GdkSession)
        val asset = assetId?.let { getAsset(it) }

        val convert = if (isPolicyAsset || assetId == null || asString == null) {
            Convert.create(
                isPolicyAsset = isPolicyAsset,
                asset = asset,
                asString = asString,
                asLong = asLong,
                unit = denomination ?: BTC_UNIT
            ).toJsonElement()
        } else if (asset != null) {
            buildJsonObject {
                put("asset_info", asset.toJsonElement())
                put(assetId, asString)
            }
        } else {
            return@withContext Balance.fromAssetWithoutMetadata(asLong ?: 0)
        }

        val balance = try {
            Balance.fromJsonElement(
                jsonElement = gdk.convertAmount(gdkSession(network), convert),
                assetId = assetId
            )
        } catch (e: Exception) {
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

    private suspend fun getUnspentOutputs(network: Network, params: BalanceParams) = authHandler(
        network,
        gdk.getUnspentOutputs(gdkSession(network), params)
    ).result<UnspentOutputs>()

    private suspend fun getUnspentOutputsForPrivateKey(network: Network, params: UnspentOutputsPrivateKeyParams) = authHandler(
        network,
        gdk.getUnspentOutputsForPrivateKey(gdkSession(network), params)
    ).result<UnspentOutputs>()

    suspend fun getUnspentOutputs(
        account: Account,
        isBump: Boolean = false,
        isExpired: Boolean = false
    ): UnspentOutputs {
        return getUnspentOutputs(
            network = account.network,
            params = if (isExpired) {
                BalanceParams(
                    subaccount = account.pointer,
                    confirmations = 1,
                    expiredAt = block(account.network).value.height
                )
            } else {
                BalanceParams(
                    subaccount = account.pointer,
                    confirmations = if (isBump) 1 else 0
                )
            }
        )
    }

    suspend fun getUnspentOutputs(network: Network, privateKey: String): UnspentOutputs {
        return getUnspentOutputsForPrivateKey(
            network, UnspentOutputsPrivateKeyParams(
                privateKey = privateKey
            )
        )
    }

    @NativeCoroutinesIgnore
    suspend fun createTransaction(network: Network, params: CreateTransactionParams) =
        if (network.isLightning) {
            createLightningTransaction(network, params).also {
                logger.d { "createLightningTransaction $it" }
            }
        } else authHandler(
            network,
            gdk.createTransaction(gdkSession(network), params)
        ).result<CreateTransaction>()

    @NativeCoroutinesIgnore
    suspend fun createRedepositTransaction(network: Network, params: CreateTransactionParams) =
        authHandler(
            network,
            gdk.createRedepositTransaction(gdkSession(network), params)
        ).result<CreateTransaction>()

    private suspend fun generateLightningError(
        account: Account,
        satoshi: Long?,
        min: Long? = null,
        max: Long? = null
    ): String? {
        val balance = this.accountAssets(account = account).value.policyAsset

        return if (satoshi == null || satoshi < 0L) {
            "id_invalid_amount"
        } else if (min != null && satoshi < min) {
            "id_amount_must_be_at_least_s|${min.toAmountLook(session = this)}"
        } else if (satoshi > balance) {
            "id_insufficient_funds"
        } else if (max != null && satoshi > max) {
            "id_amount_must_be_at_most_s|${max.toAmountLook(session = this)}"
        } else {
            null
        }
    }

    private suspend fun createLightningTransaction(network: Network, params: CreateTransactionParams): CreateTransaction {
        logger.d { "createLightningTransaction $params" }

        val address = params.addresseesAsParams?.firstOrNull()?.address ?: ""
        val userInputSatoshi = params.addresseesAsParams?.firstOrNull()?.satoshi

        return when (val lightningInputType = lightningSdk.parseBoltOrLNUrlAndCache(address)) {
            is InputType.Bolt11 -> {
                val invoice = lightningInputType.invoice

                logger.d { "Expire in ${invoice.expireIn()}" }

                var sendableSatoshi = invoice.sendableSatoshi(userInputSatoshi)

                var error = generateLightningError(account = lightningAccount, satoshi = sendableSatoshi)

                // Check expiration
                if (invoice.isExpired()) {
                    error = "id_invoice_expired"
                }

                if (sendableSatoshi == null || sendableSatoshi == 0L) {
                    error = "id_invalid_amount"
                }

                // Make it not null
                sendableSatoshi = sendableSatoshi ?: 0L

                CreateTransaction(
                    addressees = listOf(Addressee.fromInvoice(invoice, sendableSatoshi)),
                    satoshi = mapOf(network.policyAsset to (sendableSatoshi)),
                    outputs = listOf(Output.fromInvoice(invoice, sendableSatoshi)),
                    memo = invoice.description,
                    isLightning = true,
                    error = error
                )
            }

            is InputType.LnUrlPay -> {

                val requestData = lightningInputType.data

                var sendableSatoshi = requestData.sendableSatoshi(userInputSatoshi)

                val error = generateLightningError(
                    account = lightningAccount,
                    satoshi = sendableSatoshi,
                    min = requestData.minSendableSatoshi(),
                    max = requestData.maxSendableSatoshi()
                )

                // Make it not null
                sendableSatoshi = sendableSatoshi ?: 0L

                CreateTransaction(
                    addressees = listOf(Addressee.fromLnUrlPay(requestData, address, sendableSatoshi)),
                    outputs = listOf(Output.fromLnUrlPay(requestData, address, sendableSatoshi)),
                    satoshi = mapOf(network.policyAsset to sendableSatoshi),
                    isLightning = true,
                    isLightningDescriptionEditable = true,
                    error = error
                )
            }

            else -> {
                CreateTransaction(error = "id_invalid_address", isLightning = true)
            }
        }
    }

    suspend fun createSwapTransaction(network: Network, params: CreateSwapParams, twoFactorResolver: TwoFactorResolver) = authHandler(
        network,
        gdk.createSwapTransaction(gdkSession(network), params)
    ).result<CreateSwapTransaction>(twoFactorResolver = twoFactorResolver)

    suspend fun completeSwapTransaction(network: Network, params: CompleteSwapParams, twoFactorResolver: TwoFactorResolver) = authHandler(
        network,
        gdk.completeSwapTransaction(gdkSession(network), params)
    ).result<CreateTransaction>(twoFactorResolver = twoFactorResolver)

    suspend fun rsaVerify(params: RsaVerifyParams): RsaVerify {
        // TODO clean it
        if (!isNetworkInitialized) {
            prepareHttpRequest()
        }

        return authHandler(
            defaultNetwork,
            gdk.rsaVerify(gdkSession(defaultNetwork), params)
        ).result<RsaVerify>()
    }

    suspend fun signMessage(
        network: Network,
        params: SignMessageParams,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): SignMessage = authHandler(
        network,
        gdk.signMessage(gdkSession(network), params = params)
    ).result(hardwareWalletResolver = hardwareWalletResolver)

    suspend fun blindTransaction(network: Network, createTransaction: CreateTransaction) =
        authHandler(
            network,
            gdk.blindTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>()

    suspend fun signTransaction(network: Network, createTransaction: CreateTransaction): CreateTransaction = if (network.isLightning) {
        createTransaction // no need to sign on gdk side
    } else {
        authHandler(
            network,
            gdk.signTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>()
    }

    suspend fun psbtFromJson(network: Network, transaction: CreateTransaction): Psbt =
        authHandler(
            network,
            gdk.psbtFromJson(gdkSession(network), transaction = transaction.jsonElement!!)
        ).result<Psbt>()

    suspend fun broadcastTransaction(
        network: Network,
        broadcastTransaction: BroadcastTransactionParams
    ): ProcessedTransactionDetails =
        authHandler(
            network, gdk.broadcastTransaction(
                session = gdkSession(network),
                broadcastTransactionParams = broadcastTransaction
            )
        ).result<ProcessedTransactionDetails>().also {
            _walletActiveEventInvalidated = true
        }

    fun sendLightningTransaction(params: CreateTransaction, comment: String?): ProcessedTransactionDetails {
        val invoiceOrLnUrl = params.addressees.first().address
        val satoshi = params.addressees.first().satoshi?.absoluteValue ?: 0L

        _walletActiveEventInvalidated = true

        return when (val inputType = lightningSdk.parseBoltOrLNUrlAndCache(invoiceOrLnUrl)) {
            is InputType.Bolt11 -> {
                // Check for expiration
                if (inputType.invoice.isExpired()) {
                    throw Exception("id_invoice_expired")
                }

                logger.d { "Sending invoice ${inputType.invoice.bolt11}" }

                try {
                    val response = lightningSdk.sendPayment(
                        invoice = inputType.invoice,
                        satoshi = satoshi.takeIf { inputType.invoice.amountMsat == null }
                    )

                    ProcessedTransactionDetails(paymentId = response.payment.id)
                } catch (e: Exception) {
                    throw ExceptionWithSupportData(
                        throwable = e,
                        supportData = SupportData.create(
                            throwable = e,
                            paymentHash = inputType.invoice.paymentHash,
                            network = lightningAccount.network,
                            session = this
                        )
                    )
                }
            }

            is InputType.LnUrlPay -> {
                lightningSdk.payLnUrl(
                    requestData = inputType.data,
                    amount = satoshi,
                    comment = comment ?: ""
                ).let {
                    when (it) {
                        is LnUrlPayResult.EndpointSuccess -> {
                            ProcessedTransactionDetails.create(it.data)
                        }

                        is LnUrlPayResult.EndpointError -> {
                            throw Exception(it.data.reason)
                        }

                        is LnUrlPayResult.PayError -> {
                            val exception = Exception(it.data.reason)
                            throw ExceptionWithSupportData(
                                throwable = exception,
                                supportData = SupportData.create(
                                    throwable = exception,
                                    paymentHash = it.data.paymentHash,
                                    network = lightningAccount.network,
                                    session = this
                                )
                            )
                        }
                    }
                }
            }

            else -> {
                throw Exception("id_invalid")
            }
        }
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
        authHandler(
            account.network,
            gdk.sendTransaction(gdkSession(account.network), transaction = signedTransaction)
        ).result<ProcessedTransactionDetails>(twoFactorResolver = twoFactorResolver).also {
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

    private fun accountEmptiedEventIfNeeded() {
        _accountEmptiedEvent?.also { account ->
            countly.accountEmptied(
                session = this@GdkSession,
                walletHasFunds = walletAssets.value.data()?.hasFunds == true,
                accountsFunded = _accountAssetsFlow.values.map { it.value.hasFunds }
                    .count { it },
                accounts = this@GdkSession.accounts.value,
                account = account
            )
            _accountEmptiedEvent = null
        }
    }

    fun emptyLightningAccount() {
        _accountEmptiedEvent = _lightningAccount
    }

    private fun walletActiveEventIfNeeded() {
        if (_walletActiveEventInvalidated) {
            countly.activeWalletEnd(
                session = this,
                walletHasFunds = walletAssets.value.data()?.hasFunds == true,
                accountsFunded = _accountAssetsFlow.values.map { it.value.hasFunds }.count { it },
                accounts = this.accounts.value
            )
            _walletActiveEventInvalidated = false
        }
    }

    private fun onLightningEvent(event: BreezEvent) {
        when (event) {
            is BreezEvent.Synced -> {
                getTransactions(account = lightningAccount, isReset = false, isLoadMore = false)
                updateAccountsAndBalances(updateBalancesForAccounts = listOf(lightningAccount))
                updateWalletTransactions(updateForAccounts = listOf(lightningAccount))
            }

            is BreezEvent.NewBlock -> {
                lightning?.also { blockStateFlow(it).value = Block(height = event.block.toLong()) }
            }

            is BreezEvent.InvoicePaid -> {
                _lastInvoicePaid.value = event.details
            }

            else -> {}
        }
    }

    private fun scanExpired2FA() {
        if (!isWatchOnlyValue) {
            scope.launch(context = logException(countly)) {
                _expired2FAStateFlow.value = accounts.value.filter {
                    it.type == AccountType.STANDARD && getUnspentOutputs(
                        account = it,
                        isExpired = true
                    ).unspentOutputs.isNotEmpty()
                }
            }
        }
    }

    fun onNewNotification(gaSession: GASession, notification: Notification) {

        val network = gdkSessions.firstNotNullOfOrNull { if (it.value == gaSession) it.key else null } ?: return

        logger.d { "onNewNotification ${network.id} \t $notification" }

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    // SingleSig after connect immediately sends a block with height 0
                    // it's not safe to call getTransactions so early
                    if (it.height > 0) {
                        blockStateFlow(network).value = it

                        if (!_disableNotificationHandling) {
                            // Update transactions
                            accounts.value.filter { it.network == network }.also { accounts ->
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

                            if (network.isMultisig && !network.needs2faActivation(this)) {
                                scanExpired2FA()
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
                if (network == defaultNetwork) {
                    notification.tor?.let {
                        _torStatusSharedFlow.value = it
                    }
                }
            }

            "network" -> {
                notification.network?.let { event ->
                    if (isConnected) {
                        if (event.isConnected && authenticationRequired[network] == true) {
                            scope.launch(context = logException(countly)) {
                                reLogin(network)
                            }
                        } else if (!event.isConnected) {
                            // mark re-authentication is required
                            authenticationRequired[network] = true
                        }
                    }

                    networkEventsStateFlow(network).value = event

                    // Personal Electrum Server Error
                    if (network.isSinglesig && !event.isConnected && settingsManager.appSettings.getPersonalElectrumServer(network)
                            .isNotBlank()
                    ) {
                        scope.launch {
                            _networkErrors.send(network to event)
                        }
                    }
                }
            }

            "ticker" -> {
                // Update UI maybe
                _tickerSharedFlow.tryEmit(Unit)
            }

            "subaccount" -> {
                if (!_disableNotificationHandling && notification.subaccount?.isSynced == true) {
                    updateAccountsAndBalances()
                    updateWalletTransactions()
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
                            accounts.forEach {
                                // Update account transactions
                                getTransactions(account = it, isReset = false, isLoadMore = false)
                            }

                            updateAccountsAndBalances(updateBalancesForAccounts = accounts)

                            // Update wallet transactions
                            updateWalletTransactions(updateForAccounts = accounts)
                        }

                        if (network.isMultisig && !network.needs2faActivation(this)) {
                            scanExpired2FA()
                        }
                    }
                }
            }
        }
    }

    private fun cacheAssets(assetIds: Collection<String>) {
        assetIds.filter { it != BTC_POLICY_ASSET && it != LN_BTC_POLICY_ASSET }.takeIf { it.isNotEmpty() }?.also {
            logger.d { "Cache assets: $it" }
            networkAssetManager.cacheAssets(it, this)
        }
    }

    fun hasAssetIcon(assetId: String) = networkAssetManager.hasAssetIcon(assetId)
    fun getAsset(assetId: String): Asset? = networkAssetManager.getAsset(assetId, this)

    private suspend fun validateAddress(network: Network, params: ValidateAddresseesParams) = authHandler(
        network,
        gdk.validate(gdkSession(network), params)
    ).result<ValidateAddressees>()

    private fun createEcPrivateKey(): ByteArray {
        var privateKey: ByteArray
        do {
            privateKey = gdk.getRandomBytes(wally.ecPrivateKeyLen)
        } while (!wally.ecPrivateKeyVerify(privateKey))

        return privateKey
    }

    @NativeCoroutinesIgnore
    suspend fun jadePsbtRequest(psbt: String): BcurEncodedData {

        val params = BcurEncodeParams(
            urType = "crypto-psbt",
            data = psbt
        )

        return bcurEncode(params)
    }

    @NativeCoroutinesIgnore
    suspend fun jadePinRequest(payload: String): BcurEncodedData {

        val params = BcurEncodeParams(
            urType = "jade-pin",
            data = payload
        )

        return bcurEncode(params)
    }

    @NativeCoroutinesIgnore
    suspend fun jadeBip8539Request(): Pair<ByteArray, BcurEncodedData> {
        val privateKey = createEcPrivateKey()

        val params = BcurEncodeParams(
            urType = "jade-bip8539-request",
            numWords = 12,
            index = 0,
            privateKey = privateKey.toHex()
        )

        return privateKey to bcurEncode(params)
    }

    fun jadeBip8539Reply(privateKey: ByteArray, publicKey: ByteArray, encrypted: ByteArray): String? {
        return wally.bip85FromJade(privateKey, publicKey, "bip85_bip39_entropy", encrypted)
    }

    @NativeCoroutinesIgnore
    suspend fun bcurEncode(params: BcurEncodeParams): BcurEncodedData {
        val network = defaultNetworkOrNull ?: networks.bitcoinElectrum

        if (!isConnected) {
            connect(network = network, initNetworks = listOf(network))
        }

        return authHandler(network, gdk.bcurEncode(gdkSession(network), params)).result<BcurEncodedData>()
    }

    suspend fun bcurDecode(params: BcurDecodeParams, bcurResolver: BcurResolver): BcurDecodedData {
        val network = defaultNetworkOrNull ?: networks.bitcoinElectrum

        if (!isConnected) {
            connect(network = network, initNetworks = listOf(network))
        }

        return authHandler(network, gdk.bcurDecode(gdkSession(network), params)).result<BcurDecodedData>(bcurResolver = bcurResolver)
    }

    suspend fun parseInput(input: String): Pair<Network, InputType?>? =
        withContext(context = Dispatchers.Default) {
            lightning?.let { lightning ->
                lightningSdkOrNull?.parseBoltOrLNUrlAndCache(input)?.let { lightning to it }
            } ?: run {
                activeGdkSessions.mapValues {
                    validateAddress(it.key, ValidateAddresseesParams.create(it.key, input)).also {
                        logger.d { "$it" }
                    }
                }.filter { it.value.isValid }.keys.firstOrNull()?.let { it to null }
            }
        }

    fun supportId() = allAccounts.value.filter {
        it.isMultisig && it.pointer == 0L || it.isLightning
    }.joinToString(",") { "${it.network.bip21Prefix}:${if (it.isLightning) lightningSdk.nodeInfoStateFlow.value.id else it.receivingId}" }

    internal fun destroy(disconnect: Boolean = true) {
        if (disconnect) {
            disconnectAsync()
        }
        scope.cancel("Destroy")
    }

    companion object : Loggable() {
        const val WALLET_OVERVIEW_TRANSACTIONS = 50

        const val LIQUID_ASSETS_KEY = "liquid_assets"
        const val LIQUID_ASSETS_TESTNET_KEY = "liquid_assets_testnet"

        val BlockstreamWhitelistedUrls = listOf(
            "https://j8d.io",
            "https://jadepin.blockstream.com",
            "http://mrrxtq6tjpbnbm7vh5jt6mpjctn7ggyfy5wegvbeff3x7jrznqawlmid.onion", // onion jadepin
            "https://jadefw.blockstream.com",
            "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion" // onion jadefw
        )
    }
}

