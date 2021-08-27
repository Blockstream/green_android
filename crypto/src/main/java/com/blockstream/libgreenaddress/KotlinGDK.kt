package com.blockstream.libgreenaddress

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
    fun destroySession(session: GASession): GASession = GDK.destroy_session(session)

    fun connect(session: GASession, params: ConnectionParams) = GDK.connect(session, params)
    fun reconnectHint(session: GASession, hint: ReconnectHintParams) = GDK.reconnect_hint(session, hint)
    fun disconnect(session: GASession) = GDK.disconnect(session)

    fun httpRequest(session: GASession, data: JsonElement) = GDK.http_request(session, data)

    fun registerUser(session: GASession, deviceParams: DeviceParams, mnemonic: String): GAAuthHandler =
        GDK.register_user(session, deviceParams, mnemonic)

    fun loginUser(
        session: GASession,
        deviceParams: DeviceParams?,
        loginCredentialsParams: LoginCredentialsParams?
    ): GAAuthHandler = GDK.login_user(session, deviceParams, loginCredentialsParams)

    fun setPin(session: GASession, mnemonicPassphrase: String, pin: String, device: String) =
        GDK.set_pin(session, mnemonicPassphrase, pin, device)

    // TODO at the moment the encrypted mnemonic is not supported so we are always passing the empty string
    fun getMnemonicPassphrase(session: GASession) =
        GDK.get_mnemonic_passphrase(session, "")

    fun getReceiveAddress(session: GASession, params: ReceiveAddressParams): GAAuthHandler =
        GDK.get_receive_address(session, params)

    fun refreshAssets(session: GASession, params: AssetsParams) =
        GDK.refresh_assets(session, params)

    fun createSubAccount(session: GASession, params: SubAccountParams): GAAuthHandler =
        GDK.create_subaccount(session, params)

    fun getSubAccounts(session: GASession): GAAuthHandler = GDK.get_subaccounts(session)
    fun getSubAccount(session: GASession, index: Long): GASession =
        GDK.get_subaccount(session, index)

    fun renameSubAccount(session: GASession, index: Long, name: String) = GDK.rename_subaccount(session, index, name)

    fun getBalance(session: GASession, details: BalanceParams): GAAuthHandler =
        GDK.get_balance(session, details)

    fun getUnspentOutputs(session: GASession, details: BalanceParams): GAAuthHandler =
        GDK.get_unspent_outputs(session, details)

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

    fun getNetworks() = GDK.get_networks()

    fun getSystemMessage(session: GASession) = GDK.get_system_message(session)
    fun ackSystemMessage(session: GASession, message: String) = GDK.ack_system_message(session, message)


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