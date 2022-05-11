package com.blockstream.gdk

import android.content.SharedPreferences
import android.util.SparseArray
import com.blockstream.crypto.BuildConfig
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.libgreenaddress.GAAuthHandler
import com.blockstream.libgreenaddress.GASession
import com.blockstream.libgreenaddress.GDK
import com.blockstream.libgreenaddress.KotlinGDK
import com.blockstream.libwally.KotlinWally
import kotlinx.serialization.json.*
import mu.KLogging
import java.io.File
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

typealias Balances = LinkedHashMap<String, Long>
typealias BalanceMap = Map<String, Long>
typealias BalancePair = Pair<String, Long>
typealias WalletBalances = SparseArray<Balances>

val BalanceLoading = BalancePair("", -1)

class GreenWallet constructor(
    val gdk: KotlinGDK,
    private val wally: KotlinWally,
    private val sharedPreferences: SharedPreferences,
    val dataDir: File,
    developmentFlavor: Boolean,
    val extraLogger: Logger? = null
) {

    private val bip39WordList by lazy { wally.bip39Wordlist(BIP39_WORD_LIST_LANG) }

    val networks by lazy {
        val jsonElement = gdk.getNetworks() as JsonElement
        Networks.fromJsonElement(JsonDeserializer, jsonElement)
    }

    init {
        gdk.init(
            JsonConverter(developmentFlavor, !BuildConfig.DEBUG, extraLogger),
            InitConfig(datadir = dataDir.absolutePath, logLevel = if (BuildConfig.DEBUG) "debug" else "none")
        )
        wally.init(0, randomBytes(KotlinWally.WALLY_SECP_RANDOMIZE_LEN))

        sharedPreferences.getString(KEY_CUSTOM_NETWORK, null)?.let {
            try{
                val jsonElement = JsonDeserializer.parseToJsonElement(it)
                val network = JsonDeserializer.decodeFromJsonElement<Network>(jsonElement)
                gdk.registerNetwork(network.id, jsonElement)
                networks.setCustomNetwork(network)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun randomBytes(len: Int): ByteArray {
        return ByteArray(len).also {
            SecureRandom().nextBytes(it)
        }
    }

    fun randomChars(len: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val random = SecureRandom().asKotlinRandom()
        return (1..len)
            .map { random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun setNotificationHandler(notificationHandler: GDK.NotificationHandler) =
        gdk.setNotificationHandler(notificationHandler)

    fun generateMnemonic12(): String = gdk.generateMnemonic12()
    fun generateMnemonic24(): String = gdk.generateMnemonic24()

    fun registerCustomNetwork(originNetworkId: String, hostname: String){
        networks.getNetworkAsJsonElement(originNetworkId)?.jsonObject?.let { obj ->
            buildJsonObject {
                // Copy all fields
                for(k in obj){
                    put(k.key, k.value)
                }
                // Replace values
                put("id", Networks.CustomNetworkId)
                put("network", Networks.CustomNetworkId)
                put("wamp_url", "ws://$hostname/v2/ws") // for multisig
                put("electrum_url", "$hostname") // for singlesig electrum
            }.also { jsonElement ->
                val network = JsonDeserializer.decodeFromJsonElement<Network>(jsonElement)
                gdk.registerNetwork(network.id, jsonElement)
                networks.setCustomNetwork(network)
                sharedPreferences.edit().also {
                    it.putString(KEY_CUSTOM_NETWORK, jsonElement.toString())
                }.apply()
            }
        }
    }

    fun createSession() = gdk.createSession()
    fun destroySession(session: GASession) = gdk.destroySession(session)

    fun connect(session: GASession, params: ConnectionParams) {
        gdk.connect(session, params)
    }

    fun reconnectHint(session: GASession, hint: ReconnectHintParams) = gdk.reconnectHint(session, hint)

    fun getProxySettings(session: GASession): ProxySettings = JsonDeserializer.decodeFromJsonElement(gdk.getProxySettings(session) as JsonElement)

    fun httpRequest(session: GASession, data: JsonElement) = gdk.httpRequest(session, data) as JsonElement

    fun registerUser(
        session: GASession,
        deviceParams: DeviceParams,
        mnemonic: String
    ) = gdk.registerUser(session, deviceParams, mnemonic)

    fun loginUser(session: GASession,
                  deviceParams: DeviceParams? = null,
                  loginCredentialsParams: LoginCredentialsParams? = null
    ) = gdk.loginUser(session, deviceParams, loginCredentialsParams)

    fun setPin(
        session: GASession,
        mnemonicPassphrase: String,
        pin: String,
        device: String = "default"
    ): PinData =
        JsonDeserializer.decodeFromJsonElement(
            gdk.setPin(
                session,
                mnemonicPassphrase,
                pin,
                device
            ) as JsonElement
        )

    fun getMnemonicPassphrase(session: GASession) =
        gdk.getMnemonicPassphrase(session) as String

    fun getReceiveAddress(
        session: GASession,
        params: ReceiveAddressParams
    ) = gdk.getReceiveAddress(session, params)

    fun refreshAssets(session: GASession, params: AssetsParams): Assets =
        JsonDeserializer.decodeFromJsonElement(
            gdk.refreshAssets(
                session,
                params
            ) as JsonElement
        )

    fun getTransactions(session: GASession, details: TransactionParams) =
        gdk.getTransactions(session, details)

    fun getTwoFactorConfig(session: GASession): TwoFactorConfig =
        JsonDeserializer.decodeFromJsonElement(gdk.getTwoFactorConfig(session) as JsonElement)

    fun changeSettingsTwoFactor(
        session: GASession,
        method: String,
        methodConfig: TwoFactorMethodConfig
    ) = gdk.changeSettingsTwoFactor(session, method, methodConfig)

    fun getWatchOnlyUsername(session: GASession) =
        gdk.getWatchOnlyUsername(session)

    fun setWatchOnly(session: GASession, username: String, password: String) =
        gdk.setWatchOnly(session, username, password)

    fun changeSettings(session: GASession, settings: Settings) =
        gdk.changeSettings(session, settings)

    fun setCsvTime(session: GASession, value: Int) = gdk.setCsvTime(session, buildJsonObject {
            put("value", value)
        })

    fun getAvailableCurrencies(session: GASession): List<Pricing> = Pricing.fromJsonElement(gdk.getAvailableCurrencies(session) as JsonElement)

    fun getSettings(session: GASession): Settings =
        JsonDeserializer.decodeFromJsonElement(gdk.getSettings(session) as JsonElement)

    fun getBalance(session: GASession, details: BalanceParams) = gdk.getBalance(session, details)

    fun getUnspentOutputs(session: GASession, details: BalanceParams) = gdk.getUnspentOutputs(session, details)

    fun createTransaction(session: GASession, params: GAJson<*>) = gdk.createTransaction(session, params)

    fun updateTransaction(session: GASession, createTransaction: JsonElement) = gdk.updateTransaction(session, createTransaction)

    fun signTransaction(session: GASession, createTransaction: JsonElement) = gdk.signTransaction(session, createTransaction)

    fun broadcastTransaction(session: GASession, transaction: String) = gdk.broadcastTransaction(session, transaction)

    fun sendTransaction(session: GASession, createTransaction: JsonElement) = gdk.sendTransaction(session, createTransaction)

    fun createSubAccount(session: GASession, params: SubAccountParams) = gdk.createSubAccount(
        session,
        params
    )

    fun getSubAccounts(session: GASession, params: SubAccountsParams) = gdk.getSubAccounts(session, params)
    fun getSubAccount(session: GASession, index: Long) = gdk.getSubAccount(session, index)

    fun updateSubAccount(session: GASession, params: UpdateSubAccountParams) =
        gdk.updateSubAccount(session, params)

    fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): JsonElement =
        gdk.getAuthHandlerStatus(gaAuthHandler)

    fun authHandlerCall(gaAuthHandler: GAAuthHandler) = gdk.authHandlerCall(gaAuthHandler)
    fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler) =
        gdk.authHandlerRequestCode(method, gaAuthHandler)

    fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler) =
        gdk.authHandlerResolveCode(code, gaAuthHandler)

    fun destroyAuthHandler(gaAuthHandler: GAAuthHandler) = gdk.destroyAuthHandler(gaAuthHandler)

    fun twofactorReset(session: GASession, email: String, isDispute: Boolean) = gdk.twofactorReset(session, email, isDispute)

    fun twofactorUndoReset(session: GASession, email: String) = gdk.twofactorUndoReset(session, email)

    fun twofactorCancelReset(session: GASession) = gdk.twofactorCancelReset(session)

    fun twofactorChangeLimits(session: GASession, limits: Limits) =
        gdk.twofactorChangeLimits(session, limits)

    fun sendNlocktimes(session: GASession) = gdk.sendNlocktimes(session)

    fun getFeeEstimates(session: GASession) = JsonDeserializer.decodeFromJsonElement<FeeEstimation>(gdk.getFeeEstimates(session) as JsonElement)

    fun getSystemMessage(session: GASession) = gdk.getSystemMessage(session)

    fun ackSystemMessage(session: GASession, message: String) = gdk.ackSystemMessage(session, message)

    fun setTransactionMemo(session: GASession, txHash: String, memo: String) = gdk.setTransactionMemo(session, txHash, memo)

    fun convertAmount(session: GASession, amount: Convert): Balance = Balance.fromJsonElement(
        JsonDeserializer,
        gdk.convertAmount(session, amount) as JsonElement,
        amount
    )

    fun convertAmount(session: GASession, amount: Convert, assetConvert :JsonElement): Balance = Balance.fromJsonElement(
        JsonDeserializer,
        gdk.convertAmount(session, assetConvert) as JsonElement,
        amount
    )

    fun getMnemonicWordList(): List<String> {
        val wordList = mutableListOf<String>()
        val enWords = bip39WordList
        for (i in 0 until wally.BIP39_WORDLIST_LEN) {
            wordList += wally.bip39Word(enWords, i)
        }

        return wordList
    }

    fun isMnemonicValid(mnemonic: String): Boolean {
        return wally.bip39MnemonicValidate(bip39WordList, mnemonic)
    }

    fun isXpubValid(xpub: String): Boolean {
        try {
            wally.bip32KeyFromBase58(xpub)
            return true
        }catch (e: Exception){
            e.printStackTrace()
        }
        return false
    }

    companion object: KLogging(){
        /**
         * Serialization / Deserialization JSON Options
         */
        val JsonDeserializer = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        const val BIP39_WORD_LIST_LANG = "en"

        const val KEY_CUSTOM_NETWORK = "custom_network"

        val FeeBlockTarget = listOf(3, 12, 24)
    }
}