import Dispatch
import Foundation

import ga.sdk
import ga.wally

public enum GaError: Error {
    case GenericError(_ localizedDescription: String? = nil)
    case ReconnectError(_ localizedDescription: String? = nil)
    case SessionLost(_ localizedDescription: String? = nil)
    case TimeoutError(_ localizedDescription: String? = nil)
    case NotAuthorizedError(_ localizedDescription: String? = nil)
}

fileprivate func errorWrapper(_ r: Int32) throws {
    guard r == GA_OK else {
        let msg = try? getThreadErrorDetails()
        switch r {
            case GA_RECONNECT:
            throw GaError.ReconnectError(msg)
            case GA_SESSION_LOST:
                throw GaError.SessionLost(msg)
            case GA_TIMEOUT:
                throw GaError.TimeoutError(msg)
            case GA_NOT_AUTHORIZED:
                throw GaError.NotAuthorizedError(msg)
            default:
                throw GaError.GenericError(msg)
        }
    }
}

fileprivate func callWrapper(fun call: @autoclosure () -> Int32) throws {
    try errorWrapper(call())
}

fileprivate func convertJSONBytesToDict(_ input_bytes: UnsafeMutablePointer<Int8>) -> [String: Any]? {
    var dict: Any?
    let json = String(cString: input_bytes)
    if let data = json.data(using: .utf8) {
        do {
            dict = try JSONSerialization.jsonObject(with: data, options: [])
            if let object = dict as? [String: Any] {
                // json is a dictionary
                return object
            } else if let object = dict as? [Any] {
                // json is an array
                return ["array" : object]
            }
        }
        catch {
            return nil
        }
    }
    return dict as? [String: Any]
}

fileprivate func convertDictToJSON(dict: [String: Any]) throws -> OpaquePointer {
    let utf8_bytes = try JSONSerialization.data(withJSONObject: dict)
    var result: OpaquePointer? = nil
    let input = String(data: utf8_bytes, encoding: String.Encoding.utf8)!
    try callWrapper(fun: GA_convert_string_to_json(input, &result))
    return result!
}

fileprivate func convertOpaqueJsonToDict(o: OpaquePointer) throws -> [String: Any]? {
    var buff: UnsafeMutablePointer<Int8>? = nil
    defer {
        GA_destroy_string(buff)
        GA_destroy_json(o)
    }
    try callWrapper(fun: GA_convert_json_to_string(o, &buff))
    return convertJSONBytesToDict(buff!)
}

fileprivate func convertOpaqueJsonValueToString(o: OpaquePointer, path: String) throws -> String? {
    var buff: UnsafeMutablePointer<Int8>? = nil
    defer {
        GA_destroy_string(buff)
    }
    try callWrapper(fun: GA_convert_json_value_to_string(o, path, &buff))
    if let buff = buff {
        return String(cString: buff)
    }
    return nil
}

public func getThreadErrorDetails() throws -> String? {
    var details: OpaquePointer? = nil
    defer {
        GA_destroy_json(details)
    }
    GA_get_thread_error_details(&details)
    return try convertOpaqueJsonValueToString(o: details!, path: "details")
}

// Dummy resolver for Hardware calls
public func DummyResolve(call: TwoFactorCall) throws -> [String : Any] {
    while true {
        let json = try call.getStatus()
        let status = json!["status"] as! String
        if status == "call" {
            _ = try call.call()
        } else if status == "done" {
            return json!
        } else {
            let err = json?["error"] as? String
            throw GaError.GenericError(err)
        }
    }
}

// An operation that potentially requires authentication and multiple
// iterations to complete, e.g. setting and then activating email notifications
public class TwoFactorCall {
    private var optr: OpaquePointer? = nil

    public init(optr: OpaquePointer) {
        self.optr = optr
    }

    deinit {
        GA_destroy_auth_handler(optr);
    }

    public func getStatus() throws -> [String: Any]? {
        var status: OpaquePointer? = nil
        try callWrapper(fun: GA_auth_handler_get_status(self.optr, &status))
        return try convertOpaqueJsonToDict(o: status!)
    }

    // Request that the backend sends a 2fa code
    public func requestCode(method: String?) throws {
        if method != nil {
            try callWrapper(fun: GA_auth_handler_request_code(self.optr, method))
        }
    }

    // Provide the 2fa code sent by the server
    public func resolveCode(code: String?) throws {
        if code != nil {
            try callWrapper(fun: GA_auth_handler_resolve_code(self.optr, code))
        }
    }

    // Call the 2fa operation
    // Returns the next 2fa operation in the chain
    public func call() throws {
        try callWrapper(fun: GA_auth_handler_call(self.optr))
    }
}

public typealias NotificationCompletionHandler = (_ notification: [String: Any]?) -> Void
fileprivate var notificationContexts = [NSString: NotificationCompletionHandler?]()
fileprivate let queue = DispatchQueue(label: "BarrierQueue", attributes: .concurrent)

public func gdkInit(config: [String: Any]) throws {
    var config_json: OpaquePointer = try convertDictToJSON(dict: config)
    defer {
        GA_destroy_json(config_json)
    }
    try callWrapper(fun: GA_init(config_json))
}

public class Session {
    private typealias NotificationHandler = @convention(c) (UnsafeMutableRawPointer?, OpaquePointer?) -> Void

    private let notificationHandler : NotificationHandler = { (context: UnsafeMutableRawPointer?, details: OpaquePointer?) -> Void in
        queue.async(flags: .barrier) {
            if let context = context {
                let string = String(cString: context.assumingMemoryBound(to: CChar.self))
                if let nsString = NSString(utf8String: string),
                    let notificationContext = notificationContexts[nsString],
                    let notificationContext = notificationContext,
                    let jsonDetails = details,
                    let dict = try! convertOpaqueJsonToDict(o: jsonDetails) {
                    notificationContext(dict)
                }
            }
        }
    }

    private var session: OpaquePointer? = nil
    private let uuid: String

    func setNotificationHandler(notificationCompletionHandler: NotificationCompletionHandler?) {
        queue.sync() {
            guard let notificationCompletionHandler = notificationCompletionHandler else {
                let nsString = NSString(string: self.uuid)
                if notificationContexts.keys.contains(nsString) {
                    notificationContexts[nsString] = nil
                }
                return
            }
            let nsString = NSString(string: self.uuid)
            notificationContexts[nsString] = notificationCompletionHandler
            let ctx = UnsafeMutablePointer<Int8>(mutating: nsString.utf8String)
            try? callWrapper(fun: GA_set_notification_handler(self.session, self.notificationHandler, ctx))
        }
    }

    public init() throws {
        self.uuid = UUID().uuidString
        try callWrapper(fun: GA_create_session(&session))
    }

    deinit {
        setNotificationHandler(notificationCompletionHandler: nil)
        GA_destroy_session(session)
    }

    fileprivate func jsonFuncToJsonWrapper(input: [String: Any], fun call: (_: OpaquePointer, _: OpaquePointer, _: UnsafeMutablePointer<OpaquePointer?>) -> Int32) throws -> [String: Any]? {
        var result: OpaquePointer? = nil
        var input_json: OpaquePointer = try convertDictToJSON(dict: input)
        defer {
            GA_destroy_json(input_json)
        }
        try callWrapper(fun: call(session!, input_json, &result))
        return try convertOpaqueJsonToDict(o: result!)
    }

    fileprivate func jsonFuncToCallHandlerWrapper(input: [String: Any], fun call: (_: OpaquePointer, _: OpaquePointer, _: UnsafeMutablePointer<OpaquePointer?>) -> Int32) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil
        var input_json: OpaquePointer = try convertDictToJSON(dict: input)
        try callWrapper(fun: call(session!, input_json, &optr))
        defer {
            GA_destroy_json(input_json)
        }
        return TwoFactorCall(optr: optr!)
    }

    fileprivate func voidFuncToJsonWrapper(fun call: (_: OpaquePointer, _: UnsafeMutablePointer<OpaquePointer?>) -> Int32) throws -> [String: Any]? {
        var result: OpaquePointer? = nil
        try callWrapper(fun: call(session!, &result))
        return try convertOpaqueJsonToDict(o: result!)
    }

    public func connect(netParams: [String:Any]) throws {
        var netParamsJson: OpaquePointer = try convertDictToJSON(dict: netParams)
        defer {
            GA_destroy_json(netParamsJson)
        }
        try callWrapper(fun: GA_connect(session, netParamsJson))
    }

    public func reconnectHint(hint: [String: Any]) throws {
        var hintJson: OpaquePointer = try convertDictToJSON(dict: hint)
        defer {
            GA_destroy_json(hintJson)
        }
        try callWrapper(fun: GA_reconnect_hint(session, hintJson))
    }

    public func getProxySettings() throws -> [String: Any]? {
        var result: OpaquePointer? = nil
        try callWrapper(fun: GA_get_proxy_settings(session, &result))
        return try convertOpaqueJsonToDict(o: result!)
    }

    public func registerUser(details: [String: Any], hw_device: [String: Any] = [:]) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        var hw_device_json: OpaquePointer = try convertDictToJSON(dict: hw_device)
        var details_json: OpaquePointer = try convertDictToJSON(dict: details)
        try callWrapper(fun: GA_register_user(session, hw_device_json, details_json, &optr))
        defer {
            GA_destroy_json(hw_device_json)
            GA_destroy_json(details_json)
        }
        return TwoFactorCall(optr: optr!);
    }

    public func loginUser(details: [String: Any], hw_device: [String: Any] = [:]) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        var hw_device_json: OpaquePointer = try convertDictToJSON(dict: hw_device)
        var details_json: OpaquePointer = try convertDictToJSON(dict: details)
        try callWrapper(fun: GA_login_user(session, hw_device_json, details_json, &optr))
        defer {
            GA_destroy_json(hw_device_json)
            GA_destroy_json(details_json)
        }
        return TwoFactorCall(optr: optr!);
    }

    public func setWatchOnly(username: String, password: String) throws {
        try callWrapper(fun: GA_set_watch_only(session, username, password))
    }

    public func getWatchOnlyUsername() throws -> String {
        var buff: UnsafeMutablePointer<Int8>? = nil
        try callWrapper(fun: GA_get_watch_only_username(session, &buff))
        defer {
            GA_destroy_string(buff)
        }
        return String(cString: buff!)
    }

    public func removeAccount() throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        try callWrapper(fun: GA_remove_account(session, &optr));
        return TwoFactorCall(optr: optr!);
    }

    public func createSubaccount(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_create_subaccount)
    }

    public func getSubaccounts(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_subaccounts)
    }

    public func getSubaccount(subaccount: UInt32) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil
        try callWrapper(fun: GA_get_subaccount(session, subaccount, &optr))
        return TwoFactorCall(optr: optr!)
    }

    public func renameSubaccount(subaccount: UInt32, newName: String) throws -> Void {
        try callWrapper(fun: GA_rename_subaccount(session, subaccount, newName));
    }

    public func updateSubaccount(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_update_subaccount)
    }

    public func getTransactions(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_transactions)
    }

    public func getUnspentOutputs(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_unspent_outputs)
    }

    public func getUnspentOutputsForPrivateKey(private_key: String, password: String, unused: UInt32) throws -> [String: Any]? {
        var result: OpaquePointer? = nil
        try callWrapper(fun: GA_get_unspent_outputs_for_private_key(session, private_key, password, unused, &result))
        return try convertOpaqueJsonToDict(o: result!)
    }

    public func setUnspentOutputsStatus(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_set_unspent_outputs_status)
    }

    public func getReceiveAddress(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_receive_address)
    }

    public func getPreviousAddresses(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_previous_addresses)
    }

    public func getBalance(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_balance)
    }

    public func getAvailableCurrencies() throws -> [String: Any]? {
        return try voidFuncToJsonWrapper(fun: GA_get_available_currencies)
    }

    public func encryptWithPin(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_encrypt_with_pin)
    }

    public func decryptWithPin(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_decrypt_with_pin)
    }

    public func disableAllPinLogins() throws -> Void {
        try callWrapper(fun: GA_disable_all_pin_logins(session))
    }

    public func getTwoFactorConfig() throws -> [String: Any]? {
        return try voidFuncToJsonWrapper(fun: GA_get_twofactor_config)
    }

    public func convertAmount(input: [String: Any]) throws -> [String: Any]? {
        return try jsonFuncToJsonWrapper(input: input, fun: GA_convert_amount)
    }

    public func createTransaction(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_create_transaction)
    }

    public func signTransaction(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_sign_transaction)
    }

    public func createSwapTransaction(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_create_swap_transaction)
    }

    public func completeSwapTransaction(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_complete_swap_transaction)
    }

    public func signPsbt(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_psbt_sign)
    }

    public func PsbtGetDetails(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_psbt_get_details)
    }

    public func sendTransaction(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_send_transaction)
    }

    public func broadcastTransaction(tx_hex: String) throws -> String {
        var buff: UnsafeMutablePointer<Int8>? = nil
        try callWrapper(fun: GA_broadcast_transaction(session, tx_hex, &buff))
        defer {
            GA_destroy_string(buff)
        }
        return String(cString: buff!)
     }

    public func signMessage(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_sign_message)
    }

    public func sendNlocktimes() throws -> Void {
        try callWrapper(fun: GA_send_nlocktimes(session))
    }

    public func setNlockTime(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_set_nlocktime)
    }

    public func setTransactionMemo(txhash_hex: String, memo: String, memo_type: UInt32) throws -> Void {
        try callWrapper(fun: GA_set_transaction_memo(session, txhash_hex, memo, memo_type))
    }

    public func getSystemMessage() throws -> String {
        var buff: UnsafeMutablePointer<Int8>? = nil
        try callWrapper(fun: GA_get_system_message(session, &buff))
        defer {
            GA_destroy_string(buff)
        }
        return String(cString: buff!)
    }

    public func ackSystemMessage(message: String) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        try callWrapper(fun: GA_ack_system_message(session, message, &optr))
        return TwoFactorCall(optr: optr!);
    }

    public func getSettings() throws -> [String: Any]? {
        return try voidFuncToJsonWrapper(fun: GA_get_settings)
    }

    public func changeSettings(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_change_settings)
    }

    public func changeSettingsTwoFactor(method: String, details: [String: Any]) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        var details_json: OpaquePointer = try convertDictToJSON(dict: details)
        defer {
            GA_destroy_json(details_json)
        }
        try callWrapper(fun: GA_change_settings_twofactor(session, method, details_json, &optr));
        return TwoFactorCall(optr: optr!);
    }

    public func setTwoFactorLimit(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_twofactor_change_limits)
    }

    public func setCSVTime(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_set_csvtime)
    }

    public func resetTwoFactor(email: String, isDispute: Bool) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        try callWrapper(fun: GA_twofactor_reset(session, email, UInt32(isDispute ? GA_TRUE : GA_FALSE), &optr))
        return TwoFactorCall(optr: optr!);
    }

    public func undoTwoFactorReset(email: String) throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        try callWrapper(fun: GA_twofactor_undo_reset(session, email, &optr))
        return TwoFactorCall(optr: optr!);
    }

    public func cancelTwoFactorReset() throws -> TwoFactorCall {
        var optr: OpaquePointer? = nil;
        try callWrapper(fun: GA_twofactor_cancel_reset(session, &optr));
        return TwoFactorCall(optr: optr!);
    }

    public func getTransactionDetails(txhash: String) throws -> [String: Any]? {
        var result: OpaquePointer? = nil
        try callWrapper(fun: GA_get_transaction_details(session, txhash, &result))
        return try convertOpaqueJsonToDict(o: result!)
    }

    public func getFeeEstimates() throws -> [String: Any]? {
        return try voidFuncToJsonWrapper(fun: GA_get_fee_estimates)
    }

    public func getCredentials(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_get_credentials)
    }

    public func getWalletIdentifier(net_params: [String: Any], details: [String: Any]) throws -> [String: Any]? {
        var result: OpaquePointer? = nil
        let net_params_: OpaquePointer = try convertDictToJSON(dict: net_params)
        let details_: OpaquePointer = try convertDictToJSON(dict: details)
        try callWrapper(fun: GA_get_wallet_identifier(net_params_, details_, &result))
        defer {
            GA_destroy_json(net_params_)
            GA_destroy_json(details_)
        }
        return try convertOpaqueJsonToDict(o: result!)
    }

    public func httpRequest(params: [String: Any]) throws -> [String: Any]? {
        return try jsonFuncToJsonWrapper(input: params, fun: GA_http_request)
    }

    public func refreshAssets(params: [String: Any]) throws {
        let paramsJson: OpaquePointer = try convertDictToJSON(dict: params)
        defer {
            GA_destroy_json(paramsJson)
        }
        try callWrapper(fun: GA_refresh_assets(session, paramsJson))
    }

    public func getAssets(params: [String: Any]) throws -> [String: Any]? {
        return try jsonFuncToJsonWrapper(input: params, fun: GA_get_assets)
    }

    public func validateAssetDomainName(params: [String: Any]) throws -> [String: Any]? {
        return try jsonFuncToJsonWrapper(input: params, fun: GA_validate_asset_domain_name)
    }

    public func validate(details: [String: Any]) throws -> TwoFactorCall {
        return try jsonFuncToCallHandlerWrapper(input: details, fun: GA_validate)
    }
}

public func generateMnemonic() throws -> String {
    var buff : UnsafeMutablePointer<Int8>? = nil
    try callWrapper(fun: GA_generate_mnemonic(&buff))
    defer {
        GA_destroy_string(buff)
    }
    return String(cString: buff!)
}

public func generateMnemonic12() throws -> String {
    var buff : UnsafeMutablePointer<Int8>? = nil
    try callWrapper(fun: GA_generate_mnemonic_12(&buff))
    defer {
        GA_destroy_string(buff)
    }
    return String(cString: buff!)
}

public func validateMnemonic(mnemonic: String) throws -> Bool {
    var result: UInt32 = 0
    try callWrapper(fun: GA_validate_mnemonic(mnemonic, &result))
    return result == GA_TRUE
}

public func registerNetwork(name: String, details: [String: Any]) throws -> Void {
    var details_json: OpaquePointer = try convertDictToJSON(dict: details)
    defer {
        GA_destroy_json(details_json)
    }
    try callWrapper(fun: GA_register_network(name, details_json));
}

public func getNetworks() throws -> [String: Any]? {
    var result: OpaquePointer? = nil
    try callWrapper(fun: GA_get_networks(&result))
    return try convertOpaqueJsonToDict(o: result!)
}

public func getUniformUInt32(upper_bound: UInt32) throws -> UInt32 {
    var result: UInt32 = 0
    try callWrapper(fun: GA_get_uniform_uint32_t(upper_bound, &result))
    return result
}

public func getBIP39WordList() -> [String] {
    var words: [String] = []
    var WL: OpaquePointer?
    precondition(bip39_get_wordlist(nil, &WL) == WALLY_OK)
    for i in 0..<BIP39_WORDLIST_LEN {
        var word: UnsafeMutablePointer<Int8>?
        defer {
            wally_free_string(word)
        }
        precondition(bip39_get_word(WL, Int(i), &word) == WALLY_OK)
        words.append(String(cString: word!))
    }
    return words
}

func compressPublicKey(_ publicKey: [UInt8]) throws -> [UInt8] {
    switch publicKey[0] {
    case 0x04:
        if publicKey.count != 65 {
            throw GaError.GenericError()
        }
    case 0x02, 0x03:
        if publicKey.count != 33 {
            throw GaError.GenericError()
        }
        return publicKey
    default:
        throw GaError.GenericError()
    }
    let type = publicKey[64] & 1 != 0 ? 0x03 : 0x02
    return [UInt8(type)] + publicKey[1..<32+1]
}

public func bip32KeyToBase58(isMainnet: Bool = true, pubKey: [UInt8], chainCode: [UInt8] ) throws -> String {
    let version = isMainnet ? BIP32_VER_MAIN_PUBLIC : BIP32_VER_TEST_PUBLIC
    var extkey: UnsafeMutablePointer<ext_key>?
    var base58: UnsafeMutablePointer<Int8>?
    let pubKey_: UnsafePointer<UInt8> = UnsafePointer(pubKey)
    let chainCode_: UnsafePointer<UInt8> = UnsafePointer(chainCode)
    defer {
        bip32_key_free(extkey)
        wally_free_string(base58)
    }
    if (bip32_key_init_alloc(UInt32(version), UInt32(1), UInt32(0), chainCode_, chainCode.count,
                             pubKey_, pubKey.count, nil, 0, nil, 0, nil, 0, &extkey) != WALLY_OK) {
        throw GaError.GenericError()
    }
    if (bip32_key_to_base58(extkey, UInt32(BIP32_FLAG_KEY_PUBLIC), &base58) != WALLY_OK) {
        throw GaError.GenericError()
    }
    return String(cString: base58!)
}
