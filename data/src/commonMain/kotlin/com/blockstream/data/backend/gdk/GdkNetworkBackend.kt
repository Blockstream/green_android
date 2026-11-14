package com.blockstream.data.backend.gdk

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.backend.AbstractNetworkBackend
import com.blockstream.data.backend.AccountBackend
import com.blockstream.data.backend.AmountConverter
import com.blockstream.data.backend.NetworkBackend
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.AuthHandler
import com.blockstream.data.gdk.BcurResolver
import com.blockstream.data.gdk.GAAuthHandler
import com.blockstream.data.gdk.GASession
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Accounts
import com.blockstream.data.gdk.data.BcurDecodedData
import com.blockstream.data.gdk.data.BcurEncodedData
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.CreateSwapTransaction
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.EncryptWithPin
import com.blockstream.data.gdk.data.FeeEstimation
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.data.LoginData
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.NetworkEvent
import com.blockstream.data.gdk.data.Notification
import com.blockstream.data.gdk.data.Pricing
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.ProxySettings
import com.blockstream.data.gdk.data.Psbt
import com.blockstream.data.gdk.data.RsaVerify
import com.blockstream.data.gdk.data.Settings
import com.blockstream.data.gdk.data.SignMessage
import com.blockstream.data.gdk.data.TwoFactorConfig
import com.blockstream.data.gdk.data.TwoFactorMethodConfig
import com.blockstream.data.gdk.data.TwoFactorReset
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.data.ValidateAddressees
import com.blockstream.data.gdk.device.GdkHardwareWallet
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.BcurDecodeParams
import com.blockstream.data.gdk.params.BcurEncodeParams
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.CompleteSwapParams
import com.blockstream.data.gdk.params.ConnectionParams
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
import com.blockstream.data.gdk.params.SubAccountsParams
import com.blockstream.data.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.data.gdk.params.ValidateAddresseesParams
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.jade.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement

open class GdkNetworkBackend constructor(
    val gdk: Gdk,
    val gdkHwWallet: GdkHardwareWallet?,
    network: Network,
    networkAssetManager: NetworkAssetManager
) : AbstractNetworkBackend(network = network, networkAssetManager = networkAssetManager), AssetsProvider, AmountConverter {

    private var isWatchOnly: Boolean = false


    val settings: StateFlow<Settings?>
        field = MutableStateFlow<Settings?>(null)

    val twoFactorConfig: StateFlow<TwoFactorConfig?>
        field = MutableStateFlow<TwoFactorConfig?>(null)

    val twoFactorReset: StateFlow<TwoFactorReset?>
        field = MutableStateFlow<TwoFactorReset?>(null)

    val systemMessage: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val watchOnlyUsername: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val networkEvents: SharedFlow<NetworkEvent>
        field = MutableSharedFlow<NetworkEvent>(extraBufferCapacity = 8)

    val expired2FA: StateFlow<List<Account>>
        field = MutableStateFlow<List<Account>>(emptyList())

    override suspend fun isAddressValid(address: String): Boolean {
        return gdk.validate(gaSession, ValidateAddresseesParams.create(network, address)).result<ValidateAddressees>().isValid
    }

    val gaSession: GASession = gdk.createSession()

    override fun createAccountBackend(account: Account): AccountBackend = GdkAccountBackend(
        networkBackend = this,
        gdk = gdk,
        gdkHwWallet = gdkHwWallet,
        account = account,
        gaSession = gaSession
    )

    override suspend fun connect(params: ConnectionParams) {
        if (isConnected) return

        gdk.connect(gaSession, params)

        state.update {
            it.copy(isConnected = true)
        }
    }

    suspend fun encryptWithPin(
        encryptWithPinParams: EncryptWithPinParams
    ): EncryptWithPin {
        return gdk.encryptWithPin(
            session = gaSession,
            encryptWithPinParams = encryptWithPinParams
        ).result<EncryptWithPin>().also {
            it.networkInjected = (network)
        }
    }

    override suspend fun getAccount(account: Account): Account {
        return gdk.getSubAccount(
            session = gaSession,
            index = account.pointer
        ).result<Account>().also {
            it.setup(networkAssetManager, this, account.network)
        }
    }

    open suspend fun getSystemMessage(): String? =
        gdk.getSystemMessage(session = gaSession).also { systemMessage.value = it?.takeIf { msg -> msg.isNotBlank() } }

    suspend fun getWatchOnlyUsername(): String? =
        gdk.getWatchOnlyUsername(session = gaSession).also { watchOnlyUsername.value = it }

    suspend fun setTransactionMemo(
        txHash: String, memo: String
    ) {
        gdk.setTransactionMemo(
            session = gaSession,
            txHash = txHash,
            memo = memo
        )

        _networkEventsFlow.emit(
            com.blockstream.data.backend.NetworkEvent.Transaction(accountPointers = emptyList())
        )
    }

    suspend fun getCredentials(
        params: CredentialsParams = CredentialsParams()
    ): Credentials {
        return gdk.getCredentials(
            session = gaSession,
            params = params
        ).result<Credentials>()
    }

    override suspend fun broadcastTransaction(
        broadcastTransaction: BroadcastTransactionParams
    ): ProcessedTransactionDetails {
        return gdk.broadcastTransaction(
            session = gaSession,
            broadcastTransactionParams = broadcastTransaction
        ).result<ProcessedTransactionDetails>()
    }

    suspend fun bcurEncode(
        params: BcurEncodeParams
    ): BcurEncodedData {
        return gdk.bcurEncode(
            session = gaSession,
            params = params
        ).result<BcurEncodedData>()
    }

    suspend fun bcurDecode(
        params: BcurDecodeParams, bcurResolver: BcurResolver
    ): BcurDecodedData {
        return gdk.bcurDecode(
            session = gaSession,
            params = params
        ).result<BcurDecodedData>(bcurResolver = bcurResolver)
    }

    open suspend fun login(
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams,
        hardwareWalletResolver: HardwareWalletResolver?
    ): LoginData {

        isWatchOnly = loginCredentialsParams.isWatchOnly

        return gdk.loginUser(
            session = gaSession,
            deviceParams = deviceParams,
            loginCredentialsParams = loginCredentialsParams
        ).result<LoginData>(hardwareWalletResolver = hardwareWalletResolver).also {
            state.update {
                it.copy(isLoggedIn = true)
            }

            getAccounts()
        }
    }

    private suspend fun reLogin(): LoginData {
        return gdk.loginUser(
            session = gaSession,
            deviceParams = DeviceParams.fromDeviceOrEmpty(gdkHwWallet?.device),
            loginCredentialsParams = LoginCredentialsParams.empty
        ).result<LoginData>().also {
            authenticationRequired = false

            getAccounts()
        }
    }

    // Deprecated
    open suspend fun register(
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams,
        hardwareWalletResolver: HardwareWalletResolver?
    ) {

        gdk.registerUser(
            session = gaSession,
            deviceParams = deviceParams,
            loginCredentialsParams = loginCredentialsParams
        ).resolve(hardwareWalletResolver = hardwareWalletResolver)
    }

    suspend fun createWatchOnly(username: String, password: String): LoginData {
        return gdk.registerUser(
            session = gaSession,
            deviceParams = DeviceParams.Empty,
            loginCredentialsParams = LoginCredentialsParams(
                username = username,
                password = password
            )
        ).result<LoginData>()
    }

    override suspend fun getAccounts(refresh: Boolean): List<Account> {
        val accounts = gdk.getSubAccounts(
            session = gaSession,
            params = SubAccountsParams(refresh = if (isWatchOnly) false else refresh)
        ).result<Accounts>().accounts.onEach { account ->
            account.setup(
                networkAssetManager = networkAssetManager,
                assetsProvider = this,
                network = network
            )
        }.let { transformAccounts(it) }

        _accounts.value = accounts
        return accounts
    }

    /**
     * Hook for subclasses to adjust the fetched accounts before they are published.
     * The transformed list is what both the return value and [accounts] expose.
     */
    protected open suspend fun transformAccounts(accounts: List<Account>): List<Account> = accounts

    override suspend fun createAccount(
        params: SubAccountParams,
        hardwareWalletResolver: HardwareWalletResolver?
    ): Account  = backendContext{
        gdk.createSubAccount(
            session = gaSession,
            params = params
        ).result<Account>(hardwareWalletResolver = hardwareWalletResolver).also {
            it.setup(
                networkAssetManager = networkAssetManager,
                assetsProvider = this@GdkNetworkBackend,
                network = network
            )

            getAccounts()
        }
    }

    suspend fun setCsvTime(
        value: CsvParams, twoFactorResolver: TwoFactorResolver
    ) = backendContext {
        gdk.setCsvTime(
            session = gaSession,
            value = value
        ).resolve(twoFactorResolver = twoFactorResolver)
    }

    suspend fun sendNlocktimes() = backendContext {
        gdk.sendNlocktimes(session = gaSession)
    }

    suspend fun changeSettings(settings: Settings) = backendContext { gdk.changeSettings(gaSession, settings).resolve() }

    suspend fun ackSystemMessage(message: String) = backendContext {
        gdk.ackSystemMessage(gaSession, message).resolve().also { systemMessage.value = null }
    }

    suspend fun decryptCredentialsWithPin(params: DecryptWithPinParams): Credentials = backendContext {
        gdk.decryptWithPin(gaSession, params).result()
    }

    suspend fun getUnspentOutputsForPrivateKey(params: UnspentOutputsPrivateKeyParams): UnspentOutputs = backendContext {
        gdk.getUnspentOutputsForPrivateKey(gaSession, params).result()
    }

    suspend fun createRedepositTransaction(params: CreateTransactionParams): CreateTransaction =
        gdk.createRedepositTransaction(gaSession, params).result()

    suspend fun createSwapTransaction(params: CreateSwapParams, twoFactorResolver: TwoFactorResolver): CreateSwapTransaction =
        gdk.createSwapTransaction(gaSession, params).result(twoFactorResolver = twoFactorResolver)

    suspend fun completeSwapTransaction(params: CompleteSwapParams, twoFactorResolver: TwoFactorResolver): CreateTransaction =
        gdk.completeSwapTransaction(gaSession, params).result(twoFactorResolver = twoFactorResolver)

    suspend fun signMessage(params: SignMessageParams, hardwareWalletResolver: HardwareWalletResolver? = null): SignMessage =
        gdk.signMessage(gaSession, params = params).result(hardwareWalletResolver = hardwareWalletResolver)

    open suspend fun blindTransaction(createTransaction: CreateTransaction): CreateTransaction =
        gdk.blindTransaction(gaSession, createTransaction = createTransaction.jsonElement!!).result()

    suspend fun psbtFromJson(transaction: CreateTransaction): Psbt =
        gdk.psbtFromJson(gaSession, transaction = transaction.jsonElement!!).result()

    open fun getSettings(): Settings = gdk.getSettings(gaSession).also { settings.value = it }

    open fun getTwoFactorConfig(): TwoFactorConfig = gdk.getTwoFactorConfig(gaSession).also {
        twoFactorConfig.value = it
        twoFactorReset.value = it.twoFactorReset
    }

    suspend fun scanExpired2FA() {
        if (isWatchOnly || !network.isMultisig) {
            expired2FA.value = emptyList()
            return
        }

        // No 2FA-activation guard, to match iOS. Contained per account because this also runs
        // inline in the block notification handler, which drives balance updates after it.
        expired2FA.value = accounts.value.filter { account ->
            account.type == AccountType.STANDARD && tryCatch {
                (accountBackend(account) as GdkAccountBackend).getUnspentOutputs(
                    isBump = false,
                    isExpired = true,
                    expiredAt = blockStateFlow.value.height
                ).unspentOutputs.isNotEmpty()
            } == true
        }
    }

    suspend fun getAvailableCurrencies(): List<Pricing> = backendContext {
        gdk.getAvailableCurrencies(gaSession)
    }

    suspend fun getProxySettings(): ProxySettings = backendContext { gdk.getProxySettings(gaSession) }

    suspend fun httpRequest(data: JsonElement): JsonElement = backendContext { gdk.httpRequest(gaSession, data) }

    override suspend fun getFeeEstimates(): FeeEstimation = gdk.getFeeEstimates(gaSession).let { estimation ->
        // Temp fix: liquid singlesig can return zero fees
        if (network.isSinglesig && network.isLiquid) {
            FeeEstimation(estimation.fees.map { it.coerceAtLeast(100L) })
        } else {
            estimation
        }
    }.also {
        logger.d { "FeeEstimation: ${network.id} $it" }
    }

    override suspend fun convertAmount(convert: JsonElement): JsonElement = backendContext { gdk.convertAmount(gaSession, convert) }

    suspend fun changeSettingsTwoFactor(
        method: String,
        methodConfig: TwoFactorMethodConfig,
        twoFactorResolver: TwoFactorResolver
    ) = backendContext {
        gdk.changeSettingsTwoFactor(gaSession, method, methodConfig)
            .resolve(twoFactorResolver = twoFactorResolver)
    }

    suspend fun twoFactorReset(
        email: String,
        isDispute: Boolean,
        twoFactorResolver: TwoFactorResolver
    ): TwoFactorReset =
        gdk.twoFactorReset(gaSession, email, isDispute)
            .result(twoFactorResolver = twoFactorResolver)

    suspend fun twoFactorUndoReset(email: String, twoFactorResolver: TwoFactorResolver) {
        gdk.twoFactorUndoReset(gaSession, email)
            .resolve(twoFactorResolver = twoFactorResolver)
    }

    suspend fun twoFactorCancelReset(twoFactorResolver: TwoFactorResolver) {
        gdk.twoFactorCancelReset(gaSession)
            .resolve(twoFactorResolver = twoFactorResolver)
    }

    suspend fun twoFactorChangeLimits(limits: Limits, twoFactorResolver: TwoFactorResolver): Limits =
        gdk.twoFactorChangeLimits(gaSession, limits)
            .result(twoFactorResolver = twoFactorResolver)

    suspend fun rsaVerify(params: RsaVerifyParams): RsaVerify =
        gdk.rsaVerify(gaSession, params).result()

    override suspend fun getAssets(params: GetAssetsParams): LiquidAssets = gdk.getAssets(session = gaSession, params = params)

    override suspend fun refreshAssets(params: AssetsParams) = gdk.refreshAssets(session = gaSession, params = params)

    private var authenticationRequired = false

    suspend fun onNewNotification(notification: Notification) {
        when (notification.event) {
            "block" -> {
                notification.block?.let {
                    // SingleSig after connect immediately sends a block with height 0
                    // it's not safe to call getTransactions so early
                    if (it.height > 0) {
                        _blockStateFlow.value = it

                        _networkEventsFlow.emit(
                            com.blockstream.data.backend.NetworkEvent.Block(height = it.height)
                        )

                        scanExpired2FA()
                    }
                }
            }

            "network" -> {
                notification.network?.let { event ->
                    if (isConnected) {
                        if (event.isConnected && authenticationRequired) {
                            tryCatch {
                                reLogin()
                            }
                        } else if (!event.isConnected) {
                            // mark re-authentication is required
                            authenticationRequired = true
                        }
                    }
                    networkEvents.emit(event)
                }
            }

            "settings" -> {
                notification.settings?.let { settings.value = it }
            }

            "twofactor_reset" -> {
                notification.twoFactorReset?.let { twoFactorReset.value = it }
            }

            "subaccount" -> {
                if(notification.subaccount?.isSynced == true){
                    _networkEventsFlow.emit(
                        com.blockstream.data.backend.NetworkEvent.Synced(accountPointer = notification.subaccount.pointer)
                    )
                }
            }

            "transaction" -> {
                notification.transaction?.let { event ->
                    _networkEventsFlow.emit(
                        com.blockstream.data.backend.NetworkEvent.Transaction(accountPointers = event.subaccounts)
                    )
                }
            }
        }
    }

    suspend inline fun <reified T> GAAuthHandler.result(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): T {
        return AuthHandler(
            gaAuthHandler = this,
            network = network,
            gdkHwWallet = gdkHwWallet,
            assetsProvider = this@GdkNetworkBackend,
            gdk = gdk,
            getTwoFactorConfig = {
                gdk.getTwoFactorConfig(gaSession)
            }
        ).result(
            twoFactorResolver = twoFactorResolver,
            hardwareWalletResolver = hardwareWalletResolver,
            bcurResolver = bcurResolver
        )
    }

    suspend fun GAAuthHandler.resolve(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): AuthHandler {
        return AuthHandler(
            gaAuthHandler = this,
            network = network,
            gdkHwWallet = gdkHwWallet,
            assetsProvider = this@GdkNetworkBackend,
            gdk = gdk,
            getTwoFactorConfig = {
                gdk.getTwoFactorConfig(gaSession)
            }
        ).resolve(
            twoFactorResolver = twoFactorResolver,
            hardwareWalletResolver = hardwareWalletResolver,
            bcurResolver = bcurResolver
        )
    }

    override suspend fun disconnect() {
        val isConnected = isConnected
        super.disconnect()

        if (isConnected) {
            logger.d { "Destroying GDK session ${network.id}" }
            gdk.destroySession(gaSession)
        }
    }

    private suspend fun <T> backendContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(context = Dispatchers.Default) {
            block()
        }
    }

    companion object : Loggable()
}