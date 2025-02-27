package com.blockstream.common.gdk

import co.touchlab.kermit.Logger
import com.blockstream.common.gdk.GdkBinding.Companion.LOGS_SIZE
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
import com.blockstream.common.gdk.params.RsaVerifyParams
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.gdk.params.SubAccountsParams
import com.blockstream.common.gdk.params.TransactionParams
import com.blockstream.common.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.common.gdk.params.UpdateSubAccountParams
import com.blockstream.common.gdk.params.ValidateAddresseesParams
import gdk.GA_FALSE
import gdk.GA_OK
import gdk.GA_TRUE
import gdk.GA_ack_system_message
import gdk.GA_auth_handler_call
import gdk.GA_auth_handler_get_status
import gdk.GA_auth_handler_request_code
import gdk.GA_auth_handler_resolve_code
import gdk.GA_bcur_decode
import gdk.GA_bcur_encode
import gdk.GA_blind_transaction
import gdk.GA_broadcast_transaction
import gdk.GA_change_settings
import gdk.GA_change_settings_twofactor
import gdk.GA_complete_swap_transaction
import gdk.GA_connect
import gdk.GA_convert_amount
import gdk.GA_convert_json_to_string
import gdk.GA_convert_json_value_to_string
import gdk.GA_convert_string_to_json
import gdk.GA_create_redeposit_transaction
import gdk.GA_create_session
import gdk.GA_create_subaccount
import gdk.GA_create_swap_transaction
import gdk.GA_create_transaction
import gdk.GA_decrypt_with_pin
import gdk.GA_destroy_auth_handler
import gdk.GA_destroy_json
import gdk.GA_destroy_session
import gdk.GA_destroy_string
import gdk.GA_encrypt_with_pin
import gdk.GA_generate_mnemonic
import gdk.GA_generate_mnemonic_12
import gdk.GA_get_assets
import gdk.GA_get_available_currencies
import gdk.GA_get_balance
import gdk.GA_get_credentials
import gdk.GA_get_fee_estimates
import gdk.GA_get_networks
import gdk.GA_get_proxy_settings
import gdk.GA_get_random_bytes
import gdk.GA_get_receive_address
import gdk.GA_get_settings
import gdk.GA_get_subaccount
import gdk.GA_get_subaccounts
import gdk.GA_get_system_message
import gdk.GA_get_thread_error_details
import gdk.GA_get_transactions
import gdk.GA_get_twofactor_config
import gdk.GA_get_unspent_outputs
import gdk.GA_get_unspent_outputs_for_private_key
import gdk.GA_get_wallet_identifier
import gdk.GA_get_watch_only_username
import gdk.GA_http_request
import gdk.GA_init
import gdk.GA_login_user
import gdk.GA_psbt_from_json
import gdk.GA_reconnect_hint
import gdk.GA_refresh_assets
import gdk.GA_register_network
import gdk.GA_register_user
import gdk.GA_rsa_verify
import gdk.GA_send_nlocktimes
import gdk.GA_send_transaction
import gdk.GA_set_csvtime
import gdk.GA_set_notification_handler
import gdk.GA_set_transaction_memo
import gdk.GA_sign_message
import gdk.GA_sign_transaction
import gdk.GA_twofactor_cancel_reset
import gdk.GA_twofactor_change_limits
import gdk.GA_twofactor_reset
import gdk.GA_twofactor_undo_reset
import gdk.GA_update_subaccount
import gdk.GA_validate
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

typealias GA_session = cnames.structs.GA_session
typealias GA_json = cnames.structs.GA_json
typealias GA_auth_handler = cnames.structs.GA_auth_handler

fun JsonElement.toGaJson(memScope: MemScope): CPointer<cnames.structs.GA_json>? {
    val gaJson = memScope.allocPointerTo<cnames.structs.GA_json>()
    GA_convert_string_to_json(Json.encodeToString(this), gaJson.ptr)
    memScope.defer {
        GA_destroy_json(gaJson.value)
    }
    return gaJson.value
}

fun <T : GreenJson<*>> T.toGaJson(memScope: MemScope): CPointer<cnames.structs.GA_json>? {
    val gaJson = memScope.allocPointerTo<cnames.structs.GA_json>()
    GA_convert_string_to_json(this.toJson(), gaJson.ptr)
    memScope.defer {
        GA_destroy_json(gaJson.value)
    }
    return gaJson.value
}

fun <R> CPointer<cnames.structs.GA_json>?.destroyGAJson(memScope: MemScope, use: (CPointer<cnames.structs.GA_json>?) -> R): R {
    memScope.defer {
        GA_destroy_json(this)
    }
    return use(this)
}


@Suppress("UNCHECKED_CAST")
fun GASession.asGASession(): CPointer<cnames.structs.GA_session> =
    this as CPointer<GA_session>

@Suppress("UNCHECKED_CAST")
fun GAAuthHandler.asGAAuthHandler(): CPointer<cnames.structs.GA_auth_handler> =
    this as CPointer<GA_auth_handler>


fun MemScope.gdkStringOrNull(block: (CPointerVar<ByteVar>) -> Unit): String? {
    return allocPointerTo<ByteVar>().let { pointer ->
        block(pointer)

        pointer.value?.toKString().also { _ ->
            GA_destroy_string(pointer.value)
        }
    }
}

fun MemScope.gdkString(block: (CPointerVar<ByteVar>) -> Unit): String {
    return gdkStringOrNull(block) ?: ""
}

fun MemScope.gaAuthHandler(): CPointerVar<GA_auth_handler> {
    return allocPointerTo()
}

fun MemScope.gaJson(): CPointerVar<GA_json> {
    return allocPointerTo()
}

fun CPointerVar<GA_json>.toJsonString(memScope: MemScope): String {
    return this.value!!.toJsonString(memScope)
}

fun CPointer<GA_json>.toJsonString(memScope: MemScope): String {
    return memScope.gdkString {
        GA_convert_json_to_string(this, it.ptr)
    }
}

fun Boolean.boolean(): Int {
    return if (this) GA_TRUE else GA_FALSE
}

fun Int.okOrThrow(gaAuthHandler: CPointerVar<GA_auth_handler>): CPointer<GA_auth_handler> {
    return okOrThrow {
        gaAuthHandler.value!!
    }
}

fun Int.okOrThrow() {
    okOrThrow { }
}

fun <R> Int.okOrThrow(block: () -> R): R {
    if (this == GA_OK) {
        return block.invoke()
    } else {
        memScoped {
            val error = allocPointerTo<GA_json>()
            GA_get_thread_error_details(error.ptr)

            val errorMessage = gdkStringOrNull {
                GA_convert_json_value_to_string(error.value, "details", it.ptr)
            }

            error.value.destroyGAJson(this) {
                throw Exception(errorMessage)
            }
        }
    }
}


class NotifyContext constructor(
    val session: CPointer<GA_session>,
    val notificationHandler: ((session: GASession, jsonObject: JsonElement) -> Unit)? = null
)

private val _gdkNotificationHandler = staticCFunction { context: COpaquePointer?, gaJson: CPointer<GA_json>? ->
    val ref: StableRef<NotifyContext>? = context?.asStableRef()
    val notifyContext = ref?.get()

    if(ref != null && gaJson != null && notifyContext != null){
        memScoped {
            gaJson.destroyGAJson(this) {
                gaJson.toJsonString(this).also {
                    Json.parseToJsonElement(it).also { jsonElement ->
                        notifyContext.notificationHandler?.invoke(
                            notifyContext.session,
                            jsonElement
                        )
                    }
                }
            }
        }
    } else {
        Logger.e { "context == null && gaJson == null" }
    }

    Unit
}

class IOSGdkBinding constructor(config: InitConfig) : GdkBinding {
    override val logs: StringBuilder = StringBuilder()
    private val _notifyContexts = mutableMapOf<CPointer<GA_session>, StableRef<NotifyContext>>()
    private var _notificationHandler: ((session: GASession, jsonObject: JsonElement) -> Unit)? = null
    private val _dataDir: String = config.datadir

    init {
        memScoped {
            GA_init(config.toGaJson(this))
        }
    }

    override val dataDir: String
        get() = _dataDir

    override fun appendGdkLogs(json: String) {
        logs.append("$json\n")
        if (logs.length > LOGS_SIZE) {
            logs.deleteRange(0, 1_000_000)
        }
    }

    override fun setNotificationHandler(notificationHandler: (session: GASession, jsonObject: Any) -> Unit) {
        _notificationHandler = notificationHandler
    }

    @Throws(Exception::class)
    override fun createSession(): GASession {
        memScoped {
            val gaSessionPointer = allocPointerTo<GA_session>()
            return GA_create_session(gaSessionPointer.ptr).okOrThrow {
                val gaSession = gaSessionPointer.value!!

                val notify: StableRef<NotifyContext> = StableRef.create(NotifyContext(gaSession, _notificationHandler)).also { notifyContext ->
                    _notifyContexts[gaSession] = notifyContext
                }

                GA_set_notification_handler(
                    session = gaSession,
                    handler = _gdkNotificationHandler,
                    context = notify.asCPointer()
                )

                // Return gaSession
                gaSession
            }
        }
    }

    @Throws(Exception::class)
    override fun destroySession(session: GASession) {
        GA_destroy_session(session.asGASession()).okOrThrow()
        _notifyContexts.remove(session.asGASession())?.dispose()
    }

    @Throws(Exception::class)
    override fun connect(session: GASession, params: ConnectionParams) {
        memScoped {
            GA_connect(
                session = session.asGASession(), net_params = params.toGaJson(this)
            ).okOrThrow()
        }
    }

    @Throws(Exception::class)
    override fun reconnectHint(session: GASession, hint: ReconnectHintParams) {
        memScoped {
            GA_reconnect_hint(
                session = session.asGASession(), hint = hint.toGaJson(this)
            ).okOrThrow()
        }
    }

    @Throws(Exception::class)
    override fun getProxySettings(session: GASession): ProxySettings {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_proxy_settings(
                    session = session.asGASession(), output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun registerUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_register_user(
                    session = session.asGASession(),
                    hw_device = deviceParams.toGaJson(this),
                    details = loginCredentialsParams.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun loginUser(
        session: GASession,
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_login_user(
                    session = session.asGASession(),
                    hw_device = deviceParams.toGaJson(this),
                    details = loginCredentialsParams.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getWalletIdentifier(
        connectionParams: ConnectionParams, loginCredentialsParams: LoginCredentialsParams
    ): LoginData {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_wallet_identifier(
                    net_params = connectionParams.toGaJson(this),
                    params = loginCredentialsParams.toGaJson(this),
                    output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun validate(session: GASession, params: JsonElement): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_validate(
                    session = session.asGASession(),
                    details = params.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun validate(session: GASession, params: ValidateAddresseesParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_validate(
                    session = session.asGASession(),
                    details = params.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun encryptWithPin(
        session: GASession, encryptWithPinParams: EncryptWithPinParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_encrypt_with_pin(
                    session = session.asGASession(),
                    details = encryptWithPinParams.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun decryptWithPin(
        session: GASession, decryptWithPinParams: DecryptWithPinParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_decrypt_with_pin(
                    session = session.asGASession(),
                    details = decryptWithPinParams.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getCredentials(session: GASession, params: CredentialsParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_credentials(
                    session = session.asGASession(),
                    details = params.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getReceiveAddress(
        session: GASession, params: ReceiveAddressParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_receive_address(
                    session = session.asGASession(),
                    details = params.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getPreviousAddress(
        session: GASession, params: PreviousAddressParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_receive_address(
                    session = session.asGASession(),
                    details = params.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun refreshAssets(session: GASession, params: AssetsParams) {
        memScoped {
            GA_refresh_assets(session.asGASession(), params = params.toGaJson(this)).okOrThrow()
        }
    }

    @Throws(Exception::class)
    override fun getAssets(session: GASession, params: GetAssetsParams): LiquidAssets {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_assets(
                    session = session.asGASession(),
                    params = params.toGaJson(this),
                    output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun getTransactions(session: GASession, details: TransactionParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_transactions(
                    session = session.asGASession(),
                    details = details.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getTwoFactorConfig(session: GASession): TwoFactorConfig {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_twofactor_config(
                    session = session.asGASession(), config = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun changeSettingsTwoFactor(
        session: GASession, method: String, methodConfig: TwoFactorMethodConfig
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_change_settings_twofactor(
                    session = session.asGASession(),
                    method = method,
                    twofactor_details = methodConfig.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getWatchOnlyUsername(session: GASession): String? {
        return memScoped {
            gdkStringOrNull {
                GA_get_watch_only_username(session = session.asGASession(), it.ptr).okOrThrow()
            }
        }
    }

    @Throws(Exception::class)
    override fun changeSettings(session: GASession, settings: Settings): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_change_settings(
                    session = session.asGASession(),
                    settings = settings.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun setCsvTime(session: GASession, value: CsvParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_set_csvtime(
                    session = session.asGASession(),
                    locktime_details = value.toGaJson(memScope),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getSettings(session: GASession): Settings {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_settings(
                    session = session.asGASession(), settings = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun getAvailableCurrencies(session: GASession): List<Pricing> {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_available_currencies(
                    session = session.asGASession(), currencies = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        Pricing.fromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): AuthHandlerStatus {
        return memScoped {
            gaJson().let { gaJson ->
                GA_auth_handler_get_status(
                    call = gaAuthHandler.asGAAuthHandler(), output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun authHandlerCall(gaAuthHandler: GAAuthHandler) {
        GA_auth_handler_call(call = gaAuthHandler.asGAAuthHandler()).okOrThrow()
    }

    @Throws(Exception::class)
    override fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler) {
        GA_auth_handler_request_code(
            call = gaAuthHandler.asGAAuthHandler(), method = method
        ).okOrThrow()
    }

    @Throws(Exception::class)
    override fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler) {
        GA_auth_handler_resolve_code(
            call = gaAuthHandler.asGAAuthHandler(), code = code
        ).okOrThrow()
    }

    @Throws(Exception::class)
    override fun destroyAuthHandler(gaAuthHandler: GAAuthHandler) {
        GA_destroy_auth_handler(gaAuthHandler.asGAAuthHandler()).okOrThrow()
    }

    @Throws(Exception::class)
    override fun twoFactorReset(
        session: GASession, email: String, isDispute: Boolean
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_twofactor_reset(
                    session = session.asGASession(),
                    email = email,
                    is_dispute = isDispute.boolean().convert(),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun twoFactorUndoReset(session: GASession, email: String): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_twofactor_undo_reset(
                    session = session.asGASession(), email = email, call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun twoFactorCancelReset(session: GASession): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_twofactor_cancel_reset(
                    session = session.asGASession(), call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun twoFactorChangeLimits(session: GASession, limits: Limits): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_twofactor_change_limits(
                    session = session.asGASession(),
                    limit_details = limits.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun bcurEncode(session: GASession, params: BcurEncodeParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_bcur_encode(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun bcurDecode(session: GASession, params: BcurDecodeParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_bcur_decode(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun sendNlocktimes(session: GASession) {
        GA_send_nlocktimes(session = session.asGASession()).okOrThrow()
    }

    @Throws(Exception::class)
    override fun getFeeEstimates(session: GASession): FeeEstimation {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_fee_estimates(
                    session = session.asGASession(), estimates = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        JsonConverter.JsonDeserializer.decodeFromString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun getSystemMessage(session: GASession): String? {
        return memScoped {
            gdkStringOrNull {
                GA_get_system_message(session = session.asGASession(), message_text = it.ptr).okOrThrow()
            }
        }
    }

    @Throws(Exception::class)
    override fun ackSystemMessage(session: GASession, message: String): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_ack_system_message(
                    session = session.asGASession(),
                    message_text = message,
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun setTransactionMemo(session: GASession, txHash: String, memo: String) {
        GA_set_transaction_memo(
            session = session.asGASession(),
            txhash_hex = txHash,
            memo = memo,
            memo_type = (0).convert()
        ).okOrThrow()
    }

    @Throws(Exception::class)
    override fun convertAmount(session: GASession, convert: JsonElement): JsonElement {
        return memScoped {
            gaJson().let { gaJson ->
                GA_convert_amount(
                    session = session.asGASession(),
                    value_details = convert.toGaJson(this),
                    output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        Json.parseToJsonElement(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun networks(): Networks {
        return memScoped {
            gaJson().let { gaJson ->
                GA_get_networks(
                    output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        Networks.fromJsonString(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun registerNetwork(id: String, network: JsonElement) {
        memScoped {
            GA_register_network(name = id, network_details = network.toGaJson(this)).okOrThrow()
        }
    }

    @Throws(Exception::class)
    override fun blindTransaction(
        session: GASession, createTransaction: JsonElement
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_blind_transaction(
                    session = session.asGASession(),
                    transaction_details = createTransaction.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun signTransaction(
        session: GASession, createTransaction: JsonElement
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_sign_transaction(
                    session = session.asGASession(),
                    transaction_details = createTransaction.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun psbtFromJson(session: GASession, transaction: JsonElement): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_psbt_from_json(
                    session = session.asGASession(),
                    details = transaction.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    // GDK 0.73.0
    @Throws(Exception::class)
    override fun broadcastTransaction(session: GASession, broadcastTransactionParams: BroadcastTransactionParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_broadcast_transaction(
                    session = session.asGASession(),
                    details = broadcastTransactionParams.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun sendTransaction(session: GASession, transaction: JsonElement): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_send_transaction(
                    session = session.asGASession(),
                    transaction_details = transaction.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun signMessage(session: GASession, params: SignMessageParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_sign_message(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun createSubAccount(session: GASession, params: SubAccountParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_create_subaccount(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getSubAccounts(session: GASession, params: SubAccountsParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_subaccounts(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getSubAccount(session: GASession, index: Long): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_subaccount(
                    session = session.asGASession(),
                    subaccount = index.convert(),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun updateSubAccount(
        session: GASession, params: UpdateSubAccountParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_update_subaccount(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getBalance(session: GASession, details: BalanceParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_balance(
                    session = session.asGASession(),
                    details = details.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun getUnspentOutputs(session: GASession, details: BalanceParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_unspent_outputs(
                    session = session.asGASession(),
                    details = details.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    override fun getUnspentOutputsForPrivateKey(
        session: GASession,
        details: UnspentOutputsPrivateKeyParams
    ): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_get_unspent_outputs_for_private_key(
                    session = session.asGASession(),
                    details = details.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun createTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_create_transaction(
                    session = session.asGASession(),
                    transaction_details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun createRedepositTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_create_redeposit_transaction(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun createSwapTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_create_swap_transaction(
                    session = session.asGASession(),
                    swap_details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    @Throws(Exception::class)
    override fun completeSwapTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_complete_swap_transaction(
                    session = session.asGASession(),
                    swap_details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    override fun rsaVerify(session: GASession, params: RsaVerifyParams): GAAuthHandler {
        return memScoped {
            gaAuthHandler().let { gaAuthHandler ->
                GA_rsa_verify(
                    session = session.asGASession(),
                    details = params.toGaJson(this),
                    call = gaAuthHandler.ptr
                ).okOrThrow(gaAuthHandler)
            }
        }
    }

    override fun httpRequest(session: GASession, data: JsonElement): JsonElement {
        return memScoped {
            gaJson().let { gaJson ->
                GA_http_request(
                    session = session.asGASession(),
                    params = data.toGaJson(this),
                    output = gaJson.ptr
                ).okOrThrow {
                    gaJson.toJsonString(this).let {
                        Json.parseToJsonElement(it)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun generateMnemonic12(): String {
        return memScoped {
            gdkString {
                GA_generate_mnemonic_12(it.ptr).okOrThrow()
            }
        }
    }

    @Throws(Exception::class)
    override fun generateMnemonic24(): String {
        return memScoped {
            gdkString {
                GA_generate_mnemonic(it.ptr).okOrThrow()
            }
        }
    }

    @Throws(Exception::class)
    override fun getRandomBytes(size: Int): ByteArray {
        return ByteArray(size).toUByteArray().let {
            it.usePinned { byteArray ->
                GA_get_random_bytes(
                    num_bytes = size.convert(),
                    output_bytes = byteArray.addressOf(0),
                    len = size.convert()
                ).okOrThrow()
            }
            it.toByteArray()
        }
    }
}

actual fun getGdkBinding(log: Boolean, config: InitConfig): GdkBinding = IOSGdkBinding(config)

actual val GA_ERROR: Int = gdk.GA_ERROR
actual val GA_RECONNECT: Int = gdk.GA_RECONNECT
actual val GA_NOT_AUTHORIZED: Int = gdk.GA_NOT_AUTHORIZED