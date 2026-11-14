package com.blockstream.data.backend.lwk

import com.blockstream.data.backend.AbstractNetworkBackend
import com.blockstream.data.backend.AccountBackend
import com.blockstream.data.database.Database
import com.blockstream.data.database.wallet.LwkAccounts
import com.blockstream.data.gdk.AuthHandler
import com.blockstream.data.gdk.BcurResolver
import com.blockstream.data.gdk.GAAuthHandler
import com.blockstream.data.gdk.GASession
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.FeeEstimation
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.data.LoginData
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.ValidateAddressees
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.ConnectionParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.gdk.params.ValidateAddresseesParams
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.data.utils.toHex
import com.blockstream.jade.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lwk.Amp2
import lwk.Amp2Descriptor
import lwk.Bip
import lwk.DescriptorBlindingKey
import lwk.Mnemonic
import lwk.Signer
import lwk.Singlesig
import lwk.Transaction
import lwk.WolletBuilder
import lwk.WolletDescriptor
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import lwk.Network as LwkNetwork

class LwkNetworkBackend constructor(
    private val dataDir: String,
    private val database: Database,
    val gdk: Gdk,
    network: Network,
    networkAssetManager: NetworkAssetManager,
) : AbstractNetworkBackend(
    network = network,
    networkAssetManager = networkAssetManager,
) {
    private var signer: Signer? = null
    val lwkNetwork = if (network.isTestnet) LwkNetwork.testnet() else LwkNetwork.mainnet()

    private val client = LwkNetworkClient(isTestnet = network.isTestnet)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tipJob: Job? = null

    internal val amp2Client = createAmp2Client(network.isTestnet)

    val gaSession: GASession = gdk.createSession()

    private fun fingerprint() = signer!!.fingerprint()

    override suspend fun connect(params: ConnectionParams) {
        if (isConnected) return

        // Use the correct GDK network
        gdk.connect(
            gaSession, params.copy(
                networkName = gdk.networks().liquidElectrum(network.isTestnet).network
            )
        )

        state.update {
            it.copy(isConnected = true)
        }
    }

    suspend fun login(
        loginCredentialsParams: LoginCredentialsParams
    ): LoginData {
        signer?.let { existing ->
            return LoginData(xpubHashId = existing.fingerprint(), networkHashId = network.id)
        }

        val signer = Signer(Mnemonic(loginCredentialsParams.mnemonic!!), lwkNetwork)

        this.signer = signer

        state.update {
            it.copy(isLoggedIn = true)
        }

        logger.d { "LWK login complete for ${network.id}" }

        blockHeaderPolling()

        database.getAccountsFlow(fingerprint()).onEach { rows ->
            _accounts.value = rows.map { it.toAccount() }
        }.launchIn(scope)

        return LoginData(xpubHashId = signer.fingerprint(), networkHashId = network.id)
    }

    override suspend fun isAddressValid(address: String): Boolean {
        return gdk.validate(gaSession, ValidateAddresseesParams.create(network, address)).result<ValidateAddressees>().isValid
    }

    override suspend fun getAccount(account: Account): Account {
        return database.getAccount(fingerprint = fingerprint(), accountType = account.type, index = account.pointer)!!.toAccount()
    }

    private suspend fun LwkAccounts.toAccount(): Account =
        Account(
            gdkName = name,
            pointer = index,
            type = account_type,
            coreDescriptors = listOf(descriptor),
            hidden = hidden,
            receivingId = wid ?: ""
        ).also {
            it.setup(
                networkAssetManager = networkAssetManager,
                assetsProvider = this@LwkNetworkBackend,
                network = network
            )
        }

    override suspend fun getAccounts(refresh: Boolean): List<Account> = _accounts.value

    override suspend fun createAccount(params: SubAccountParams, hardwareWalletResolver: HardwareWalletResolver?): Account {
        val signer = checkNotNull(signer) { "LWK signer not initialised; login first" }

        var wId: String? = null

        require(database.getAccount(fingerprint = fingerprint(), accountType = params.type, index = 0) == null) {
            "LWK supports only one account per type"
        }

        val descriptor: WolletDescriptor = when (params.type) {
            AccountType.BIP84_SEGWIT ->
                signer.singlesigDesc(Singlesig.WPKH, DescriptorBlindingKey.SLIP77)

            AccountType.BIP49_SEGWIT_WRAPPED ->
                signer.singlesigDesc(Singlesig.SH_WPKH, DescriptorBlindingKey.SLIP77)

            AccountType.AMP2_ACCOUNT -> {
                // AMP2 wallets must be registered with the AMP2 server before they can be used.
                val amp2Descriptor = amp2Client.descriptorFromStr(
                    signer.keyoriginXpub(Bip.newBip87()),
                    signer.slip77MasterBlindingKey()
                )
                wId = amp2Client.registerWallet(amp2Descriptor)
                logger.d { "Registered AMP2 wallet $wId" }
                amp2Descriptor.descriptor()
            }

            else -> throw IllegalArgumentException("Unsupported LWK account type: ${params.type}")
        }

        // The wollet descriptor string is what the account backend rebuilds the wollet from.
        val outputDescriptor = descriptor.toString()

        val account = Account(
            gdkName = params.name,
            pointer = 0,
            networkInjected = network,
            type = params.type,
            coreDescriptors = listOf(outputDescriptor),
            receivingId = wId ?: ""
        ).also {
            it.setup(networkAssetManager = networkAssetManager, assetsProvider = this, network = network)
        }

        database.insertAccount(
            fingerprint = fingerprint(),
            accountType = params.type,
            index = 0,
            name = account.name,
            descriptor = outputDescriptor,
            wid = wId
        )

        // Replace any existing account of the same type and expose the new set.
        _accounts.value = (_accounts.value.filter { it.type != account.type } + account).sorted()

        // Eagerly build the matching backend so the account is immediately usable.
        accountBackend(account)

        return account
    }

    suspend fun updateAccount(account: Account, name: String?, hidden: Boolean?) {
        database.updateAccount(
            fingerprint = fingerprint(),
            accountType = account.type,
            index = account.pointer,
            name = name ?: account.name,
            hidden = hidden ?: account.hidden
        )
    }

    override suspend fun getFeeEstimates(): FeeEstimation =
        FeeEstimation(listOf(100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L))

    private fun blockHeaderPolling() {
        if (tipJob != null) return
        tipJob = scope.launch {
            while (coroutineContext.isActive) {
                try {
                    val header = client.tip()

                    _blockStateFlow.value = Block(
                        hash = header.blockHash(),
                        height = header.height().toLong(),
                        timestamp = header.time().toLong()
                    ).also {
                        logger.d { "Update block $it" }
                    }

                    scanBlockchain()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.w { "LWK tip poll failed: ${e.message}" }
                }
                delay(TIP_POLL_INTERVAL_SEC.toDuration(DurationUnit.SECONDS))
            }
        }
    }

    private suspend fun scanBlockchain() {
        accountBackends.values.forEach { backend ->
            if (backend is LwkAccountBackend) {
                backend.scanBlockchain()
            }
        }
    }

    override fun createAccountBackend(account: Account): AccountBackend {
        requireNotNull(signer)

        val descriptor = WolletDescriptor(account.outputDescriptors!!)

        val datadir = "$dataDir/lwk/${network.id}/${account.type.gdkType}/${account.pointer}"

        val wollet = WolletBuilder(lwkNetwork, descriptor).also {
            it.withLegacyFsStore(datadir)
        }.build()

//        val store = mutableMapOf<String, ByteArray>()

//        val wollet = Wollet.withCustomStore(network = lwkNetwork, descriptor = descriptor, store = ForeignStoreLink(object : ForeignStore{
//            override fun get(key: String): ByteArray? {
//                return store[key]
//            }
//
//            override fun put(key: String, value: ByteArray) {
//                store[key] = value
//            }
//
//            override fun remove(key: String) {
//                store.remove(key)
//            }
//        }))

        return LwkAccountBackend(
            networkBackend = this,
            networkClient = client,
            wollet = wollet,
            signer = signer!!,
            account = account,
        )
    }

    override suspend fun broadcastTransaction(broadcastTransaction: BroadcastTransactionParams): ProcessedTransactionDetails {
        requireNotNull(broadcastTransaction.transaction)

        val txId = client.broadcast(Transaction.fromString(broadcastTransaction.transaction))

        scope.launch {
            delay(1.seconds)
            scanBlockchain()
            _networkEventsFlow.emit(com.blockstream.data.backend.NetworkEvent.Transaction(accountPointers = listOf(0)))
        }

        return ProcessedTransactionDetails(
            signedTransaction = broadcastTransaction.transaction,
            txHash = txId.toString()
        )
    }

    override suspend fun disconnect() {
        val isConnected = isConnected
        super.disconnect()

        if (isConnected) {
            logger.d { "Destroying GDK session ${network.id}" }
            gdk.destroySession(gaSession)
        }

        scope.cancel()
        tipJob = null

        signer?.close()
        signer = null
    }

    override suspend fun getAssets(params: GetAssetsParams): LiquidAssets = gdk.getAssets(session = gaSession, params = params)

    override suspend fun refreshAssets(params: AssetsParams) = gdk.refreshAssets(session = gaSession, params = params)

    suspend inline fun <reified T> GAAuthHandler.result(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): T {
        return AuthHandler(
            gaAuthHandler = this,
            network = network,
            gdkHwWallet = null,
            assetsProvider = null,
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

    companion object : Loggable() {

        // TODO USE PRODUCTION KEYS
        private const val Amp2ServerKey =
            "[b805d768/87h/1h/0h]tpubDCYEgnLyCH2okSittQNNB8JHLwPgmoEAoKcMrJDHP9dFVamsadPAFJQ77C1htgR8ksie3VksLXoryng9AUaPZSF8FwTwEv6CaHp8j2YCrds"

        private const val Amp2TestnetServerKey =
            "[b805d768/87h/1h/0h]tpubDCYEgnLyCH2okSittQNNB8JHLwPgmoEAoKcMrJDHP9dFVamsadPAFJQ77C1htgR8ksie3VksLXoryng9AUaPZSF8FwTwEv6CaHp8j2YCrds"

        private const val Amp2Url = "https://amp.enterprise.blockstream.com/"
        private const val Amp2TestnetUrl = "https://amp.enterprise.blockstream.com/"

        fun createAmp2Client(isTestnet: Boolean) =
            Amp2(serverKey = if (isTestnet) Amp2TestnetServerKey else Amp2ServerKey, url = if (isTestnet) Amp2TestnetUrl else Amp2Url)

        const val TIP_POLL_INTERVAL_SEC = 20L
    }
}
