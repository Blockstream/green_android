import Foundation
import UIKit

struct Account: Codable, Equatable {
    var name: String
    var network: String
    let id: String
    let isJade: Bool
    let isLedger: Bool
    let username: String?
    var password: String?
    let keychain: String

    init(name: String, network: String, isJade: Bool = false, isLedger: Bool = false) {
        // Software / Hardware wallet account
        id = UUID().uuidString
        self.name = name
        self.network = network
        self.isJade = isJade
        self.isLedger = isLedger
        self.username = nil
        self.password = nil
        self.keychain = id
    }

    init(name: String, network: String, username: String, password: String? = nil) {
        // Watchonly account
        id = UUID().uuidString
        self.name = name
        self.network = network
        self.isJade = false
        self.isLedger = false
        self.username = username
        self.password = password
        self.keychain = id
    }

    init(name: String, network: String, keychain: String) {
        // Migrated account
        id = UUID().uuidString
        self.name = name
        self.network = network
        self.keychain = keychain
        self.isJade = false
        self.isLedger = false
        self.username = nil
        self.password = nil
    }

    var isWatchonly: Bool {
        get {
            return !(username?.isEmpty ?? true)
        }
    }

    var hasManualPin: Bool {
        get {
            return AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: keychain)
        }
    }

    var hasBioPin: Bool {
        get {
            AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: keychain)
        }
    }

    var hasPin: Bool {
        get {
            return hasManualPin || hasBioPin
        }
    }

    func auth(_ method: String) throws -> [String: Any] {
        return try AuthenticationTypeHandler.getAuth(method: method, forNetwork: keychain)
    }

    var icon: UIImage {
        get {
            if isJade || isLedger {
                return UIImage(named: "ic_hww")!
            }
            switch network {
            case "mainnet":
                return UIImage(named: "ntw_btc")!
            case "liquid":
                return UIImage(named: "ntw_liquid")!
            default:
                return UIImage(named: "ntw_testnet")!
            }
        }
    }

    var attempts: Int {
        get {
            return UserDefaults.standard.integer(forKey: keychain + "_pin_attempts")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: keychain + "_pin_attempts")
        }
    }

    var gdkNetwork: GdkNetwork {
        get {
            getGdkNetwork(network)
        }
    }

    func removeBioKeychainData() {
        _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: keychain)
        try? AuthenticationTypeHandler.removePrivateKey(forNetwork: keychain)
        UserDefaults.standard.set(nil, forKey: "AuthKeyBiometricPrivateKey" + keychain)
    }

    func removePinKeychainData() {
        _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: keychain)
    }

    func addBioPin(session: Session) throws {
        if UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + keychain) == nil {
            try AuthenticationTypeHandler.generateBiometricPrivateKey(network: keychain)
        }
        let password = String.random(length: 14)
        let deviceid = String.random(length: 14)
        let mnemonics = try session.getMnemonicPassphrase(password: "")
        guard let pindata = try session.setPin(mnemonic: mnemonics, pin: password, device: deviceid) else {
            throw AuthenticationTypeHandler.AuthError.NotSupported
        }
        try AuthenticationTypeHandler.addBiometryType(data: pindata, extraData: password, forNetwork: keychain)
    }

    func addPin(session: Session, pin: String, mnemonic: String) throws {
        let deviceid = String.random(length: 14)
        guard let pindata = try session.setPin(mnemonic: mnemonic, pin: pin, device: deviceid) else {
            throw AuthenticationTypeHandler.AuthError.NotSupported
        }
        try AuthenticationTypeHandler.addPIN(data: pindata, forNetwork: keychain)
    }

}
