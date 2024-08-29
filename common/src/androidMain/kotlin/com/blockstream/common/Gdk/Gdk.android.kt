package com.blockstream.common.gdk

import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.common.gdk.data.FeeEstimation
import com.blockstream.common.gdk.data.LiquidAssets
import com.blockstream.common.gdk.data.LoginData
import com.blockstream.common.gdk.data.Networks
import com.blockstream.common.gdk.data.Pricing
import com.blockstream.common.gdk.data.ProxySettings
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.TwoFactorConfig
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.BalanceParams
import com.blockstream.common.gdk.params.BcurDecodeParams
import com.blockstream.common.gdk.params.BcurEncodeParams
import com.blockstream.common.gdk.params.BroadcastTransactionParams
import com.blockstream.common.gdk.params.ConnectionParams
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
import com.blockstream.green_gdk.GDK
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

class AndroidGdk(log: Boolean, config: InitConfig) : GdkBinding {
    private val _dataDir: String = config.datadir

    init {
        // Set maskSensitiveFields always as true for QA peace of mind
        GDK.init(
            GdkJsonConverter(JsonConverter(log = log, maskSensitiveFields = true)),
            config
        )
    }

    override val dataDir: String
        get() = _dataDir

    override fun setNotificationHandler(notificationHandler: (session: GASession, jsonObject: Any) -> Unit) {
        GDK.setNotificationHandler(notificationHandler)
    }

    override fun createSession(): GASession = GDK.create_session()

    override fun destroySession(session: GASession) = GDK.destroy_session(session)

    override fun connect(session: GASession, params: ConnectionParams) = GDK.connect(session, params)

    override fun reconnectHint(session: GASession, hint: ReconnectHintParams) = GDK.reconnect_hint(session, hint)

    override fun getProxySettings(session: GASession): ProxySettings {
        return JsonDeserializer.decodeFromJsonElement(GDK.get_proxy_settings(session) as JsonElement)
    }

    override fun registerUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler = GDK.register_user(session, deviceParams, loginCredentialsParams)


    override fun loginUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler = GDK.login_user(session, deviceParams, loginCredentialsParams)

    override fun getWalletIdentifier(
        connectionParams: ConnectionParams,
        loginCredentialsParams: LoginCredentialsParams
    ): LoginData {
        return JsonDeserializer.decodeFromJsonElement(
            GDK.get_wallet_identifier(
            connectionParams,
            loginCredentialsParams
        ) as JsonElement)
    }

    override fun validate(session: GASession, params: JsonElement): GAAuthHandler {
        return GDK.validate(session, params)
    }

    override fun validate(session: GASession, params: ValidateAddresseesParams): GAAuthHandler {
        return GDK.validate(session, params)
    }

    override fun encryptWithPin(
        session: GASession,
        encryptWithPinParams: EncryptWithPinParams
    ): GAAuthHandler {
        return GDK.encrypt_with_pin(session, encryptWithPinParams)
    }

    override fun decryptWithPin(
        session: GASession,
        decryptWithPinParams: DecryptWithPinParams
    ): GAAuthHandler {
        return GDK.decrypt_with_pin(session, decryptWithPinParams)
    }

    override fun getCredentials(session: GASession, params: CredentialsParams): GAAuthHandler {
        return GDK.get_credentials(session, params)
    }

    override fun getReceiveAddress(
        session: GASession,
        params: ReceiveAddressParams
    ): GAAuthHandler {
        return GDK.get_receive_address(session, params)
    }

    override fun getPreviousAddress(
        session: GASession,
        params: PreviousAddressParams
    ): GAAuthHandler {
        return GDK.get_previous_addresses(session, params)
    }

    override fun refreshAssets(session: GASession, params: AssetsParams) = GDK.refresh_assets(session, params)


    override fun getAssets(session: GASession, params: GetAssetsParams): LiquidAssets {
        return JsonDeserializer.decodeFromJsonElement(
            GDK.get_assets(
                session,
                params
            ) as JsonElement)
    }

    override fun getTransactions(session: GASession, details: TransactionParams): GAAuthHandler {
        return GDK.get_transactions(session, details)
    }

    override fun getTwoFactorConfig(session: GASession): TwoFactorConfig {
        return JsonDeserializer.decodeFromJsonElement(GDK.get_twofactor_config(session) as JsonElement)
    }

    override fun changeSettingsTwoFactor(
        session: GASession,
        method: String,
        methodConfig: TwoFactorMethodConfig
    ): GAAuthHandler {
        return GDK.change_settings_twofactor(session, method, methodConfig)
    }

    override fun getWatchOnlyUsername(session: GASession): String? {
        return GDK.get_watch_only_username(session)
    }

    override fun changeSettings(session: GASession, settings: Settings): GAAuthHandler {
        return GDK.change_settings(session, settings)
    }

    override fun setCsvTime(session: GASession, value: CsvParams): GAAuthHandler {
        return GDK.set_csvtime(session, value)
    }

    override fun getSettings(session: GASession): Settings {
        return JsonDeserializer.decodeFromJsonElement(GDK.get_settings(session) as JsonElement)
    }

    @Throws
    override fun getAvailableCurrencies(session: GASession): List<Pricing> {
        return Pricing.fromJsonElement(GDK.get_available_currencies(session) as JsonElement)
    }

    override fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): AuthHandlerStatus {
        return JsonDeserializer.decodeFromJsonElement(GDK.auth_handler_get_status(gaAuthHandler) as JsonElement)
    }

    override fun authHandlerCall(gaAuthHandler: GAAuthHandler) = GDK.auth_handler_call(gaAuthHandler)

    override fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler) = GDK.auth_handler_request_code(gaAuthHandler, method)

    override fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler) = GDK.auth_handler_resolve_code(gaAuthHandler, code)

    override fun destroyAuthHandler(gaAuthHandler: GAAuthHandler) = GDK.destroy_auth_handler(gaAuthHandler)

    override fun twoFactorReset(
        session: GASession,
        email: String,
        isDispute: Boolean
    ): GAAuthHandler {
        return GDK.twofactor_reset(session, email,
            (if(isDispute) GDK.GA_TRUE else GDK.GA_FALSE).toLong()
        )
    }

    override fun twoFactorUndoReset(session: GASession, email: String): GAAuthHandler {
        return GDK.twofactor_undo_reset(session, email)
    }

    override fun twoFactorCancelReset(session: GASession): GAAuthHandler {
        return GDK.twofactor_cancel_reset(session)
    }

    override fun twoFactorChangeLimits(session: GASession, limits: Limits): GAAuthHandler {
        return GDK.twofactor_change_limits(session, limits)
    }

    override fun bcurEncode(session: GASession, params: BcurEncodeParams): GAAuthHandler {
        return GDK.bcur_encode(session, params)
    }

    override fun bcurDecode(session: GASession, params: BcurDecodeParams): GAAuthHandler {
        return GDK.bcur_decode(session, params)
    }

    override fun sendNlocktimes(session: GASession) = GDK.send_nlocktimes(session)


    override fun getFeeEstimates(session: GASession): FeeEstimation {
        return JsonDeserializer.decodeFromJsonElement(GDK.get_fee_estimates(session) as JsonElement)
    }

    override fun getSystemMessage(session: GASession): String? {
        return GDK.get_system_message(session)
    }

    override fun ackSystemMessage(session: GASession, message: String): GAAuthHandler {
        return GDK.ack_system_message(session, message)
    }

    override fun setTransactionMemo(session: GASession, txHash: String, memo: String) {
        return GDK.set_transaction_memo(session, txHash, memo, 0)
    }

    override fun convertAmount(session: GASession, convert: JsonElement): JsonElement {
        return GDK.convert_amount(session, convert) as JsonElement
    }

    private var _cachedNetworks: Networks? = null
    override fun networks(): Networks {
        return _cachedNetworks ?: Networks.fromJsonElement(GDK.get_networks() as JsonElement).also {
            _cachedNetworks = it
        }
    }

    override fun registerNetwork(id: String, network: JsonElement) {
        GDK.register_network(id, network)
    }

    override fun blindTransaction(session: GASession, createTransaction: JsonElement): GAAuthHandler =
        GDK.blind_transaction(session, createTransaction)

    override fun signTransaction(session: GASession, createTransaction: JsonElement): GAAuthHandler =
        GDK.sign_transaction(session, createTransaction)

    override fun psbtFromJson(session: GASession, transaction: JsonElement): GAAuthHandler = GDK.psbt_from_json(session, transaction)

    override fun broadcastTransaction(session: GASession, broadcastTransactionParams: BroadcastTransactionParams): GAAuthHandler =
        GDK.broadcast_transaction(session, broadcastTransactionParams)

    override fun sendTransaction(session: GASession, transaction: JsonElement): GAAuthHandler =
        GDK.send_transaction(session, transaction)

    override fun signMessage(session: GASession, params: SignMessageParams): GAAuthHandler =
        GDK.sign_message(session, params)

    override fun createSubAccount(session: GASession, params: SubAccountParams): GAAuthHandler {
        return GDK.create_subaccount(session, params)
    }

    override fun getSubAccounts(session: GASession, params: SubAccountsParams): GAAuthHandler {
        return GDK.get_subaccounts(session, params)
    }

    override fun getSubAccount(session: GASession, index: Long): GAAuthHandler {
        return GDK.get_subaccount(session, index)
    }

    override fun updateSubAccount(
        session: GASession,
        params: UpdateSubAccountParams
    ): GAAuthHandler {
        return GDK.update_subaccount(session, params)
    }

    override fun getBalance(session: GASession, details: BalanceParams): GAAuthHandler {
        return GDK.get_balance(session, details)
    }

    override fun getUnspentOutputs(session: GASession, details: BalanceParams): GAAuthHandler {
        return GDK.get_unspent_outputs(session, details)
    }

    override fun getUnspentOutputsForPrivateKey(session: GASession, details: UnspentOutputsPrivateKeyParams): GAAuthHandler {
        return GDK.get_unspent_outputs_for_private_key(session, details)
    }

    override fun createTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return GDK.create_transaction(session, params)
    }

    override fun createRedepositTransaction(
        session: GASession,
        params: GreenJson<*>
    ): GAAuthHandler {
        return GDK.create_redeposit_transaction(session, params)
    }

    override fun createSwapTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return GDK.create_swap_transaction(session, params)
    }

    override fun completeSwapTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return GDK.complete_swap_transaction(session, params)
    }


    override fun httpRequest(session: GASession, data: JsonElement): JsonElement {
        return GDK.http_request(session, data) as JsonElement
    }

    override fun generateMnemonic12(): String = GDK.generate_mnemonic_12()

    override fun generateMnemonic24(): String = GDK.generate_mnemonic()

    override fun getRandomBytes(size: Int): ByteArray {
        return GDK.get_random_bytes(size.toLong())
    }
}



actual fun getGdkBinding(log: Boolean, config: InitConfig): GdkBinding = AndroidGdk(log, config)

actual val GA_ERROR: Int = GDK.GA_ERROR
actual val GA_RECONNECT: Int = GDK.GA_RECONNECT
actual val GA_NOT_AUTHORIZED: Int = GDK.GA_NOT_AUTHORIZED