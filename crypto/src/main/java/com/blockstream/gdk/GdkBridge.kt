package com.blockstream.gdk

import android.content.SharedPreferences
import com.blockstream.crypto.BuildConfig
import com.blockstream.gdk.data.Assets
import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.data.FeeEstimation
import com.blockstream.gdk.data.LoginData
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.Networks
import com.blockstream.gdk.data.Pricing
import com.blockstream.gdk.data.ProxySettings
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.TwoFactorConfig
import com.blockstream.gdk.data.TwoFactorMethodConfig
import com.blockstream.gdk.params.AssetsParams
import com.blockstream.gdk.params.BalanceParams
import com.blockstream.gdk.params.ConnectionParams
import com.blockstream.gdk.params.Convert
import com.blockstream.gdk.params.CredentialsParams
import com.blockstream.gdk.params.DecryptWithPinParams
import com.blockstream.gdk.params.DeviceParams
import com.blockstream.gdk.params.EncryptWithPinParams
import com.blockstream.gdk.params.GetAssetsParams
import com.blockstream.gdk.params.InitConfig
import com.blockstream.gdk.params.Limits
import com.blockstream.gdk.params.LoginCredentialsParams
import com.blockstream.gdk.params.PreviousAddressParams
import com.blockstream.gdk.params.ReceiveAddressParams
import com.blockstream.gdk.params.ReconnectHintParams
import com.blockstream.gdk.params.SubAccountParams
import com.blockstream.gdk.params.SubAccountsParams
import com.blockstream.gdk.params.TransactionParams
import com.blockstream.gdk.params.UpdateSubAccountParams
import com.blockstream.libgreenaddress.GAAuthHandler
import com.blockstream.libgreenaddress.GASession
import com.blockstream.libgreenaddress.GDK
import com.blockstream.libgreenaddress.KotlinGDK
import com.blockstream.libwally.KotlinWally
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import mu.KLogging
import java.io.File
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

const val BTC_POLICY_ASSET = "btc"

class GdkBridge constructor(
    private val gdk: KotlinGDK,
    private val wally: KotlinWally,
    private val sharedPreferences: SharedPreferences,
    private val dataDir: File,
    isDevelopment: Boolean,
    extraLogger: Logger? = null
) {

    private val bip39WordList by lazy { wally.bip39Wordlist(BIP39_WORD_LIST_LANG) }

    val networks by lazy {
        val jsonElement = gdk.getNetworks() as JsonElement
        Networks.fromJsonElement(JsonDeserializer, jsonElement)
    }

    init {
        gdk.init(
            JsonConverter(isDevelopment, !BuildConfig.DEBUG, extraLogger),
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
        deviceParams: DeviceParams = DeviceParams(),
        loginCredentialsParams: LoginCredentialsParams
    ) = gdk.registerUser(session, deviceParams, loginCredentialsParams)

    fun loginUser(session: GASession,
                  deviceParams: DeviceParams = DeviceParams(),
                  loginCredentialsParams: LoginCredentialsParams
    ) = gdk.loginUser(session, deviceParams, loginCredentialsParams)

    fun getWalletIdentifier(
        connectionParams: ConnectionParams,
        loginCredentialsParams: LoginCredentialsParams
    ): LoginData = JsonDeserializer.decodeFromJsonElement(
        gdk.getWalletIdentifier(
            connectionParams,
            loginCredentialsParams
        ) as JsonElement
    )

    fun validate(
        session: GASession,
        params: JsonElement
    ) = gdk.validate(session, params)

    fun encryptWithPin(
        session: GASession,
        encryptWithPinParams: EncryptWithPinParams
    ) = gdk.encryptWithPin(session, encryptWithPinParams)

    fun decryptWithPin(
        session: GASession,
        decryptWithPinParams: DecryptWithPinParams
    ) = gdk.decryptWithPin(session, decryptWithPinParams)

    fun getCredentials(session: GASession, params: CredentialsParams) = gdk.getCredentials(session, params)

    fun getReceiveAddress(
        session: GASession,
        params: ReceiveAddressParams
    ) = gdk.getReceiveAddress(session, params)

    fun getPreviousAddress(
        session: GASession,
        params: PreviousAddressParams
    ) = gdk.getPreviousAddresses(session, params)

    fun refreshAssets(session: GASession, params: AssetsParams) {
        gdk.refreshAssets(
            session,
            params
        )
    }

    fun getAssets(session: GASession, params: GetAssetsParams): Assets =
        JsonDeserializer.decodeFromJsonElement(
            gdk.getAssets(
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

    fun createSwapTransaction(session: GASession, params: GAJson<*>) = gdk.createSwapTransaction(session, params)

    fun completeSwapTransaction(session: GASession, params: GAJson<*>) = gdk.completeSwapTransaction(session, params)

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

    fun hasGdkCache(loginData: LoginData): Boolean {
        return File(dataDir, "state/${loginData.networkHashId}").exists()
    }

    fun removeGdkCache(loginData: LoginData): Boolean {
        return removeGdkCache(loginData.networkHashId)
    }

    fun removeGdkCache(networkHashId: String): Boolean {
        return File(dataDir, "state/${networkHashId}").deleteRecursively()
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

        // val FeeBlockTarget = listOf(3, 12, 24) // Old calculations
        val FeeBlockTarget = listOf(1, 9, 18)
    }
}