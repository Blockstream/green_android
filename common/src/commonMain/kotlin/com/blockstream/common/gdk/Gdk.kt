package com.blockstream.common.gdk

import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.gdk.data.FeeEstimation
import com.blockstream.common.gdk.data.LiquidAssets
import com.blockstream.common.gdk.data.LoginData
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Networks
import com.blockstream.common.gdk.data.Pricing
import com.blockstream.common.gdk.data.ProxySettings
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.TwoFactorConfig
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.BalanceParams
import com.blockstream.common.gdk.params.ConnectionParams
import com.blockstream.common.gdk.params.Convert
import com.blockstream.common.gdk.params.CredentialsParams
import com.blockstream.common.gdk.params.CsvParams
import com.blockstream.common.gdk.params.DecryptWithPinParams
import com.blockstream.common.gdk.params.DeviceParams
import com.blockstream.common.gdk.params.EncryptWithPinParams
import com.blockstream.common.gdk.params.GetAssetsParams
import com.blockstream.common.gdk.params.InitConfig
import com.blockstream.common.gdk.params.Limits
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.gdk.params.PreviousAddressParams
import com.blockstream.common.gdk.params.ReceiveAddressParams
import com.blockstream.common.gdk.params.ReconnectHintParams
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.gdk.params.SubAccountsParams
import com.blockstream.common.gdk.params.TransactionParams
import com.blockstream.common.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.common.gdk.params.UpdateSubAccountParams
import com.blockstream.common.gdk.params.ValidateAddresseesParams
import com.blockstream.common.platformFileSystem
import com.russhwolf.settings.set
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okio.Path.Companion.toPath

typealias GASession = Any
typealias GAAuthHandler = Any

interface GdkBinding {
    val dataDir: String

    fun setNotificationHandler(notificationHandler: (session: GASession, jsonObject: Any) -> Unit)

    @Throws(Exception::class)
    fun createSession(): GASession

    @Throws(Exception::class)
    fun destroySession(session: GASession)
    @Throws(Exception::class)
    fun connect(session: GASession, params: ConnectionParams)

    @Throws(Exception::class)
    fun reconnectHint(session: GASession, hint: ReconnectHintParams)

    @Throws(Exception::class)
    fun getProxySettings(session: GASession): ProxySettings

    @Throws(Exception::class)
    fun registerUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun loginUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun getWalletIdentifier(connectionParams: ConnectionParams, loginCredentialsParams: LoginCredentialsParams): LoginData

    @Throws(Exception::class)
    fun validate(
        session: GASession,
        params: JsonElement
    ): GAAuthHandler

    @Throws(Exception::class)
    fun validate(
        session: GASession,
        params: ValidateAddresseesParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun encryptWithPin(
        session: GASession,
        encryptWithPinParams: EncryptWithPinParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun decryptWithPin(
        session: GASession,
        decryptWithPinParams: DecryptWithPinParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun getCredentials(session: GASession, params: CredentialsParams): GAAuthHandler

    @Throws(Exception::class)
    fun getReceiveAddress(
        session: GASession,
        params: ReceiveAddressParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun getPreviousAddress(
        session: GASession,
        params: PreviousAddressParams
    ): GAAuthHandler

    @Throws(Exception::class)
    fun refreshAssets(session: GASession, params: AssetsParams)

    @Throws(Exception::class)
    fun getAssets(session: GASession, params: GetAssetsParams): LiquidAssets

    @Throws(Exception::class)
    fun getTransactions(session: GASession, details: TransactionParams): GAAuthHandler

    @Throws(Exception::class)
    fun getTwoFactorConfig(session: GASession): TwoFactorConfig

    @Throws(Exception::class)
    fun changeSettingsTwoFactor(
        session: GASession,
        method: String,
        methodConfig: TwoFactorMethodConfig
    ): GAAuthHandler

    @Throws(Exception::class)
    fun getWatchOnlyUsername(session: GASession): String?

    @Throws(Exception::class)
    fun setWatchOnly(session: GASession, username: String, password: String)

    @Throws(Exception::class)
    fun changeSettings(session: GASession, settings: Settings): GAAuthHandler

    @Throws(Exception::class)
    fun setCsvTime(session: GASession, value: CsvParams): GAAuthHandler

    @Throws(Exception::class)
    fun getSettings(session: GASession): Settings

    @Throws(Exception::class)
    fun getAvailableCurrencies(session: GASession): List<Pricing>

    @Throws(Exception::class)
    fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): AuthHandlerStatus

    @Throws(Exception::class)
    fun authHandlerCall(gaAuthHandler: GAAuthHandler)

    @Throws(Exception::class)
    fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler)

    @Throws(Exception::class)
    fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler)

    @Throws(Exception::class)
    fun destroyAuthHandler(gaAuthHandler: GAAuthHandler)

    @Throws(Exception::class)
    fun twoFactorReset(session: GASession, email: String, isDispute: Boolean): GAAuthHandler

    @Throws(Exception::class)
    fun twoFactorUndoReset(session: GASession, email: String): GAAuthHandler

    @Throws(Exception::class)
    fun twoFactorCancelReset(session: GASession): GAAuthHandler

    @Throws(Exception::class)
    fun twoFactorChangeLimits(session: GASession, limits: Limits): GAAuthHandler

    @Throws(Exception::class)
    fun sendNlocktimes(session: GASession)

    @Throws(Exception::class)
    fun getFeeEstimates(session: GASession): FeeEstimation

    @Throws(Exception::class)
    fun getSystemMessage(session: GASession): String?

    @Throws(Exception::class)
    fun ackSystemMessage(session: GASession, message: String): GAAuthHandler

    @Throws(Exception::class)
    fun setTransactionMemo(session: GASession, txHash: String, memo: String)

    @Throws(Exception::class)
    fun convertAmount(session: GASession, amount: Convert): Balance

    @Throws(Exception::class)
    fun convertAmount(session: GASession, amount: Convert, assetConvert :JsonElement): Balance

    @Throws(Exception::class)
    fun networks(): Networks

    @Throws(Exception::class)
    fun registerNetwork(id: String, network: JsonElement)

    @Throws(Exception::class)
    fun blindTransaction(session: GASession, createTransaction: JsonElement): GAAuthHandler

    @Throws(Exception::class)
    fun signTransaction(session: GASession, createTransaction: JsonElement): GAAuthHandler

    @Throws(Exception::class)
    fun broadcastTransaction(session: GASession, transaction: String): String

    @Throws(Exception::class)
    fun sendTransaction(session: GASession, transaction: JsonElement): GAAuthHandler

    @Throws(Exception::class)
    fun signMessage(session: GASession, params: SignMessageParams): GAAuthHandler

    @Throws(Exception::class)
    fun createSubAccount(session: GASession, params: SubAccountParams): GAAuthHandler

    @Throws(Exception::class)
    fun getSubAccounts(session: GASession, params: SubAccountsParams): GAAuthHandler

    @Throws(Exception::class)
    fun getSubAccount(session: GASession, index: Long): GAAuthHandler

    @Throws(Exception::class)
    fun updateSubAccount(session: GASession, params: UpdateSubAccountParams): GAAuthHandler

    @Throws(Exception::class)
    fun getBalance(session: GASession, details: BalanceParams): GAAuthHandler

    @Throws(Exception::class)
    fun getUnspentOutputs(session: GASession, details: BalanceParams): GAAuthHandler

    @Throws(Exception::class)
    fun getUnspentOutputsForPrivateKey(session: GASession, details: UnspentOutputsPrivateKeyParams): GAAuthHandler

    @Throws(Exception::class)
    fun createTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler

    @Throws(Exception::class)
    fun createSwapTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler

    @Throws(Exception::class)
    fun completeSwapTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler

    fun httpRequest(session: GASession, data: JsonElement): JsonElement

    @Throws(Exception::class)
    fun generateMnemonic12(): String

    @Throws(Exception::class)
    fun generateMnemonic24(): String

    @Throws(Exception::class)
    fun getRandomBytes(size: Int): ByteArray
}

class Gdk constructor(
    private val settings: com.russhwolf.settings.Settings,
    private val gdkBinding: GdkBinding
) : GdkBinding by gdkBinding {

    init {
        settings.getStringOrNull(KEY_CUSTOM_NETWORK)?.also {
            try{
                val jsonElement = JsonConverter.JsonDeserializer.parseToJsonElement(it)
                val network = JsonConverter.JsonDeserializer.decodeFromJsonElement<Network>(jsonElement)
                registerNetwork(network.id, jsonElement)
                networks().setCustomNetwork(network)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun hasGdkCache(loginData: LoginData): Boolean {
        return "${gdkBinding.dataDir.toPath()}/state/${loginData.networkHashId}".toPath().let {
            platformFileSystem().exists(it)
        }
    }

    fun removeGdkCache(loginData: LoginData): Boolean {
        return "${gdkBinding.dataDir}/state/${loginData.networkHashId}".toPath().let {
            platformFileSystem().deleteRecursively(it, false)
            true
        }
    }

    fun registerCustomNetwork(originNetworkId: String, hostname: String){
        networks().getNetworkAsJsonElement(originNetworkId)?.jsonObject?.let { obj ->
            buildJsonObject {
                // Copy all fields
                for(k in obj){
                    put(k.key, k.value)
                }
                // Replace values
                put("id", Networks.CustomNetworkId)
                put("network", Networks.CustomNetworkId)
                put("wamp_url", "ws://$hostname/v2/ws") // for multisig
                put("electrum_url", hostname) // for singlesig electrum
            }.also { jsonElement ->
                val network = JsonConverter.JsonDeserializer.decodeFromJsonElement<Network>(jsonElement)
                registerNetwork(network.id, jsonElement)
                networks().setCustomNetwork(network)
                // Save settings
                settings[KEY_CUSTOM_NETWORK] = jsonElement.toString()
            }
        }
    }

    companion object {
        const val KEY_CUSTOM_NETWORK = "custom_network"
    }
}

expect fun getGdkBinding(log: Boolean, config: InitConfig): GdkBinding

val FeeBlockTarget = listOf(1, 9, 18)

expect val GA_ERROR: Int
expect val GA_RECONNECT: Int
expect val GA_NOT_AUTHORIZED: Int