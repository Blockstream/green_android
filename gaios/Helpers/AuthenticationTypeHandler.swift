import Foundation
import LocalAuthentication
import Security

class AuthenticationTypeHandler {
    public enum AuthError: Error, Equatable {
        case CanceledByUser
        case ServiceNotAvailable
        case Unknown(desc: String)

        var localizedDescription: String {
            get {
                switch self {
                case .Unknown(let desc):
                    return desc
                default:
                    return ""
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

    fileprivate static func describeSecurityError(_ error: Unmanaged<CFError>) {
        let err = CFErrorCopyDescription(error.takeRetainedValue())
        NSLog("Operation failed: \(String(describing: err))")
#if DEBUG
        NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
    }

    fileprivate static func callWrapper(fun call: @autoclosure () -> Int32) -> OSStatus {
        return describeKeychainError(call())
    }

    fileprivate static func getACL() -> SecAccessControl? {
        guard #available(iOS 11.3, *) else {
            return nil
        }
        var error: Unmanaged<CFError>?
        let access = SecAccessControlCreateWithFlags(nil,
                                                     kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                                                     [SecAccessControlCreateFlags.biometryAny,
                                                      SecAccessControlCreateFlags.privateKeyUsage],
                                                     &error)
        guard error == nil else {
            describeSecurityError(error!)
            return nil
        }
        return access
    }

    static func generateBiometricPrivateKey(network: String) -> Bool {
        guard let acl = getACL() else {
            return false
        }

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
            describeSecurityError(error!)
            return false
        }

        UserDefaults.standard.set(privateKeyLabel, forKey: "AuthKeyBiometricPrivateKey" + network)

        return true
    }

    fileprivate static func getPrivateKey(forNetwork: String) -> SecKey? {
        let privateKeyLabel = UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + forNetwork)
        let q: [CFString: Any] = [kSecClass: kSecClassKey,
                                  kSecAttrKeyType: ECCKeyType,
                                  kSecAttrKeySizeInBits: ECCKeySizeInBits,
                                  kSecAttrLabel: privateKeyLabel!,
                                  kSecReturnRef: true,
                                  kSecUseOperationPrompt: "Unlock Green"]

        var privateKey: CFTypeRef?
        let status = SecItemCopyMatching(q as CFDictionary, &privateKey)
        guard status == errSecSuccess else {
            _ = describeKeychainError(status)
            return nil
        }
        return (privateKey as! SecKey)
    }

    fileprivate static func getPublicKey(forNetwork: String) -> SecKey? {
        let privateKey = getPrivateKey(forNetwork: forNetwork)
        guard privateKey != nil else {
            return nil
        }
        return SecKeyCopyPublicKey(privateKey!)
    }

    fileprivate static func decrypt(base64Encoded: Data, forNetwork: String) throws -> String? {
        let privateKey = getPrivateKey(forNetwork: forNetwork)
        guard privateKey != nil else {
            return nil
        }

        let canDecrypt = SecKeyIsAlgorithmSupported(privateKey!, SecKeyOperationType.decrypt, ECCEncryptionType)
        guard canDecrypt else {
            NSLog("Operation failed: Decryption algorithm not supported.")
#if DEBUG
            NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
            return nil
        }

        var error: Unmanaged<CFError>?
        let decrypted = SecKeyCreateDecryptedData(privateKey!, ECCEncryptionType, base64Encoded as CFData, &error)
        guard error == nil else {
            describeSecurityError(error!)
            throw AuthError.CanceledByUser
        }
        return String(data: decrypted! as Data, encoding: .utf8)
    }

    fileprivate static func encrypt(plaintext: String, forNetwork: String) -> String? {
        let publicKey = getPublicKey(forNetwork: forNetwork)
        guard publicKey != nil else {
            return nil
        }

        let canEncrypt = SecKeyIsAlgorithmSupported(publicKey!, SecKeyOperationType.encrypt, ECCEncryptionType)
        guard canEncrypt else {
            NSLog("Operation failed: Encryption algorithm not supported.")
#if DEBUG
            NSLog("Stacktrace: \(Thread.callStackSymbols)")
#endif
            return nil
        }

        var error: Unmanaged<CFError>?
        let data = plaintext.data(using: .utf8, allowLossyConversion: false)
        let encrypted = SecKeyCreateEncryptedData(publicKey!, ECCEncryptionType, data! as CFData, &error)
        guard error == nil else {
            describeSecurityError(error!)
            return nil
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
        let data = try? JSONSerialization.data(withJSONObject: data)
        guard data != nil else {
            throw AuthError.ServiceNotAvailable
        }
        let q = queryFor(method: method, forNetwork: forNetwork)
        let qAdd = q.merging([kSecValueData: data!]) { (current, _) in current }
        var status = callWrapper(fun: SecItemAdd(qAdd as CFDictionary, nil))
        if status == errSecDuplicateItem {
            status = callWrapper(fun: SecItemDelete(q as CFDictionary))
            status = callWrapper(fun: SecItemAdd(qAdd as CFDictionary, nil))
        } else if status != errSecSuccess {
            throw AuthError.Unknown(desc: "UnknownSecStatusCode: \(status)")
        }
    }

    fileprivate static func get_(method: String, forNetwork: String) -> [String: Any]? {
        let q = queryForData(method: method, forNetwork: forNetwork)
        var result: CFTypeRef?
        let status = callWrapper(fun: SecItemCopyMatching(q as CFDictionary, &result))
        guard status == errSecSuccess, result != nil, let resultData = result as? Data else {
            return nil
        }
        let data = try? JSONSerialization.jsonObject(with: resultData, options: [])
        guard data != nil else {
            return nil
        }
        return (data! as? [String: Any])
    }

    fileprivate static func get(method: String, toDecrypt: Bool, forNetwork: String) throws -> [String: Any]? {
        guard let data = get_(method: method, forNetwork: forNetwork) else {
            return nil
        }
        var extended = data
        if toDecrypt {
            precondition(method == AuthKeyBiometric)
            let encryptedBiometric = data["encrypted_biometric"] as? String
            guard let decoded = Data(base64Encoded: encryptedBiometric!),
                let plaintext = try decrypt(base64Encoded: decoded, forNetwork: forNetwork) else {
                    return nil
            }
            extended["plaintext_biometric"] = plaintext
        }
        return extended
    }

    static func getAuth(method: String, forNetwork: String) throws -> [String: Any]? {
        return try get(method: method, toDecrypt: method == AuthKeyBiometric, forNetwork: forNetwork)
    }

    static func findAuth(method: String, forNetwork: String) -> Bool {
        return get_(method: method, forNetwork: forNetwork) != nil
    }

    static func removePrivateKey(forNetwork: String) {
        let privateKeyLabel = UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + forNetwork)
        let q: [CFString: Any] = [kSecClass: kSecClassKey,
                                  kSecAttrKeyType: ECCKeyType,
                                  kSecAttrKeySizeInBits: ECCKeySizeInBits,
                                  kSecAttrLabel: privateKeyLabel!,
                                  kSecReturnRef: true]
        _ = callWrapper(fun: SecItemDelete(q as CFDictionary))
    }

    static func removeAuth(method: String, forNetwork: String) -> Bool {
        let q = queryForData(method: method, forNetwork: forNetwork)
        return callWrapper(fun: SecItemDelete(q as CFDictionary)) == errSecSuccess
    }

    static func addBiometryType(data: [String: Any], extraData: String, forNetwork: String) throws {
        guard let encrypted = encrypt(plaintext: extraData, forNetwork: forNetwork) else {
            throw AuthError.ServiceNotAvailable
        }
        var extended = data
        extended["encrypted_biometric"] = encrypted
        try set(method: AuthKeyBiometric, data: extended, forNetwork: forNetwork)
    }

    static func addPIN(data: [String: Any], forNetwork: String) throws {
        try set(method: AuthKeyPIN, data: data, forNetwork: forNetwork)
    }
}
