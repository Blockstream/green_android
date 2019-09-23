import Foundation
import LocalAuthentication
import Security

class AuthenticationTypeHandler {
    public enum AuthError: Error, Equatable {
        case CanceledByUser
        case ServiceNotAvailable(_ desc: String)
        case NotSupported
        case PasscodeNotSet
        case SecurityError(_ desc: String)
        case KeychainError(_ status: OSStatus)

        var localizedDescription: String {
            get {
                switch self {
                case .KeychainError(let status):
                    if #available(iOS 11.3, *) {
                        let text = SecCopyErrorMessageString(status, nil) ?? "" as CFString
                        return "Operation failed: \(status) \(text))"
                    } else {
                        return "Operation failed: \(status). Check the error message through https://osstatus.com."
                    }
                case .ServiceNotAvailable(let desc), .SecurityError(let desc):
                    return desc
                case .NotSupported:
                    return NSLocalizedString("id_your_ios_device_might_not_be", comment: "")
                case .PasscodeNotSet:
                    return NSLocalizedString("id_set_up_a_passcode_for_your_ios", comment: "")
                case .CanceledByUser:
                    return NSLocalizedString("id_action_canceled", comment: "")
                }
            }
        }
    }

    static let AuthKeyBiometric = "com.blockstream.green.auth_key_biometric"
    static let AuthKeyPIN = "com.blockstream.green.auth_key_pin"

    static let PrivateKeyPathSize = 32
    static let AuthKeyBiometricPrivateKeyPathPrefix = "com.blockstream.green."
    static let ECCEncryptionType = SecKeyAlgorithm.eciesEncryptionCofactorVariableIVX963SHA256AESGCM
    static let ECCKeyType = kSecAttrKeyTypeECSECPrimeRandom
    static let ECCKeySizeInBits = 256

    static var biometryType: LABiometryType? {
        let context = LAContext()

        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return nil
        }
        return context.biometryType
    }

    static func supportsBiometricAuthentication() -> Bool {
        guard let biometryType = biometryType else {
            return false
        }
        return biometryType == LABiometryType.faceID || biometryType == LABiometryType.touchID
    }

    static func supportsPasscodeAuthentication() -> Bool {
        return LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: nil)
    }

    fileprivate static func describeKeychainError(_ status: OSStatus) -> OSStatus {
        if status != errSecSuccess && status != errSecDuplicateItem {
            if #available(iOS 11.3, *) {
                let err = SecCopyErrorMessageString(status, nil)
                NSLog("Operation failed: \(String(describing: err))")
#if DEBUG
                NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
            } else {
                NSLog("Operation failed: \(status). Check the error message through https://osstatus.com.")
#if DEBUG
                NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
            }
        }
        return status
    }

    fileprivate static func describeSecurityError(_ error: CFError) -> String {
        let err = CFErrorCopyDescription(error)
        let errorString = String(describing: err!)
        NSLog("Operation failed: \(errorString)")
#if DEBUG
        NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
        return errorString
    }

    fileprivate static func callWrapper(fun call: @autoclosure () -> Int32) -> OSStatus {
        return describeKeychainError(call())
    }

    fileprivate static func getACL() throws -> SecAccessControl {
        guard #available(iOS 11.3, *) else {
            throw AuthError.NotSupported
        }
        var error: Unmanaged<CFError>?
        let access = SecAccessControlCreateWithFlags(nil,
                                                     kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                                                     [SecAccessControlCreateFlags.biometryAny,
                                                      SecAccessControlCreateFlags.privateKeyUsage],
                                                     &error)
        guard error == nil else {
            throw AuthError.SecurityError(describeSecurityError(error!.takeRetainedValue()))
        }
        guard access != nil else {
            let text = "Operation failed: Access control not supported."
            NSLog(text)
            throw AuthError.ServiceNotAvailable(text)
        }
        return access!
    }

    static func generateBiometricPrivateKey(network: String) throws {
        let acl = try getACL()

        let privateKeyLabel = AuthKeyBiometricPrivateKeyPathPrefix + String.random(length: PrivateKeyPathSize) + network
        let params: [CFString: Any] = [kSecAttrKeyType: ECCKeyType,
                                       kSecAttrKeySizeInBits: ECCKeySizeInBits,
                                       kSecAttrTokenID: kSecAttrTokenIDSecureEnclave,
                                       kSecPrivateKeyAttrs: [kSecAttrLabel: privateKeyLabel,
                                                             kSecAttrAccessControl: acl,
                                                             kSecAttrIsPermanent: true]]

        var error: Unmanaged<CFError>?
        _ = SecKeyCreateRandomKey(params as CFDictionary, &error)
        guard error == nil else {
            throw AuthError.SecurityError(describeSecurityError(error!.takeRetainedValue()))
        }

        UserDefaults.standard.set(privateKeyLabel, forKey: "AuthKeyBiometricPrivateKey" + network)
    }

    fileprivate static func getPrivateKey(forNetwork: String) throws -> SecKey {
        let privateKeyLabel = UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + forNetwork)
        guard privateKeyLabel != nil else {
            throw AuthError.ServiceNotAvailable("Operation failed: Key not found.")
        }
        let q: [CFString: Any] = [kSecClass: kSecClassKey,
                                  kSecAttrKeyType: ECCKeyType,
                                  kSecAttrKeySizeInBits: ECCKeySizeInBits,
                                  kSecAttrLabel: privateKeyLabel!,
                                  kSecReturnRef: true,
                                  kSecUseOperationPrompt: "Unlock Green"]

        var privateKey: CFTypeRef?
        let status = callWrapper(fun: SecItemCopyMatching(q as CFDictionary, &privateKey))
        guard status == errSecSuccess else {
            throw AuthError.KeychainError(status)
        }
        return (privateKey as! SecKey)
    }

    fileprivate static func getPublicKey(forNetwork: String) throws -> SecKey {
        let privateKey = try getPrivateKey(forNetwork: forNetwork)
        guard let pubkey = SecKeyCopyPublicKey(privateKey) else {
            let text = "Operation failed: key does not contain a public key."
            NSLog(text)
            throw AuthError.ServiceNotAvailable(text)
        }
        return pubkey
    }

    fileprivate static func decrypt(base64Encoded: Data, forNetwork: String) throws -> String {
        let privateKey = try getPrivateKey(forNetwork: forNetwork)

        let canDecrypt = SecKeyIsAlgorithmSupported(privateKey, SecKeyOperationType.decrypt, ECCEncryptionType)
        guard canDecrypt else {
            NSLog("Operation failed: Decryption algorithm not supported.")
#if DEBUG
            NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
            throw AuthError.ServiceNotAvailable("Operation failed: Decryption algorithm not supported.")
        }

        var error: Unmanaged<CFError>?
        let decrypted = SecKeyCreateDecryptedData(privateKey, ECCEncryptionType, base64Encoded as CFData, &error)
        guard error == nil else {
            let cfError = error!.takeRetainedValue()
            if CFErrorGetCode(cfError) == -2 {
                throw AuthError.CanceledByUser
            } else {
                throw AuthError.SecurityError(describeSecurityError(cfError))
            }
        }
        return String(data: decrypted! as Data, encoding: .utf8)!
    }

    fileprivate static func encrypt(plaintext: String, forNetwork: String) throws -> String {
        let publicKey = try getPublicKey(forNetwork: forNetwork)

        let canEncrypt = SecKeyIsAlgorithmSupported(publicKey, SecKeyOperationType.encrypt, ECCEncryptionType)
        guard canEncrypt else {
            NSLog("Operation failed: Encryption algorithm not supported.")
#if DEBUG
            NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
            throw AuthError.ServiceNotAvailable("Operation failed: Encryption algorithm not supported.")
        }

        var error: Unmanaged<CFError>?
        let data = plaintext.data(using: .utf8, allowLossyConversion: false)
        let encrypted = SecKeyCreateEncryptedData(publicKey, ECCEncryptionType, data! as CFData, &error)
        guard error == nil else {
            throw AuthError.SecurityError(describeSecurityError(error!.takeRetainedValue()))
        }

        return (encrypted! as Data).base64EncodedString()
    }

    fileprivate static func queryFor(method: String, forNetwork: String) -> [CFString: Any] {
        let q: [CFString: Any] = [kSecClass: kSecClassGenericPassword,
                                  kSecAttrAccessible: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                                  kSecAttrService: method,
                                  kSecAttrAccount: forNetwork]
        return q
    }

    fileprivate static func queryForData(method: String, forNetwork: String) -> [CFString: Any] {
        return queryFor(method: method, forNetwork: forNetwork)
                        .merging([kSecReturnData: kCFBooleanTrue ?? true]) { (current, _) in current }
    }

    fileprivate static func set(method: String, data: [String: Any], forNetwork: String) throws {
        if !supportsPasscodeAuthentication() {
            throw AuthError.PasscodeNotSet
        }
        guard let data = try? JSONSerialization.data(withJSONObject: data) else {
            throw AuthError.ServiceNotAvailable("Operation failed: Invalid json on serialization.")
        }
        let q = queryFor(method: method, forNetwork: forNetwork)
        let qAdd = q.merging([kSecValueData: data]) { (current, _) in current }
        var status = callWrapper(fun: SecItemAdd(qAdd as CFDictionary, nil))
        if status == errSecDuplicateItem {
            status = callWrapper(fun: SecItemDelete(q as CFDictionary))
            status = callWrapper(fun: SecItemAdd(qAdd as CFDictionary, nil))
        }
        if status != errSecSuccess {
            throw AuthError.KeychainError(status)
        }
    }

    fileprivate static func get_(method: String, forNetwork: String) throws -> [String: Any] {
        let q = queryForData(method: method, forNetwork: forNetwork)
        var result: CFTypeRef?
        let status = callWrapper(fun: SecItemCopyMatching(q as CFDictionary, &result))
        guard status == errSecSuccess, result != nil, let resultData = result as? Data else {
            throw AuthError.KeychainError(status)
        }
        guard let data = try? JSONSerialization.jsonObject(with: resultData, options: []) as? [String: Any] else {
            throw AuthError.ServiceNotAvailable("Operation failed: Invalid json on serialization.")
        }
        return data
    }

    fileprivate static func get(method: String, toDecrypt: Bool, forNetwork: String) throws -> [String: Any] {
        let data = try get_(method: method, forNetwork: forNetwork)
        var extended = data
        if toDecrypt {
            precondition(method == AuthKeyBiometric)
            let encryptedBiometric = data["encrypted_biometric"] as? String
            let decoded = Data(base64Encoded: encryptedBiometric!)
            let plaintext = try decrypt(base64Encoded: decoded!, forNetwork: forNetwork)
            extended["plaintext_biometric"] = plaintext
        }
        return extended
    }

    static func getAuth(method: String, forNetwork: String) throws -> [String: Any] {
        return try get(method: method, toDecrypt: method == AuthKeyBiometric, forNetwork: forNetwork)
    }

    static func findAuth(method: String, forNetwork: String) -> Bool {
        return (try? get_(method: method, forNetwork: forNetwork)) != nil
    }

    static func removePrivateKey(forNetwork: String) throws {
        let privateKeyLabel = UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + forNetwork)
        guard privateKeyLabel != nil else {
            throw AuthError.ServiceNotAvailable("Operation failed: Key not found.")
        }
        let q: [CFString: Any] = [kSecClass: kSecClassKey,
                                  kSecAttrKeyType: ECCKeyType,
                                  kSecAttrKeySizeInBits: ECCKeySizeInBits,
                                  kSecAttrLabel: privateKeyLabel!,
                                  kSecReturnRef: true]
        let status = callWrapper(fun: SecItemDelete(q as CFDictionary))
        if status != errSecSuccess {
            throw AuthError.KeychainError(status)
        }
    }

    static func removeAuth(method: String, forNetwork: String) -> Bool {
        let q = queryForData(method: method, forNetwork: forNetwork)
        return callWrapper(fun: SecItemDelete(q as CFDictionary)) == errSecSuccess
    }

    static func addBiometryType(data: [String: Any], extraData: String, forNetwork: String) throws {
        let encrypted = try encrypt(plaintext: extraData, forNetwork: forNetwork)
        var extended = data
        extended["encrypted_biometric"] = encrypted
        try set(method: AuthKeyBiometric, data: extended, forNetwork: forNetwork)
    }

    static func addPIN(data: [String: Any], forNetwork: String) throws {
        try set(method: AuthKeyPIN, data: data, forNetwork: forNetwork)
    }
}
