package com.blockstream.data.gdk

import com.blockstream.data.gdk.data.AuthHandlerStatus
import com.blockstream.data.gdk.data.FeeEstimation
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.data.LoginData
import com.blockstream.data.gdk.data.Networks
import com.blockstream.data.gdk.data.Pricing
import com.blockstream.data.gdk.data.ProxySettings
import com.blockstream.data.gdk.data.Settings
import com.blockstream.data.gdk.data.TwoFactorConfig
import com.blockstream.data.gdk.data.TwoFactorMethodConfig
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.BalanceParams
import com.blockstream.data.gdk.params.BcurDecodeParams
import com.blockstream.data.gdk.params.BcurEncodeParams
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.ConnectionParams
import com.blockstream.data.gdk.params.CredentialsParams
import com.blockstream.data.gdk.params.CsvParams
import com.blockstream.data.gdk.params.DecryptWithPinParams
import com.blockstream.data.gdk.params.DeviceParams
import com.blockstream.data.gdk.params.EncryptWithPinParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.gdk.params.InitConfig
import com.blockstream.data.gdk.params.Limits
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.gdk.params.PreviousAddressParams
import com.blockstream.data.gdk.params.ReceiveAddressParams
import com.blockstream.data.gdk.params.ReconnectHintParams
import com.blockstream.data.gdk.params.RsaVerifyParams
import com.blockstream.data.gdk.params.SignMessageParams
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.gdk.params.SubAccountsParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.gdk.params.UnspentOutputsPrivateKeyParams
import com.blockstream.data.gdk.params.UpdateSubAccountParams
import com.blockstream.data.gdk.params.ValidateAddresseesParams
import kotlinx.serialization.json.JsonElement

actual fun getGdkBinding(
    printGdkMessages: Boolean,
    config: InitConfig
): GdkBinding {
    return object : GdkBinding {
        override val logs: StringBuilder = StringBuilder()

        override val dataDir: String
            get() = config.datadir

        override fun appendGdkLogs(json: String) {
            TODO("Not yet implemented")
        }

        override fun setNotificationHandler(notificationHandler: (session: GASession, jsonObject: Any) -> Unit) {

        }

        override fun createSession(): GASession {
            TODO("Not yet implemented")
        }

        override fun destroySession(session: GASession) {
            TODO("Not yet implemented")
        }

        override fun connect(session: GASession, params: ConnectionParams) {
            TODO("Not yet implemented")
        }

        override fun reconnectHint(session: GASession, hint: ReconnectHintParams) {
            TODO("Not yet implemented")
        }

        override fun getProxySettings(session: GASession): ProxySettings {
            TODO("Not yet implemented")
        }

        override fun registerUser(
            session: GASession,
            deviceParams: DeviceParams,
            loginCredentialsParams: LoginCredentialsParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun loginUser(
            session: GASession,
            deviceParams: DeviceParams,
            loginCredentialsParams: LoginCredentialsParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getWalletIdentifier(
            connectionParams: ConnectionParams,
            loginCredentialsParams: LoginCredentialsParams
        ): LoginData {
            TODO("Not yet implemented")
        }

        override fun validate(session: GASession, params: JsonElement): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun validate(session: GASession, params: ValidateAddresseesParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun encryptWithPin(
            session: GASession,
            encryptWithPinParams: EncryptWithPinParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun decryptWithPin(
            session: GASession,
            decryptWithPinParams: DecryptWithPinParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getCredentials(session: GASession, params: CredentialsParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getReceiveAddress(
            session: GASession,
            params: ReceiveAddressParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getPreviousAddress(
            session: GASession,
            params: PreviousAddressParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun refreshAssets(session: GASession, params: AssetsParams) {
            TODO("Not yet implemented")
        }

        override fun getAssets(session: GASession, params: GetAssetsParams): LiquidAssets {
            TODO("Not yet implemented")
        }

        override fun getTransactions(
            session: GASession,
            details: TransactionParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getTwoFactorConfig(session: GASession): TwoFactorConfig {
            TODO("Not yet implemented")
        }

        override fun changeSettingsTwoFactor(
            session: GASession,
            method: String,
            methodConfig: TwoFactorMethodConfig
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getWatchOnlyUsername(session: GASession): String? {
            TODO("Not yet implemented")
        }

        override fun changeSettings(session: GASession, settings: Settings): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun setCsvTime(session: GASession, value: CsvParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getSettings(session: GASession): Settings {
            TODO("Not yet implemented")
        }

        override fun getAvailableCurrencies(session: GASession): List<Pricing> {
            TODO("Not yet implemented")
        }

        override fun getAuthHandlerStatus(gaAuthHandler: GAAuthHandler): AuthHandlerStatus {
            TODO("Not yet implemented")
        }

        override fun authHandlerCall(gaAuthHandler: GAAuthHandler) {
            TODO("Not yet implemented")
        }

        override fun authHandlerRequestCode(method: String, gaAuthHandler: GAAuthHandler) {
            TODO("Not yet implemented")
        }

        override fun authHandlerResolveCode(code: String, gaAuthHandler: GAAuthHandler) {
            TODO("Not yet implemented")
        }

        override fun destroyAuthHandler(gaAuthHandler: GAAuthHandler) {
            TODO("Not yet implemented")
        }

        override fun twoFactorReset(
            session: GASession,
            email: String,
            isDispute: Boolean
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun twoFactorUndoReset(session: GASession, email: String): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun twoFactorCancelReset(session: GASession): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun twoFactorChangeLimits(session: GASession, limits: Limits): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun bcurEncode(session: GASession, params: BcurEncodeParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun bcurDecode(session: GASession, params: BcurDecodeParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun sendNlocktimes(session: GASession) {
            TODO("Not yet implemented")
        }

        override fun getFeeEstimates(session: GASession): FeeEstimation {
            TODO("Not yet implemented")
        }

        override fun getSystemMessage(session: GASession): String? {
            TODO("Not yet implemented")
        }

        override fun ackSystemMessage(session: GASession, message: String): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun setTransactionMemo(session: GASession, txHash: String, memo: String) {
            TODO("Not yet implemented")
        }

        override fun convertAmount(session: GASession, convert: JsonElement): JsonElement {
            TODO("Not yet implemented")
        }

        override fun networks(): Networks {
            TODO("Not yet implemented")
        }

        override fun registerNetwork(id: String, network: JsonElement) {
            TODO("Not yet implemented")
        }

        override fun blindTransaction(
            session: GASession,
            createTransaction: JsonElement
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun signTransaction(
            session: GASession,
            createTransaction: JsonElement
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun psbtFromJson(session: GASession, transaction: JsonElement): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun broadcastTransaction(
            session: GASession,
            broadcastTransactionParams: BroadcastTransactionParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun sendTransaction(session: GASession, transaction: JsonElement): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun signMessage(session: GASession, params: SignMessageParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun createSubAccount(session: GASession, params: SubAccountParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getSubAccounts(session: GASession, params: SubAccountsParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getSubAccount(session: GASession, index: Long): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun updateSubAccount(
            session: GASession,
            params: UpdateSubAccountParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getBalance(session: GASession, details: BalanceParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getUnspentOutputs(session: GASession, details: BalanceParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun getUnspentOutputsForPrivateKey(
            session: GASession,
            details: UnspentOutputsPrivateKeyParams
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun createTransaction(session: GASession, params: GreenJson<*>): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun createRedepositTransaction(
            session: GASession,
            params: GreenJson<*>
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun createSwapTransaction(
            session: GASession,
            params: GreenJson<*>
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun completeSwapTransaction(
            session: GASession,
            params: GreenJson<*>
        ): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun rsaVerify(session: GASession, params: RsaVerifyParams): GAAuthHandler {
            TODO("Not yet implemented")
        }

        override fun httpRequest(session: GASession, data: JsonElement): JsonElement {
            TODO("Not yet implemented")
        }

        override fun generateMnemonic12(): String {
            TODO("Not yet implemented")
        }

        override fun generateMnemonic24(): String {
            TODO("Not yet implemented")
        }

        override fun getRandomBytes(size: Int): ByteArray {
            TODO("Not yet implemented")
        }

    }
}

actual val GA_ERROR: Int = -1
actual val GA_RECONNECT: Int = -2
actual val GA_NOT_AUTHORIZED: Int = -5