package com.blockstream.libgreenaddress

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.TwoFactorMethodConfig
import com.blockstream.gdk.params.*
import kotlinx.serialization.json.JsonElement

typealias GASession = Any
typealias GAAuthHandler = Any

class KotlinGDK {
    fun init(converter: GDK.JSONConverter, config: InitConfig) = GDK.init(converter, config)

    fun setNotificationHandler(notificationHandler: GDK.NotificationHandler) {
        GDK.setNotificationHandler(notificationHandler)
    }

    fun createSession(): GASession = GDK.create_session()
    fun destroySession(session: GASession) = GDK.destroy_session(session)

    fun connect(session: GASession, params: ConnectionParams) = GDK.connect(session, params)
    fun reconnectHint(session: GASession, hint: ReconnectHintParams) = GDK.reconnect_hint(session, hint)

    fun getProxySettings(session: GASession) = GDK.get_proxy_settings(session)

    fun httpRequest(session: GASession, data: JsonElement) = GDK.http_request(session, data)

    fun registerUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler =
        GDK.register_user(session, deviceParams, loginCredentialsParams)

    fun loginUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler = GDK.login_user(session, deviceParams, loginCredentialsParams)

    fun validate(session: GASession, params: JsonElement): GAAuthHandler = GDK.validate(session, params)

    fun encryptWithPin(session: GASession, params: EncryptWithPinParams): GAAuthHandler = GDK.encrypt_with_pin(session, params)

    fun decryptWithPin(session: GASession, params: DecryptWithPinParams): GAAuthHandler = GDK.decrypt_with_pin(session, params)

    fun getCredentials(session: GASession, params: CredentialsParams) = GDK.get_credentials(session, params)

    fun getReceiveAddress(session: GASession, params: ReceiveAddressParams): GAAuthHandler =
        GDK.get_receive_address(session, params)

    fun refreshAssets(session: GASession, params: AssetsParams) =
        GDK.refresh_assets(session, params)

    fun createSubAccount(session: GASession, params: SubAccountParams): GAAuthHandler =
        GDK.create_subaccount(session, params)

    fun getSubAccounts(session: GASession, params: SubAccountsParams): GAAuthHandler = GDK.get_subaccounts(session, params)
    fun getSubAccount(session: GASession, index: Long): GASession =
        GDK.get_subaccount(session, index)

    fun updateSubAccount(session: GASession, params: UpdateSubAccountParams) = GDK.update_subaccount(session, params)

    fun getBalance(session: GASession, details: BalanceParams): GAAuthHandler =
        GDK.get_balance(session, details)

    fun getUnspentOutputs(session: GASession, details: BalanceParams): GAAuthHandler =
        GDK.get_unspent_outputs(session, details)

    fun createTransaction(session: GASession, params: GAJson<*>): GAAuthHandler =
        GDK.create_transaction(session, params)

    fun updateTransaction(session: GASession, createTransaction: JsonElement): GAAuthHandler =
        GDK.create_transaction(session, createTransaction)

    fun signTransaction(session: GASession, createTransaction: JsonElement): GAAuthHandler =
        GDK.sign_transaction(session, createTransaction)

    fun broadcastTransaction(session: GASession, transaction: String): String =
        GDK.broadcast_transaction(session, transaction)

    fun sendTransaction(session: GASession, transaction: JsonElement): GAAuthHandler =
        GDK.send_transaction(session, transaction)

    fun getTransactions(session: GASession, details: TransactionParams): GAAuthHandler =
        GDK.get_transactions(session, details)

    fun getTwoFactorConfig(session: GASession) = GDK.get_twofactor_config(session)

    fun changeSettingsTwoFactor(session: GASession, method: String, methodConfig: TwoFactorMethodConfig) = GDK.change_settings_twofactor(session, method, methodConfig)

    fun getWatchOnlyUsername(session: GASession) = GDK.get_watch_only_username(session)

    fun setWatchOnly(session: GASession, username : String, password: String) = GDK.set_watch_only(session, username, password)

    fun changeSettings(session: GASession, settings: Settings): GAAuthHandler =
        GDK.change_settings(session, settings)

    fun setCsvTime(session: GASession, value: JsonElement): GAAuthHandler =
        GDK.set_csvtime(session, value)

    fun getSettings(session: GASession) = GDK.get_settings(session)

    fun getAvailableCurrencies(session: GASession) = GDK.get_available_currencies(session)

    fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): JsonElement =
        GDK.auth_handler_get_status(gaAuthHandler) as JsonElement

    fun authHandlerCall(gaAuthHandler: GAAuthHandler) = GDK.auth_handler_call(gaAuthHandler)
    fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler) =
        GDK.auth_handler_request_code(gaAuthHandler, method)

    fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler) =
        GDK.auth_handler_resolve_code(gaAuthHandler, code)

    fun destroyAuthHandler(gaAuthHandler: GAAuthHandler) = GDK.destroy_auth_handler(gaAuthHandler)

    fun twofactorReset(session: GASession, email: String, isDispute: Boolean) = GDK.twofactor_reset(session, email,
        (if(isDispute) GDK.GA_TRUE else GDK.GA_FALSE).toLong()
    )

    fun twofactorUndoReset(session: GASession, email: String) = GDK.twofactor_undo_reset(session, email)

    fun twofactorCancelReset(session: GASession) = GDK.twofactor_cancel_reset(session)

    fun twofactorChangeLimits(session: GASession, limits: Limits) = GDK.twofactor_change_limits(session, limits)

    fun sendNlocktimes(session: GASession) = GDK.send_nlocktimes(session)

    fun convertAmount(session: GASession, convert: Convert) = GDK.convert_amount(session, convert)

    fun convertAmount(session: GASession, convert: JsonElement) = GDK.convert_amount(session, convert)

    fun getNetworks() = GDK.get_networks()

    fun registerNetwork(id: String, network: JsonElement) = GDK.register_network(id, network)

    fun getFeeEstimates(session: GASession) = GDK.get_fee_estimates(session)

    fun getSystemMessage(session: GASession) = GDK.get_system_message(session)
    fun ackSystemMessage(session: GASession, message: String) = GDK.ack_system_message(session, message)

    fun setTransactionMemo(session: GASession, txHash:String, memo: String) = GDK.set_transaction_memo(session, txHash, memo, 0)


    fun generateMnemonic12(): String = GDK.generate_mnemonic_12()
    fun generateMnemonic24(): String = GDK.generate_mnemonic()


    companion object {
        const val GA_OK = GDK.GA_OK
        const val GA_ERROR = GDK.GA_ERROR
        const val GA_RECONNECT = GDK.GA_RECONNECT
        const val GA_SESSION_LOST = GDK.GA_SESSION_LOST
        const val GA_TIMEOUT = GDK.GA_TIMEOUT
        const val GA_NOT_AUTHORIZED = GDK.GA_NOT_AUTHORIZED
        const val GA_NONE = GDK.GA_NONE
        const val GA_INFO = GDK.GA_INFO
        const val GA_DEBUG = GDK.GA_DEBUG
        const val GA_TRUE = GDK.GA_TRUE
        const val GA_FALSE = GDK.GA_FALSE
    }
}