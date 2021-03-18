import Foundation
import UIKit

struct Account: Codable {
    let name: String
    let network: String
    let id: String
    let isJade: Bool
    let isLedger: Bool

    init(name: String, network: String) {
        id = UUID().uuidString
        self.name = name
        self.network = network
        self.isJade = false
        self.isLedger = false
    }

    var hasManualPin: Bool {
        get {
            return AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: id)
        }
    }

    var hasBioPin: Bool {
        get {
            AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: id)
        }
    }

    var hasPin: Bool {
        get {
            return hasManualPin || hasBioPin
        }
    }

    func auth(_ method: String) throws -> [String: Any] {
        return try AuthenticationTypeHandler.getAuth(method: method, forNetwork: id)
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
            return UserDefaults.standard.integer(forKey: id + "_pin_attempts")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: id + "_pin_attempts")
        }
    }

    func removeBioKeychainData() {
        let network = getNetwork()
        _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
        try? AuthenticationTypeHandler.removePrivateKey(forNetwork: self.network)
        UserDefaults.standard.set(nil, forKey: "AuthKeyBiometricPrivateKey" + self.network)
    }

    func removePinKeychainData() {
        let network = getNetwork()
        _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
    }
}
