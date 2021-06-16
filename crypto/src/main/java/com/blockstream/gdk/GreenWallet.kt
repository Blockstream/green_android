package com.blockstream.gdk

import com.blockstream.crypto.BuildConfig
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.*
import com.blockstream.libgreenaddress.GAAuthHandler
import com.blockstream.libgreenaddress.GASession
import com.blockstream.libgreenaddress.GDK
import com.blockstream.libgreenaddress.KotlinGDK
import com.blockstream.libwally.KotlinWally
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

typealias BalanceMap = Map<String, Long>
typealias BalancePair = Pair<String, Long>

class GreenWallet(
    val gdk: KotlinGDK,
    private val wally: KotlinWally,
    dataDir: String,
    developmentFlavor: Boolean
) {

    private val bip39WordList by lazy { wally.bip39Wordlist(BIP39_WORD_LIST_LANG) }

    init {
        gdk.init(JsonConverter(developmentFlavor, !BuildConfig.DEBUG), InitConfig(dataDir))
        wally.init(0, randomBytes(KotlinWally.WALLY_SECP_RANDOMIZE_LEN))
    }

    fun randomBytes(len: Int): ByteArray {
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
            .joinToString("");


    }

    fun setNotificationHandler(notificationHandler: GDK.NotificationHandler) =
        gdk.setNotificationHandler(notificationHandler)

    val networks by lazy {
        val jsonElement = gdk.getNetworks() as JsonElement
        Networks.fromJsonElement(JsonDeserializer, jsonElement)
    }

    fun generateMnemonic12(): String = gdk.generateMnemonic12()
    fun generateMnemonic24(): String = gdk.generateMnemonic24()

    fun createSession() = gdk.createSession()

    // Destroing a session can be dangerous if of some reason the session is reused. eg in rx thread
    // fun destroySession(session: GASession) = gdk.destroySession(session)

    fun connect(session: GASession, params: ConnectionParams) {
        gdk.connect(session, params)
    }

    fun reconnectHint(session: GASession) = gdk.reconnectHint(session, ReconnectHintParams())

    fun disconnect(session: GASession) {
        gdk.disconnect(session)
    }

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

    @Deprecated("Use GA_login_user", ReplaceWith("gdk.loginUser(session, loginParams)"))
    fun loginWatchOnly(session: GASession, username: String, password: String) {
        gdk.loginWatchOnly(session, username, password)
    }

    @Deprecated("Use GA_login_user", ReplaceWith("gdk.loginUser(session, loginParams)"))
    fun loginWithMnemonic(
        session: GASession,
        deviceParams: DeviceParams?,
        mnemonic: String,
        password: String
    ) = gdk.loginWithMnemonic(session, deviceParams, mnemonic, password)

    @Deprecated("Use GA_login_user", ReplaceWith("gdk.loginUser(session, loginParams)"))
    fun loginWithPin(
        session: GASession,
        pin: String,
        pinData: PinData,
    ) = gdk.loginWithPin(session, pin, pinData)

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

    fun getAvailableCurrencies(session: GASession): List<Pricing> =
        Pricing.fromJsonElement(gdk.getAvailableCurrencies(session) as JsonElement)

    fun getSettings(session: GASession): Settings =
        JsonDeserializer.decodeFromJsonElement(gdk.getSettings(session) as JsonElement)

    fun getBalance(session: GASession, details: BalanceParams) = gdk.getBalance(session, details)

    fun createSubAccount(session: GASession, params: SubAccountParams) = gdk.createSubAccount(
        session,
        params
    )

    fun getSubAccounts(session: GASession) = gdk.getSubAccounts(session)
    fun getSubAccount(session: GASession, index: Long) = gdk.getSubAccount(session, index)
    fun renameSubAccount(session: GASession, index: Long, name: String) =
        gdk.renameSubAccount(session, index, name)

    fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): JsonElement =
        gdk.getAuthHandlerStatus(gaAuthHandler)

    fun authHandlerCall(gaAuthHandler: GAAuthHandler) = gdk.authHandlerCall(gaAuthHandler)
    fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler) =
        gdk.authHandlerRequestCode(method, gaAuthHandler)

    fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler) =
        gdk.authHandlerResolveCode(code, gaAuthHandler)

    fun destroyAuthHandler(gaAuthHandler: GAAuthHandler) = gdk.destroyAuthHandler(gaAuthHandler)

    fun twofactorCancelReset(session: GASession) = gdk.twofactorCancelReset(session)

    fun twofactorChangeLimits(session: GASession, limits: Limits) =
        gdk.twofactorChangeLimits(session, limits)

    fun convertAmount(session: GASession, amount: Convert): Balance = Balance.fromJsonElement(
        JsonDeserializer,
        gdk.convertAmount(session, amount) as JsonElement,
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

    companion object {
        /**
         * Serialization / Deserialization JSON Options
         */
        val JsonDeserializer = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        const val BIP39_WORD_LIST_LANG = "en"
    }
}