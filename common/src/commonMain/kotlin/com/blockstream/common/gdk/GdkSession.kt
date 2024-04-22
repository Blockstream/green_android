package com.blockstream.common.gdk

import breez_sdk.BreezEvent
import breez_sdk.InputType
import breez_sdk.InvoicePaidDetails
import breez_sdk.LnUrlPayResult
import breez_sdk.ReceivePaymentResponse
import breez_sdk.SwapInfo
import co.touchlab.kermit.Logger
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.CountlyBase
import com.blockstream.common.data.CountlyAsset
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.ExceptionWithErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.extensions.title
import com.blockstream.common.extensions.toSortedLinkedHashMap
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
import com.blockstream.common.gdk.data.PreviousAddresses
import com.blockstream.common.gdk.data.SendTransactionSuccess
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
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.gdk.device.DeviceState
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.BalanceParams
import com.blockstream.common.gdk.params.BcurDecodeParams
import com.blockstream.common.gdk.params.BcurEncodeParams
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
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.gdk.params.SubAccountsParams
import com.blockstream.common.gdk.params.TransactionParams
import com.blockstream.common.gdk.params.TransactionParams.Companion.TRANSACTIONS_PER_PAGE
import com.blockstream.common.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.common.gdk.params.UpdateSubAccountParams
import com.blockstream.common.gdk.params.ValidateAddresseesParams
import com.blockstream.common.interfaces.HttpRequestHandler
import com.blockstream.common.interfaces.HttpRequestProvider
import com.blockstream.common.interfaces.HttpRequestUrlValidator
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
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.server
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toHex
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
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

typealias EnrichedAssetPair = Pair<EnrichedAsset, Long>

typealias AccountId = String

/* Handles multiple GDK sessions per network */
class GdkSession constructor(
    private val userAgent: String,
    private val sessionManager: SessionManager,
    private val lightningManager: LightningManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val gdk: Gdk,
    private val wally: Wally,
    private val countly: CountlyBase
) : HttpRequestHandler, HttpRequestProvider, AssetsProvider {
    fun createScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) = CoroutineScope(SupervisorJob() + dispatcher + logException(countly))

    private val scope = createScope(Dispatchers.Default)
    private val parentJob = SupervisorJob()

    var httpRequestUrlValidator: HttpRequestUrlValidator? = null

    val isTestnet: Boolean // = false
        get() = defaultNetwork.isTestnet

    val isMainnet: Boolean
        get() = !isTestnet

    var isWatchOnly: Boolean = false

    //  Disable notification handling until all networks are initialized
    private var _disableNotificationHandling = false

    private var _walletTotalBalanceSharedFlow = MutableStateFlow(-1L)
    private var _activeAccountStateFlow: MutableStateFlow<Account?> = MutableStateFlow(null)
    private var _walletAssetsFlow : MutableStateFlow<Assets> = MutableStateFlow(Assets())
    private var _enrichedAssetsFlow : MutableStateFlow<List<CountlyAsset>> = MutableStateFlow(listOf())
    private var _walletHasHistorySharedFlow = MutableStateFlow(false)
    private var _accountAssetsFlow = mutableMapOf<AccountId, MutableStateFlow<Assets>>()
    private val _accountsAndBalanceUpdatedSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    private var _walletTransactionsStateFlow : MutableStateFlow<List<Transaction>> = MutableStateFlow(listOf(Transaction.LoadingTransaction))
    private var _accountTransactionsStateFlow = mutableMapOf<AccountId, MutableStateFlow<List<Transaction>>>()
    private var _accountTransactionsPagerSharedFlow = mutableMapOf<AccountId, MutableSharedFlow<Boolean>>()
    private var _blockStateFlow = mutableMapOf<Network, MutableStateFlow<Block>>()
    private var _settingsStateFlow = mutableMapOf<Network, MutableStateFlow<Settings?>>()
    private var _twoFactorConfigStateFlow = mutableMapOf<Network, MutableStateFlow<TwoFactorConfig?>>()
    private var _twoFactorResetStateFlow = mutableMapOf<Network, MutableStateFlow<TwoFactorReset?>>()
    private var _networkEventsStateFlow = mutableMapOf<Network, MutableStateFlow<NetworkEvent?>>()
    private val _networkErrors: Channel<Pair<Network, NetworkEvent>> = Channel()
    private var _failedNetworksStateFlow: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    private var _systemMessageStateFlow : MutableStateFlow<List<Pair<Network, String>>> = MutableStateFlow(listOf())
    private var _allAccountsStateFlow = MutableStateFlow<List<Account>>(listOf())
    private var _accountsStateFlow = MutableStateFlow<List<Account>>(listOf())
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

    private fun accountTransactionsPagerSharedFlow(account: Account): MutableSharedFlow<Boolean>{
        return _accountTransactionsPagerSharedFlow.getOrPut(account.id) {
            MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
    }

    private fun accountAssetsStateFlow(account: Account): MutableStateFlow<Assets> {
        return _accountAssetsFlow.getOrPut(account.id) {
            MutableStateFlow(Assets())
        }
    }

    private fun accountTransactionsStateFlow(account: Account): MutableStateFlow<List<Transaction>>{
        return _accountTransactionsStateFlow.getOrPut(account.id) {
            MutableStateFlow(listOf(Transaction.LoadingTransaction))
        }
    }

    val activeAccount get() = _activeAccountStateFlow.asStateFlow()

    val walletTotalBalance get() = _walletTotalBalanceSharedFlow.asStateFlow()

    val walletAssets: StateFlow<Assets> get() = _walletAssetsFlow.asStateFlow()

    val enrichedAssets: StateFlow<List<CountlyAsset>> get() = _enrichedAssetsFlow.asStateFlow()

    val walletHasHistory get() = _walletHasHistorySharedFlow.value

    fun accountAssets(account: Account) = accountAssetsStateFlow(account).asStateFlow()

    val accountsAndBalanceUpdated get() = _accountsAndBalanceUpdatedSharedFlow.asSharedFlow()

    val walletTransactions get() = _walletTransactionsStateFlow.asStateFlow()

    val failedNetworks get() = _failedNetworksStateFlow.asStateFlow()

    val systemMessage get() = _systemMessageStateFlow.asStateFlow()

    val allAccounts : StateFlow<List<Account>> get() = _allAccountsStateFlow.asStateFlow()

    val accounts : StateFlow<List<Account>> get() = _accountsStateFlow.asStateFlow()

    val lastInvoicePaid = _lastInvoicePaid.asStateFlow()

    val accountAsset : StateFlow<List<AccountAsset>> get() = _accountAssetStateFlow.asStateFlow()

    val torStatusFlow = _torStatusSharedFlow.asStateFlow()

    val tickerFlow = _tickerSharedFlow.asSharedFlow()

    val zeroAccounts = _zeroAccounts.asStateFlow()

    val multisigBitcoinWatchOnly = _multisigBitcoinWatchOnly.asStateFlow()
    val multisigLiquidWatchOnly = _multisigLiquidWatchOnly.asStateFlow()

    val networkErrors = _networkErrors.receiveAsFlow()
    fun getEnrichedAssets(id: String?) = _enrichedAssetsFlow.value.find { it.assetId == id }

    fun accountTransactions(account: Account) = accountTransactionsStateFlow(account).asStateFlow()

    fun accountTransactionsPager(account: Account) = accountTransactionsPagerSharedFlow(account).asSharedFlow()

    fun block(network: Network): StateFlow<Block> = blockStateFlow(network).asStateFlow()

    fun settings(network: Network = defaultNetwork) = settingsStateFlow(network).asStateFlow()
    fun twoFactorConfig(network: Network = defaultNetwork) = twoFactorConfigStateFlow(network).asStateFlow()

    fun twoFactorReset(network: Network) = twoFactorResetStateFlow(network).asStateFlow()

    fun networkEvents(network: Network) = networkEventsStateFlow(network).asStateFlow()

    val hasLiquidAccount : Boolean get() = accounts.value.any { it.isLiquid }

    val lightningNodeInfoStateFlow
        get() = lightningSdk.nodeInfoStateFlow

    val lspInfoStateFlow
        get() = lightningSdk.lspInfoStateFlow

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

    val gdkSessions = linkedMapOf<Network, GASession>()
    val activeSessions = mutableSetOf<Network>()

    fun hasActiveNetwork(network: Network?) = activeSessions.contains(network)

    private val activeGdkSessions: Map<Network, GASession>
        get() = gdkSessions.filter { activeSessions.contains(it.key) }

    private fun gdkSession(network: Network) = gdkSessions.getOrPut(network){
        gdk.createSession()
    }

    var hasLightning : Boolean = false
        private set

    var isLightningShortcut : Boolean = false
        private set

    private var _lightningAccount: Account? = null

    val lightningAccount : Account
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

    var device: DeviceInterface? = null
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

    var xPubHashId : String? = null
        private set

    var pendingTransaction: Pair<CreateTransactionParams, CreateTransaction>? = null

    val networkAssetManager: NetworkAssetManager get() = assetManager.getNetworkAssetManager(isMainnet)

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
        }.launchIn(scope + Dispatchers.IO)

        isConnectedState.drop(1).onEach {
            sessionManager.fireConnectionChangeEvent()
        }.launchIn(scope)
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
        if (isNetworkInitialized && !isWatchOnly) {
            countly.getRemoteConfigValueForAssets(if (isMainnet) LIQUID_ASSETS_KEY else LIQUID_ASSETS_TESTNET_KEY)?.also {
                cacheAssets(it.map { it.assetId })
            }.also {
                _enrichedAssetsFlow.value = it ?: listOf()
            }
        }
    }

    fun reportLightningError(paymentHash: String){
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

    fun updateSettings(network: Network? = null): Settings?{
        activeSessions.filter { network == null || it.network == network.id }.forEach {
            settingsStateFlow(it).value = gdk.getSettings(gdkSession(it))
            if(it.isMultisig){
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
        if(!network.isMultisig) return null

        var config = if(useCache) twoFactorConfigStateFlow(network).value else null

        if(config == null){
            gdk.getTwoFactorConfig(gdkSession(network)).also {
                twoFactorConfigStateFlow(network).value = it
                twoFactorResetStateFlow(network).value = it.twoFactorReset
                config = it
            }
        }

        return config
    }

    fun changeSettings(network: Network, settings: Settings) =
        authHandler(network, gdk.changeSettings(gdkSession(network), settings)).resolve()

    fun changeGlobalSettings(settings: Settings){
        val exceptions = mutableListOf<Exception>()
        activeSessions.forEach { network ->
            getSettings(network)?.also { networkSettings ->

                if(walletExistsAndIsUnlocked(network)) {
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

        if(exceptions.isNotEmpty()){
            throw Exception(exceptions.first().message)
        }
    }

    private fun syncSettings(){
        // Prefer Multisig for initial sync as those networks are synced across devices
        // In case of Lightning Shorcut get settings from parent wallet
        val syncNetwork = activeBitcoinMultisig ?: activeLiquidMultisig ?: defaultNetwork
        val prominentSettings = ephemeralWallet?.extras?.settings?.takeIf { isLightningShortcut } ?: getSettings(network = syncNetwork)
        prominentSettings?.also {
            try{
                changeGlobalSettings(it)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    @Throws(Exception::class)
    fun availableCurrencies() = gdk.getAvailableCurrencies(gdkSession(defaultNetwork))

    fun prominentNetwork(isTestnet: Boolean) = if(isTestnet) networks.testnetBitcoinElectrum else networks.bitcoinElectrum
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
        var spvServers: List<String>? = null

        // SPV for liquid is disabled // https://gl.blockstream.com/blockstream/green/gdk/-/issues/580
        val spvEnabled = applicationSettings.spv && !Network.isLiquid(network.id)
        var spvMulti = false // Only available in Singlesig

        if (network.isElectrum) {
            electrumUrl = applicationSettings.getPersonalElectrumServer(network).takeIf { it.isNotBlank() }

            spvMulti = applicationSettings.multiServerValidation && !Network.isLiquid(network.id)

            applicationSettings.getSpvElectrumServer(network).takeIf { spvMulti && it.isNotBlank() }?.also { spvElectrumServer ->
                spvServers = spvElectrumServer.split(",").map { it.trim() }
            }

        } else {
            val url = applicationSettings.getPersonalElectrumServer(network)

            if (spvEnabled && !url.isNullOrBlank()) {
                electrumUrl = url
            }
        }

        val useTor = applicationSettings.tor

        return ConnectionParams(
            networkName = network.id,
            useTor = useTor,
            userAgent = userAgent,
            proxy = applicationSettings.proxyUrl ?: "",
            spvEnabled = spvEnabled,
            spvMulti = spvMulti,
            electrumUrl = electrumUrl,
            electrumOnionUrl = electrumUrl.takeIf { useTor },
            spvServers = spvServers
        )
    }

    // Use it only for connected sessions
    fun supportsLightning() = supportsLightning(isWatchOnly = isWatchOnly, device = device)

    private fun supportsLightning(isWatchOnly: Boolean, device: DeviceInterface?) :Boolean {
        return !isWatchOnly && (device == null || device.isJade)
    }

    fun networks(isTestnet: Boolean, isWatchOnly: Boolean, device: DeviceInterface?): List<Network> {
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
            networks(isTestnet = isTestnet, isWatchOnly = isWatchOnly, device = device)
        }else{
            // init the provided networks
            initNetworks
        }).forEach {
            if(it.isLightning){
                lightning = it
            }else{
                gdkSession(it)
            }
        }
    }

    suspend fun connect(network: Network, initNetworks: List<Network>? = null): List<Network> {
        defaultNetworkOrNull = network

        disconnect()

        initGdkSessions(initNetworks = initNetworks)

        return gdkSessions.map {
            scope.async(context = Dispatchers.IO, start = CoroutineStart.DEFAULT) {
                try {
                    gdk.connect(it.value, createConnectionParams(it.key))
                    it.key
                } catch (e: Exception) {
                    _failedNetworksStateFlow.value = _failedNetworksStateFlow.value + it.key
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    fun getProxySettings() = gdk.getProxySettings(gdkSession(defaultNetwork))

    fun reconnectHint(hint: ReconnectHintParams) =
        scope.launch(context = Dispatchers.IO + logException(countly)) {
            gdkSessions.forEach {
                gdk.reconnectHint(it.value, hint)
            }
        }

    fun disconnect() {
        _isConnectedState.value = false
        xPubHashId = null

        authenticationRequired.clear()

        _activeAccountStateFlow.value = null

        // Recreate subject so that can be sure we have fresh data, especially on shared sessions eg. HWW sessions
        _accountsStateFlow.value = listOf()
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

        // Clear Assets
        _walletAssetsFlow.value = Assets()

        // Clear Enriched Assets
        _enrichedAssetsFlow.value = listOf()

        // Clear Transactions
        _walletTransactionsStateFlow.value = listOf(Transaction.LoadingTransaction)
        _accountTransactionsStateFlow = mutableMapOf()
        _accountTransactionsPagerSharedFlow = mutableMapOf()

        _tempAllowedServers.clear()

        _walletActiveEventInvalidated = true
        _accountEmptiedEvent = null

        val gaSessionToBeDestroyed = gdkSessions.values.toList()

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
            gdk.destroySession(it)
        }
    }

    private fun resetNetwork(network: Network){
        // Remove as active network
        activeSessions.remove(network)

        gdkSessions.remove(network)?.also { gaSessionToBeDestroyed ->
            gdk.destroySession(gaSessionToBeDestroyed)
        }

        // Init a new Session and connect
        try{
            gdk.connect(gdkSession(network), createConnectionParams(network))
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun disconnectAsync(reason: LogoutReason = LogoutReason.USER_ACTION) {
        // Disconnect only if needed
        if(isConnected) {
            logoutReason = reason
            _isConnectedState.value = false

            scope.launch(context = Dispatchers.IO + logException(countly)) {
                disconnect()

                // Destroy session if it's ephemeral
                ephemeralWallet?.also {
                    sessionManager.destroyEphemeralSession(gdkSession = this@GdkSession)
                }

                // Disconnect last device connection
                device?.also { device ->
                    if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.connectionIdentifier == device.connectionIdentifier }){
                        device.disconnect()
                    }
                }
            }
        }
    }

    override val httpRequest: HttpRequestHandler
        get() = this

    override fun prepareHttpRequest() {
        Logger.i { "Prepare HTTP Request Provider" }
        disconnect()

        networks.bitcoinElectrum.also {
            runBlocking {
                connect(network = it, initNetworks = listOf(it))
            }
        }
    }

    override fun httpRequest(
        method: String,
        urls: List<String>?,
        data: String?,
        accept: String?,
        certs: List<String>?
    ): JsonElement {

        val details = buildJsonObject {
            put("method", method)

            if(urls != null){

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

            if (certs != null){
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

    override fun httpRequest(details: JsonElement): JsonElement {
        if(!isNetworkInitialized){
            prepareHttpRequest()
        }

        val urls = details.jsonObject["urls"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: listOf()

        httpRequestUrlValidator?.also { urlValidator ->
            val isUrlSafe = urls.filter { it.isNotBlank() }.all { url ->
                BlockstreamPinOracleUrls.any { blockstreamUrl ->
                    url.startsWith(blockstreamUrl)
                }
            }

            val servers = urls.map {
                it.server()
            }

            if(!isUrlSafe && !(settingsManager.isAllowCustomPinServer(urls) || _tempAllowedServers.containsAll(servers))){
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
            if(urls.find { it.contains("/set_pin") } != null){
                countly.jadeInitialize()
            }
        }
    }

    private suspend fun initLightningSdk(lightningMnemonic: String?) {
        if(isHardwareWallet){
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
            if(lightningSdkOrNull == null && supportsLightning()){
                // Init SDK
                initLightningSdk(mnemonic)
            }

            if (!hasLightning) {
                connectToGreenlight(mnemonic = mnemonic ?: deriveLightningMnemonic())

                if(!hasLightning){
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

    fun <T> initNetworkIfNeeded(network: Network, hardwareWalletResolver: HardwareWalletResolver? = null, action: () -> T) : T{
        if(!activeSessions.contains(network)){

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
                changeSettings(network, Settings.normalizeFromProminent(networkSettings = getSettings(network) ?: it, prominentSettings = it, pgpFromProminent = true))
            }

            if (network.isMultisig) {
                updateTwoFactorConfig(network = network, useCache = false)
                updateWatchOnlyUsername(network = network)
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
        scope.launch(context = Dispatchers.IO + logException(countly)){

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
                    Logger.i { "Login into ${network.id}" }

                    if (network.isLightning) {
                        // Connect SDK
                        connectToGreenlight(mnemonic = deriveLightningMnemonic())
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

    suspend fun loginWithPin(
        wallet: GreenWallet,
        pin: String,
        loginCredentials: LoginCredentials,
        appGreenlightCredentials: AppGreenlightCredentials?,
        isRestore: Boolean = false,
        initializeSession : Boolean = true,
    ): LoginData {
        val initNetworks = if(wallet.isLightning){
            listOf(
                networks.bitcoinElectrum,
                networks.lightning
            )
        }else{
            null
        }
        return loginWithLoginCredentials(
            prominentNetwork = prominentNetwork(wallet, loginCredentials),
            initNetworks = initNetworks,
            wallet = wallet,
            loginCredentialsParams = LoginCredentialsParams(pin = pin, pinData = loginCredentials.pin_data),
            appGreenlightCredentials = appGreenlightCredentials,
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
            loginCredentialsParams = loginCredentialsParams,
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
            loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic),
        )
    }


    suspend fun loginWatchOnly(wallet: GreenWallet, username: String, watchOnlyCredentials: WatchOnlyCredentials) {
        loginWatchOnly(prominentNetwork(wallet), username, watchOnlyCredentials = watchOnlyCredentials)
    }

    suspend fun loginWatchOnly(network: Network, username: String = "", watchOnlyCredentials: WatchOnlyCredentials): LoginData {
        val loginCredentialsParams = if (network.isSinglesig) {
            if (watchOnlyCredentials.coreDescriptors != null) {
                LoginCredentialsParams(coreDescriptors = watchOnlyCredentials.coreDescriptors)
            } else {
                LoginCredentialsParams(slip132ExtendedPubkeys = watchOnlyCredentials.slip132ExtendedPubkeys)
            }
        } else {
            LoginCredentialsParams(username = username, password = watchOnlyCredentials.password)
        }

        return loginWatchOnly(
            network = network,
            loginCredentialsParams = loginCredentialsParams,
            isRestore = false, // No need to do restore procedures for wo
        )
    }

    private suspend fun loginWatchOnly(network: Network, loginCredentialsParams: LoginCredentialsParams, isRestore: Boolean): LoginData {
        return loginWithLoginCredentials(
            prominentNetwork = network,
            initNetworks = listOf(network),
            loginCredentialsParams = loginCredentialsParams,
            isRestore = isRestore,
            initializeSession = true
        )
    }

    suspend fun loginWithDevice(
        wallet: GreenWallet,
        device: DeviceInterface,
        derivedLightningMnemonic: String?,
        hardwareWalletResolver: HardwareWalletResolver,
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
            prominentNetwork = supportedNetworks.first(),
            initNetworks = initNetworks,
            wallet = wallet,
            loginCredentialsParams = LoginCredentialsParams.empty,
            derivedLightningMnemonic = derivedLightningMnemonic,
            device = device,
            isSmartDiscovery = true,
            hardwareWalletResolver = hardwareWalletResolver,
            hwInteraction = hwInteraction,
        )
    }

    private suspend fun loginWithLoginCredentials(
        prominentNetwork: Network,
        initNetworks: List<Network>? = null,
        wallet: GreenWallet? = null,
        loginCredentialsParams: LoginCredentialsParams,
        appGreenlightCredentials: AppGreenlightCredentials? = null,
        derivedLightningMnemonic: String? = null,
        device: DeviceInterface? = null,
        isCreate: Boolean = false,
        isRestore: Boolean = false,
        isSmartDiscovery: Boolean = false,
        initializeSession: Boolean = true,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        hwInteraction: HardwareWalletInteraction? = null,
    ): LoginData {
        this.isWatchOnly = loginCredentialsParams.isWatchOnly
        this.device = device

        _disableNotificationHandling = true
        _walletActiveEventInvalidated = true

        val connectedNetworks = connect(
            network = prominentNetwork,
            initNetworks = initNetworks,
        )

        device?.deviceState?.onEach {
            // Device went offline
            if(it == DeviceState.DISCONNECTED){
                disconnectAsync(reason = LogoutReason.DEVICE_DISCONNECTED)
            }
        }?.launchIn(scope)

        val deviceParams = DeviceParams.fromDeviceOrEmpty(device?.gdkHardwareWallet?.device)

        val initAccount = wallet?.activeAccount
        val initNetwork = wallet?.activeNetwork

        // Get enabled singlesig networks (multisig can be identified by login_user)
        val enabledGdkSessions = gdkSessions.toSortedLinkedHashMap { n1, n2 -> // Sort prominent first
            if(n1 == prominentNetwork) {
                -1
            }else if(n2 == prominentNetwork) {
                1
            }else{
                n1.id.compareTo(n2.id)
            }
        }

        // If it's a pin login, check if the prominent network is connected
        if(loginCredentialsParams.pin.isNotBlank() && !connectedNetworks.contains(prominentNetwork)){
            throw Exception("id_connection_failed")
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

        val exceptions = mutableListOf<Exception>()

        isLightningShortcut = wallet?.isLightning == true
        hasLightning = isLightningShortcut || appGreenlightCredentials != null || derivedLightningMnemonic != null

        return (enabledGdkSessions.mapNotNull { gdkSession ->
            scope.async(context = Dispatchers.IO, start = CoroutineStart.LAZY) {
                val isProminent = gdkSession.key == prominentNetwork
                val network = gdkSession.key
                try {
                    val hasGdkCache = if(isHardwareWallet || loginCredentialsParams.mnemonic.isNotBlank()){
                        try {
                            gdk.hasGdkCache(
                                getWalletIdentifier(
                                    network = network,
                                    loginCredentialsParams = loginCredentialsParams,
                                    hwInteraction = hwInteraction
                                )
                            )
                        } catch (e: Exception){
                            e.printStackTrace()
                            false
                        }
                    } else {
                        false
                    }

                    if (gdkSession.key.isSinglesig && !isProminent && !isRestore && !isSmartDiscovery && !hasGdkCache){
                        Logger.i { "Skip login in ${network.id}" }
                        return@async null
                    }

                    // On Create just login into Bitcoin network
                    if(isCreate && (gdkSession.key.isMultisig || gdkSession.key.isLiquid)){
                        Logger.i { "Skip login in ${network.id}" }
                        return@async null
                    }

                    Logger.i { "Login into ${network.id}" }

                    authHandler(
                        gdkSession.key,
                        gdk.loginUser(
                            session = gdkSession.value,
                            deviceParams = deviceParams,
                            loginCredentialsParams = loginCredentialsParams
                        )
                    ).result<LoginData>(hardwareWalletResolver = hardwareWalletResolver).also { loginData ->
                        // Mark it as active
                        activeSessions.add(network)

                        // Do a refresh
                        if (network.isElectrum && initializeSession && (isRestore || isSmartDiscovery)) {
                             if(isRestore || !hasGdkCache){
                                Logger.i { "BIP44 Discovery for ${network.id}" }

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
                                        gdk.removeGdkCache(loginData)
                                    }
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
                context = Dispatchers.IO,
                start = CoroutineStart.LAZY
            ) {

                if(isHardwareWallet && derivedLightningMnemonic == null){
                    return@async null
                }

                val lightningMnemonic = derivedLightningMnemonic ?: deriveLightningMnemonic(Credentials.fromLoginCredentialsParam(loginCredentialsParams))

                // Init SDK
                initLightningSdk(lightningMnemonic)

                if (hasLightning || ((isRestore || isSmartDiscovery) && settingsManager.isLightningEnabled())) {
                    // Make it async to speed up login process
                    val job = scope.async {
                        try {
                            val xPubHashId = if(isLightningShortcut) wallet?.xPubHashId else getWalletIdentifier(
                                network = prominentNetwork,
                                loginCredentialsParams = loginCredentialsParams,
                                hwInteraction = hwInteraction
                            ).xpubHashId

                            // Connect SDK
                            connectToGreenlight(mnemonic = lightningMnemonic, parentXpubHashId = xPubHashId, checkCredentials = isRestore || isSmartDiscovery, quickResponse = isRestore)

                            if(isRestore) {
                                hasLightning = lightningSdk.isConnected
                            }

                            updateAccountsAndBalances(updateBalancesForNetwork = lightning)
                            updateWalletTransactions(updateForNetwork = lightning)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _failedNetworksStateFlow.value = _failedNetworksStateFlow.value + listOfNotNull(lightning)
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
                if(isHardwareWallet){ // On hardware connection in series to avoid any race condition with the hw api
                    deferred.map { it.await() }
                }else{
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
                    initAccount = initAccount,
                    initNetwork = initNetwork,
                    initializeSession = initializeSession
                )
            }
        }
    }

    private suspend fun connectToGreenlight(
        mnemonic: String,
        parentXpubHashId: String? = null,
        checkCredentials: Boolean = false,
        quickResponse: Boolean = false
    ) {
        Logger.i { "Login into ${lightning?.id}" }

        countly.loginLightningStart()

        lightningSdk.connectToGreenlight(mnemonic, parentXpubHashId ?: xPubHashId.takeIf { !isLightningShortcut }, checkCredentials, quickResponse).also {
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

    private fun reLogin(network: Network): LoginData {
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

            updateAccountsAndBalances(updateBalancesForNetwork =  network)
            updateWalletTransactions(updateForNetwork = network)
        }
    }

    private suspend fun onLoginSuccess(
        loginData: LoginData,
        initNetwork: String?,
        initAccount: Long?,
        initializeSession: Boolean
    ) {
        _isConnectedState.value = true
        xPubHashId = if(isWatchOnly) loginData.networkHashId else loginData.xpubHashId

        if(initializeSession) {
            countly.activeWalletStart()
            initializeSessionData(initNetwork, initAccount)
        }

        // Allow initialization calls to have priority over notifications initiated updates (getWalletTransactions & updateAccountAndBalances)
        _disableNotificationHandling = false
    }

    private suspend fun initializeSessionData(initNetwork: String?, initAccount: Long?) {
        // Check if active account index was archived from 1) a different client (multisig) or 2) from cached Singlesig hww session
        // Expect refresh = true to be already called
        updateAccounts()

        _activeAccountStateFlow.value = accounts.value.find {
            it.networkId == initNetwork && it.pointer == initAccount && !it.hidden
        } ?: accounts.value.firstOrNull() ?: this.allAccounts.value.firstOrNull()

        // Update Liquid Assets from GDK before getting balances to sort them properly
        updateLiquidAssets()

        // Update the enriched assets
        updateEnrichedAssets()

        if(!isWatchOnly && !isLightningShortcut) {
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
            }catch (e: Exception){
                countly.recordException(e)
            }
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
        scope.launch(context = Dispatchers.IO + logException(countly)) {
            _systemMessageStateFlow.value = gdkSessions.map {
                it.key to (gdk.getSystemMessage(it.value) ?: "")
            }.filter { !it.second.isNullOrBlank() }
        }
    }

    fun ackSystemMessage(network: Network, message: String) = authHandler(
        network,
        gdk.ackSystemMessage(gdkSession(network), message)
    ).resolve()

    fun setTransactionMemo(transaction: Transaction, memo: String) {
        gdk.setTransactionMemo(gdkSession(transaction.account.network), transaction.txHash, memo)

        // update transaction
        getTransactions(account = transaction.account, isReset = false, isLoadMore = false)
        updateWalletTransactions(updateForAccounts = listOf(transaction.account))
    }

    fun getWalletIdentifier(
        network: Network,
        loginCredentialsParams: LoginCredentialsParams? = null,
        gdkHwWallet: GdkHardwareWallet? = null,
        hwInteraction: HardwareWalletInteraction? = null,
    ) = gdk.getWalletIdentifier(
        connectionParams = createConnectionParams(network),
        loginCredentialsParams = loginCredentialsParams?.takeIf { !it.mnemonic.isNullOrBlank() }
            ?: (gdkHwWallet ?: this.gdkHwWallet)?.let {
                LoginCredentialsParams(
                    masterXpub = it.getXpubs(network, hwInteraction, listOf(listOf())).first()
                )
            }
            ?: loginCredentialsParams
            ?: getCredentials().let { LoginCredentialsParams.fromCredentials(it) }
    )

    fun getWalletFingerprint(
        network: Network,
        gdkHwWallet: GdkHardwareWallet? = null,
        hwInteraction: HardwareWalletInteraction? = null,
    ): String? {
        return (gdkHwWallet ?: this.gdkHwWallet)?.let {
            wally.bip32Fingerprint(
                it.getXpubs(network, hwInteraction, listOf(listOf())).first()
            )
        }
    }

    fun encryptWithPin(network: Network?, encryptWithPinParams: EncryptWithPinParams): EncryptWithPin {
        @Suppress("NAME_SHADOWING")
        val network = network ?: defaultNetwork

        return authHandler(
            network,
            gdk.encryptWithPin(gdkSession(network), encryptWithPinParams)
        ).result<EncryptWithPin>().also {
            it.networkInjected = (network)
        }
    }

    private fun decryptCredentialsWithPin(network: Network, decryptWithPinParams: DecryptWithPinParams): Credentials {
        return authHandler(
            network,
            gdk.decryptWithPin(gdkSession(network), decryptWithPinParams)
        ).result()
    }

    fun getCredentials(params: CredentialsParams = CredentialsParams()): Credentials {
        val network = defaultNetwork.takeIf { hasActiveNetwork(defaultNetwork) } ?: activeSessions.first()
        return authHandler(network, gdk.getCredentials(gdkSession(network), params)).result()
    }

    private var _derivedHwLightningMnemonic: String? = null
    fun deriveLightningMnemonic(credentials: Credentials? = null): String {
        if (isHardwareWallet && credentials == null) {
            return _derivedHwLightningMnemonic ?: throw Exception("HWW can't derive lightning mnemonic")
        }

        return (credentials ?: getCredentials()).let{
            if (isLightningShortcut || isHardwareWallet) {
                it.mnemonic // Already derived
            } else {
                wally.bip85FromMnemonic(
                    mnemonic = it.mnemonic,
                    passphrase = it.bip39Passphrase,
                    index = 0,
                    isTestnet = isTestnet
                ) ?: throw Exception("Couldn't derive lightning mnemonic")
            }
        }
    }

    fun getReceiveAddress(account: Account) = authHandler(
        account.network,
        gdk.getReceiveAddress(gdkSession(account.network),
            ReceiveAddressParams(account.pointer)
        )
    ).result<Address>()

    // Combine with receive address
    fun receiveOnchain(): SwapInfo {
        return lightningSdk.receiveOnchain()
    }

    fun createLightningInvoice(satoshi: Long, description: String): ReceivePaymentResponse {
        return lightningSdk.createInvoice(satoshi, description)
    }

    fun getPreviousAddresses(account: Account, lastPointer: Int?) = authHandler(
        account.network,
        gdk.getPreviousAddress(gdkSession(account.network),
            PreviousAddressParams(account.pointer, lastPointer = lastPointer)
        )
    ).result<PreviousAddresses>()

    override fun refreshAssets(params: AssetsParams) {
        (activeLiquid ?: liquid)?.also { gdk.refreshAssets(gdkSession(it), params) }
    }
    override fun getAssets(params: GetAssetsParams) = (activeLiquid ?: liquid)?.let { gdk.getAssets(gdkSession(it), params) }

    fun createAccount(
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
        (if(isLightningShortcut){
            listOf()
        }else{
            activeGdkSessions.map {
                scope.async(context = Dispatchers.IO) {
                    getAccounts(it.key, refresh)
                }
            }.awaitAll().flatten()
        }).let {
            if(hasLightning) it + lightningAccount else it
        }.sorted()
    }

    private fun getAccounts(network: Network, refresh: Boolean = false): List<Account> = initNetworkIfNeeded(network) {
        gdkSession(network).let {
            // Watch-only can't discover new accounts
            authHandler(network, gdk.getSubAccounts(it, SubAccountsParams(refresh = if(isWatchOnly) false else refresh)))
                .result<Accounts>().accounts.onEach { account ->
                    account.setup(this, network)
                }
        }
    }

    fun getAccount(account: Account) = authHandler(
        account.network, gdk.getSubAccount(gdkSession(account.network), account.pointer)
    ).result<Account>().also {
        it.setup(this, account.network)

        // Update active account if needed
        if(_activeAccountStateFlow.value?.id == it.id){
            _activeAccountStateFlow.value = it
        }
    }

    fun setActiveAccount(account: Account){
        // Get the account from the list of accounts just to be sure we have the latest account data
        _activeAccountStateFlow.value = accounts.value.find { it.id == account.id } ?: account
    }

    fun removeAccount(account: Account){
        if(account.isLightning){
            hasLightning = false

            lightningSdk.stop()

            // Update accounts
            updateAccounts()

            updateAccountsAndBalances()
            updateWalletTransactions()
        }
    }

    fun updateAccount(
        account: Account,
        isHidden: Boolean,
        resetAccountName: String? = null
    ): Account {
        // Disable account editing for lightning accounts
        if(account.isLightning) return account

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
        authHandler(account.network,
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

    suspend fun getFeeEstimates(network: Network): FeeEstimation = try {
        if(network.isLightning){
            lightningSdk.recommendedFees().let { fees ->
                (
                        listOf(fees.minimumFee) +
                        List(FeeBlockTarget[0] - 0) { fees.fastestFee } +
                        List(FeeBlockTarget[1] - FeeBlockTarget[0]) { fees.halfHourFee } +
                        List(FeeBlockTarget[2] - FeeBlockTarget[1]) { fees.hourFee }
                        ).map { it.toLong() }.let {
                    FeeEstimation(it)
                }
            }

        }else{
            gdk.getFeeEstimates(gdkSession(network)).let {
                // Temp fix
                if(network.isSinglesig && network.isLiquid) {
                    FeeEstimation(it.fees.map { fee ->
                        fee.coerceAtLeast(100L)
                    })
                }else{
                    it
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        FeeEstimation(fees = mutableListOf(network.defaultFee))
    }

    fun getTransactions(account: Account, params: TransactionParams = TransactionParams(subaccount = 0)) = (if (account.network.isLightning) {
        getLightningTransactions()
    } else {
        authHandler(account.network, gdk.getTransactions(gdkSession(account.network), params))
            .result<Transactions>()
    }).also {
        it.transactions.onEach { tx ->
            tx.accountInjected = account
        }
    }

    private val refreshMutex = Mutex()
    fun refresh(account: Account? = null) {
        scope.launch(context = Dispatchers.IO + logException(countly)) {
            refreshMutex.withLock {
                if(account == null || account.isLightning){
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
        Transactions(transactions = it ?: listOf(Transaction.LoadingTransaction))
    }

    private val accountsAndBalancesMutex = Mutex()
    fun updateAccountsAndBalances(
        isInitialize: Boolean = false,
        refresh: Boolean = false,
        updateBalancesForNetwork: Network? = null,
        updateBalancesForAccounts: Collection<Account>? = null
    ) {

        scope.launch(context = Dispatchers.IO + logException(countly)) {

            try{
                accountsAndBalancesMutex.withLock {

                    // Update accounts
                    updateAccounts(refresh = refresh)

                    for (account in this@GdkSession.allAccounts.value) {
                        if((updateBalancesForAccounts == null && updateBalancesForNetwork == null) || updateBalancesForAccounts?.find { account.id == it.id } != null || account.network == updateBalancesForNetwork) {
                            getBalance(account = account, cacheAssets = isInitialize).also {
                                accountAssetsStateFlow(account).value = it
                            }
                        }
                    }

                    // Wallet Assets
                    val walletAssets = linkedMapOf<String, Long>()

                    // Fix for only LN + Liquid wallets when LN is not fully initialized.
                    // The denomination Liquid based as we reside in the first key of _walletAssetsFlow to identify the main assetId
                    if(hasLightning){
                        walletAssets[BTC_POLICY_ASSET] = 0
                    }

                    accounts.value.forEach { account ->
                        this@GdkSession.accountAssets(account).value.assets.forEach { (key, value) ->
                            walletAssets[key] = (walletAssets[key] ?: 0) + value
                        }
                    }

                    if(isInitialize) {
                        // Cache wallet assets (again) + Enriched assets + liquid asset if network exists
                        (walletAssets.keys +
                                (enrichedAssets.value.takeIf { liquid != null }?.map { it.assetId } ?: emptyList()) +
                                listOfNotNull(liquid?.policyAsset))
                            .toSet().also {
                                cacheAssets(it)
                            }
                    }


                    walletAssets.toSortedLinkedHashMap(::sortAssets).also {
                        _walletAssetsFlow.value = Assets(it)
                    }

                    val accountAndAssets = accounts.value.flatMap {
                        this@GdkSession.accountAssets(it).value.toAccountAsset(it, this@GdkSession)
                    }

                    // Mark it if necessary
                    if(!walletHasHistory){
                        if(walletAssets.size > 2 || walletAssets.values.sum() > 0L) {
                            _walletHasHistorySharedFlow.value = true
                        }
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
    fun getTransactions(account: Account, isReset : Boolean, isLoadMore: Boolean) {
        scope.launch(context = Dispatchers.IO + logException(countly)) {
            val transactionsPagerSharedFlow = accountTransactionsPagerSharedFlow(account)
            val transactionsStateFlow = accountTransactionsStateFlow(account)

            try {
                transactionsMutex.withLock {
                    transactionsPagerSharedFlow.emit(false)

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

                    val transactions = getTransactions(account, TransactionParams(subaccount = account.pointer, offset = offset, limit = limit)).transactions

                    // Update transactions
                    transactionsStateFlow.value = if(isLoadMore){
                        transactionsStateFlow.value + transactions
                    }else{
                        transactions
                    }

                    // Update pager
                    if(isReset || isLoadMore){
                        transactionsPagerSharedFlow.emit(if(account.isLightning) false else transactions.size == TRANSACTIONS_PER_PAGE)
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
    private val _walletTransactions = mutableMapOf<AccountId, List<Transaction>>()
    fun updateWalletTransactions(updateForNetwork: Network? = null, updateForAccounts: Collection<Account>? = null) {
        scope.launch(context = Dispatchers.IO + logException(countly)) {
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
                            if(account.hidden){
                                // Clear transactions
                                _walletTransactions.remove(account.id)
                            }else {
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
                    if(hasLightning){
                        walletTransactions = lightningSdk.swapInfoStateFlow.value.map {
                            Transaction.fromSwapInfo(lightningAccount, it.first, it.second)
                        } + lightningSdk.reverseSwapInfoStateFlow.value.map {
                            Transaction.fromReverseSwapInfo(lightningAccount, it)
                        } + walletTransactions
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
                    val weight1 = getEnrichedAssets(a1)?.weight ?: 0
                    val weight2 = getEnrichedAssets(a2)?.weight ?: 0

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

    fun sortEnrichedAssets(a1: EnrichedAsset, a2: EnrichedAsset): Int {
        val w1 = a1.sortWeight(this)
        val w2 = a2.sortWeight(this)

        if(w1 != w2){
            return w2.compareTo(w1)
        }

        return a1.name(this).compareTo(a2.name(this))
    }

    private fun sortAccountAssets(a1: AccountAsset, a2: AccountAsset): Int = when {
        a1.account.isBitcoin && a2.account.isLiquid -> -1
        a1.account.isLiquid && a2.account.isBitcoin -> 1
        a1.asset.assetId == a2.asset.assetId -> a1.account.compareTo(a2.account)
        else -> {
            sortAssets(a1.asset.assetId, a2.asset.assetId)
        }
    }

    fun getBalance(account: Account, confirmations: Int = 0, cacheAssets: Boolean = false): Assets {
        val assets: LinkedHashMap<String, Long>? = if(account.isLightning) {
            lightningSdkOrNull?.balance()?.let { linkedMapOf(BTC_POLICY_ASSET to it) }
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

        if(cacheAssets) {
            // Cache assets before sorting them, as the sort function uses the asset metadata
            assets?.also { cacheAssets(it.keys) }
        }

        return Assets(assets?.toSortedLinkedHashMap(::sortAssets))
    }

    fun changeSettingsTwoFactor(
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

    fun setWatchOnly(network: Network, username: String, password: String) = gdk.setWatchOnly(
        gdkSession(network),
        username,
        password
    ).also {
        updateWatchOnlyUsername(network)
    }

    fun watchOnlyUsername(network: Network): StateFlow<String?> {
        return (if (network.isBitcoin) {
            _multisigBitcoinWatchOnly
        } else {
            _multisigLiquidWatchOnly
        }).asStateFlow()
    }

    private fun updateWatchOnlyUsername(network: Network? = null) {
        if (isWatchOnly) return

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

    fun twoFactorReset(
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

    fun twoFactorUndoReset(network: Network, email: String, twoFactorResolver: TwoFactorResolver) {
        logger.d { "TwoFactorUndoReset ${network.id} email:$email" }
        authHandler(network, gdk.twoFactorUndoReset(gdkSession(network), email)).resolve(
            twoFactorResolver = twoFactorResolver
        ).also {
            // Should we disconnect
            // disconnectAsync(LogoutReason.USER_ACTION)
            updateSettings(network)
        }
    }

    fun twoFactorCancelReset(network: Network, twoFactorResolver: TwoFactorResolver) {
        logger.d { "TwoFactorCancelReset ${network.id}" }
        authHandler(network, gdk.twoFactorCancelReset(gdkSession(network))).resolve(
            twoFactorResolver = twoFactorResolver
        ).also {
            // Should we disconnect
            // disconnectAsync(LogoutReason.USER_ACTION)
            updateSettings(network)
        }
    }

    fun twoFactorChangeLimits(network: Network, limits: Limits, twoFactorResolver: TwoFactorResolver): Limits {
        return authHandler(network, gdk.twoFactorChangeLimits(gdkSession(network), limits)).result<Limits>(
            twoFactorResolver = twoFactorResolver
        ).also {
            updateTwoFactorConfig(network)
        }
    }

    fun sendNlocktimes(network: Network) = gdk.sendNlocktimes(gdkSession(network))

    fun setCsvTime(network: Network, value: CsvParams, twoFactorResolver: TwoFactorResolver) {
        authHandler(
            network,
            gdk.setCsvTime(gdkSession(network), value)
        ).resolve(twoFactorResolver = twoFactorResolver).also {
            updateSettings(network)
        }
    }

    private fun updateAccounts(refresh: Boolean = false) {
        getAccounts(refresh).also { fetchedAccounts ->
            _allAccountsStateFlow.value = fetchedAccounts
            fetchedAccounts.filter { !it.hidden }.also {
                _accountsStateFlow.value = it
                _zeroAccounts.value = it.isEmpty() && failedNetworks.value.isEmpty()
            }

            // Update active account to get fresh data, also prevent it from being archived and active
            _activeAccountStateFlow.value = accounts.value.find {
                activeAccount.value?.id == it.id
            } ?: accounts.value.firstOrNull() ?: this.allAccounts.value.firstOrNull()
        }
    }

    // asset_info in Convert object can be null for liquid assets that don't have asset metadata
    // if no asset is given, no conversion is needed (conversion will be identified as a btc value in gdk)
    suspend fun convert(assetId: String? = null, asString: String? = null, asLong: Long? = null, denomination: String? = null): Balance? {
        val network = assetId.networkForAsset(this)
        val isPolicyAsset = assetId.isPolicyAsset(this)
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
            return Balance.fromAssetWithoutMetadata(asLong ?: 0)
        }

        val balance = withContext(context = Dispatchers.IO) {
            try {
                Balance.fromJsonElement(jsonElement = gdk.convertAmount(gdkSession(network), convert), assetId = assetId)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        balance?.asset = asset

        return balance
    }

    private fun getUnspentOutputs(network: Network, params: BalanceParams) = authHandler(
        network,
        gdk.getUnspentOutputs(gdkSession(network), params)
    ).result<UnspentOutputs>().also {
        it.fillUtxosJsonElement()
    }

    private fun getUnspentOutputsForPrivateKey(network: Network, params: UnspentOutputsPrivateKeyParams) = authHandler(
        network,
        gdk.getUnspentOutputsForPrivateKey(gdkSession(network), params)
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

    fun getUnspentOutputs(network: Network, privateKey: String): UnspentOutputs {
        return getUnspentOutputsForPrivateKey(network, UnspentOutputsPrivateKeyParams(
            privateKey = privateKey
        ))
    }

    @NativeCoroutinesIgnore
    suspend fun createTransaction(network: Network, params: CreateTransactionParams) =
        withContext(context = Dispatchers.IO) {
            if (network.isLightning) {
                createLightningTransaction(network, params)
            } else authHandler(network,
                gdk.createTransaction(gdkSession(network), params)
            ).result<CreateTransaction>()
        }

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
        Logger.d { "createLightningTransaction $params" }

        val address = params.addresseesAsParams?.firstOrNull()?.address ?: ""
        val userInputSatoshi = params.addresseesAsParams?.firstOrNull()?.satoshi

        return when (val lightningInputType = lightningSdk.parseBoltOrLNUrlAndCache(address)) {
            is InputType.Bolt11 -> {
                val invoice = lightningInputType.invoice

                Logger.d { "Expire in ${invoice.expireIn()}" }

                var sendableSatoshi = invoice.sendableSatoshi(userInputSatoshi)

                var error = generateLightningError(account = lightningAccount, satoshi = sendableSatoshi)

                // Check expiration
                if (invoice.isExpired()) {
                    error = "id_invoice_expired"
                }

                if(sendableSatoshi == null || sendableSatoshi == 0L){
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
                    error = error
                )
            }

            else -> {
                CreateTransaction(error = "id_invalid_address", isLightning = true)
            }
        }
    }

    fun createSwapTransaction(network: Network, params: CreateSwapParams, twoFactorResolver: TwoFactorResolver) = authHandler(
        network,
        gdk.createSwapTransaction(gdkSession(network), params)
    ).result<CreateSwapTransaction>(twoFactorResolver = twoFactorResolver)

    fun completeSwapTransaction(network: Network, params: CompleteSwapParams, twoFactorResolver: TwoFactorResolver) = authHandler(
        network,
        gdk.completeSwapTransaction(gdkSession(network), params)
    ).result<CreateTransaction>(twoFactorResolver = twoFactorResolver)

    fun signMessage(network: Network, params: SignMessageParams, hardwareWalletResolver: HardwareWalletResolver? = null): SignMessage = authHandler(
        network,
        gdk.signMessage(gdkSession(network), params = params)
    ).result(hardwareWalletResolver = hardwareWalletResolver)

    fun blindTransaction(network: Network, createTransaction: CreateTransaction) =
        authHandler(
            network,
            gdk.blindTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>()

    fun signTransaction(network: Network, createTransaction: CreateTransaction): CreateTransaction = if(network.isLightning){
        createTransaction // no need to sign on gdk side
    }else{
        authHandler(
            network,
            gdk.signTransaction(gdkSession(network), createTransaction = createTransaction.jsonElement!!)
        ).result<CreateTransaction>()
    }

    fun broadcastTransaction(network: Network, transaction: String) = SendTransactionSuccess(
        txHash = gdk.broadcastTransaction(
            gdkSession(network),
            transaction
        )
    ).also {
        _walletActiveEventInvalidated = true
    }

    fun sendTransaction(
        account: Account,
        signedTransaction: CreateTransaction,
        twoFactorResolver: TwoFactorResolver
    ): SendTransactionSuccess = if (account.network.isLightning) {
        val invoiceOrLnUrl = signedTransaction.addressees.first().address
        val satoshi = signedTransaction.addressees.first().satoshi?.absoluteValue ?: 0L
        val comment = signedTransaction.memo

        Logger.d { "invoiceOrLnUrl: $invoiceOrLnUrl satoshi: $satoshi comment: $comment " }

        _walletActiveEventInvalidated = true

        when (val inputType = lightningSdk.parseBoltOrLNUrlAndCache(invoiceOrLnUrl)) {
            is InputType.Bolt11 -> {
                // Check for expiration
                if(inputType.invoice.isExpired()){
                    throw Exception("id_invoice_expired")
                }

                Logger.d { "Sending invoice ${inputType.invoice.bolt11}" }

                try {
                    val response = lightningSdk.sendPayment(
                        inputType.invoice.bolt11,
                        satoshi.takeIf { inputType.invoice.amountMsat == null }
                    )

                    SendTransactionSuccess(paymentId = response.payment.id)
                } catch (e: Exception){
                    throw ExceptionWithErrorReport(
                        throwable = e,
                        ErrorReport.create(
                            throwable = e,
                            paymentHash = inputType.invoice.paymentHash,
                            network = account.network,
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
                            SendTransactionSuccess.create(it.data)
                        }
                        is LnUrlPayResult.EndpointError -> {
                            throw Exception(it.data.reason)
                        }
                        is LnUrlPayResult.PayError -> {
                            val exception = Exception(it.data.reason)
                            throw ExceptionWithErrorReport(
                                throwable = exception,
                                ErrorReport.create(
                                    throwable = exception,
                                    paymentHash = it.data.paymentHash,
                                    network = account.network,
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
    } else {
        authHandler(
            account.network,
            gdk.sendTransaction(gdkSession(account.network), transaction = signedTransaction.jsonElement!!)
        ).result<SendTransactionSuccess>(twoFactorResolver = twoFactorResolver).also {
            if(signedTransaction.isSendAll){
                _accountEmptiedEvent = account
            }
        }
    }

    private fun accountEmptiedEventIfNeeded() {
        _accountEmptiedEvent?.also { account ->
            countly.accountEmptied(
                session = this@GdkSession,
                walletHasFunds = walletAssets.value.hasFunds,
                accountsFunded = _accountAssetsFlow.values.map { it.value.hasFunds }
                    .count { it },
                accounts = this@GdkSession.accounts.value,
                account = account
            )
            _accountEmptiedEvent = null
        }
    }

    fun emptyLightningAccount(){
        _accountEmptiedEvent = _lightningAccount
    }

    private fun walletActiveEventIfNeeded(){
        if(_walletActiveEventInvalidated) {
            countly.activeWalletEnd(
                session = this,
                walletHasFunds = walletAssets.value.hasFunds,
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
            else -> { }
        }
    }

    fun onNewNotification(gaSession: GASession, notification: Notification) {

        val network = gdkSessions.firstNotNullOfOrNull { if(it.value == gaSession) it.key else null } ?: return

        Logger.d { "onNewNotification ${network.id} \t $notification" }

        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    // SingleSig after connect immediatelly sends a block with height 0
                    // it's not safe to call getTransactions so early
                    if(it.height > 0) {
                        blockStateFlow(network).value = it

                        if(!_disableNotificationHandling) {
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
                            scope.launch(context = Dispatchers.IO + logException(countly)){
                                reLogin(network)
                            }
                        }else if(!event.isConnected){
                            // mark re-authentication is required
                            authenticationRequired[network] = true
                        }
                    }

                    networkEventsStateFlow(network).value = event

                    // Personal Electrum Server Error
                    if(network.isSinglesig && !event.isConnected && settingsManager.appSettings.getPersonalElectrumServer(network).isNotBlank()){
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
                if(!_disableNotificationHandling && notification.subaccount?.isSynced == true) {
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
                    }
                }
            }
        }
    }

    private fun cacheAssets(assetIds: Collection<String>) {
        assetIds.filter { it != BTC_POLICY_ASSET }.takeIf { it.isNotEmpty() }?.also {
            Logger.d { "Cache assets: $it" }
            networkAssetManager.cacheAssets(it, this)
        }
    }

    fun hasAssetIcon(assetId : String) = networkAssetManager.hasAssetIcon(assetId)
    fun getAsset(assetId : String): Asset? = networkAssetManager.getAsset(assetId, this)

    private fun validateAddress(network: Network, params: ValidateAddresseesParams) = authHandler(
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

        if(!isConnected) {
            connect(network = network, initNetworks = listOf(network))
        }

        return authHandler(network, gdk.bcurEncode(gdkSession(network), params)).result<BcurEncodedData>()
    }

    suspend fun bcurDecode(params: BcurDecodeParams, bcurResolver: BcurResolver): BcurDecodedData {
        val network = defaultNetworkOrNull ?: networks.bitcoinElectrum

        if(!isConnected) {
            connect(network = network, initNetworks = listOf(network))
        }

        return authHandler(network, gdk.bcurDecode(gdkSession(network), params)).result<BcurDecodedData>(bcurResolver = bcurResolver)
    }

    suspend fun parseInput(input: String): Pair<Network, InputType?>? =
        withContext(context = Dispatchers.IO) {
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

    internal fun destroy() {
        disconnect()
        scope.cancel("Destroy")
    }

    companion object: Loggable() {
        const val WALLET_OVERVIEW_TRANSACTIONS = 20

        const val LIQUID_ASSETS_KEY = "liquid_assets"
        const val LIQUID_ASSETS_TESTNET_KEY = "liquid_assets_testnet"

        val BlockstreamPinOracleUrls = listOf(
            "https://j8d.io",
            "https://jadepin.blockstream.com",
            "http://mrrxtq6tjpbnbm7vh5jt6mpjctn7ggyfy5wegvbeff3x7jrznqawlmid.onion", // onion jadepin
            "https://jadefw.blockstream.com",
            "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion" // onion jadefw
        )
    }
}

