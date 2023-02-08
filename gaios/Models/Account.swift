import Foundation
import UIKit
import PromiseKit

struct Account: Codable, Equatable {

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case isJade
        case isLedger
        case username
        case password
        case keychain
        case network
        case isSingleSig
        case walletHashId = "wallet_hash_id"
        case askEphemeral = "ask_ephemeral"
        case xpubHashId = "xpub_hash_id"
    }

    var name: String
    let id: String
    let isJade: Bool
    let isLedger: Bool
    let username: String?
    var password: String?
    let keychain: String
    var network: String
    var isSingleSig: Bool? // optional to support pre singleSig stored wallets
    var walletHashId: String?
    var xpubHashId: String?
    var gdkNetwork: GdkNetwork? { get { getGdkNetwork(networkName) }}
    var isEphemeral: Bool = false
    var askEphemeral: Bool?
    var ephemeralId: Int?

    init(id: String? = nil, name: String, network: String, isJade: Bool = false, isLedger: Bool = false, isSingleSig: Bool?, isEphemeral: Bool = false, askEphemeral: Bool = false) {
        // Software / Hardware wallet account
        self.id = id ?? UUID().uuidString
        self.name = name
        self.network = network
        self.isJade = isJade
        self.isLedger = isLedger
        self.username = nil
        self.password = nil
        self.keychain = self.id
        self.isSingleSig = isSingleSig
        self.isEphemeral = isEphemeral
        self.askEphemeral = askEphemeral
        if isEphemeral {
            let ephAccounts = AccountsManager.shared.ephAccounts
            if ephAccounts.count == 0 {
                self.ephemeralId = 1
            } else {
                if let last = ephAccounts.sorted(by: { ($0.ephemeralId ?? 0) > ($1.ephemeralId ?? 0) }).first, let id = last.ephemeralId {
                    self.ephemeralId = id + 1
                }
            }
        }
    }

    init(name: String, network: String, username: String, password: String? = nil, isSingleSig: Bool) {
        // Watchonly account
        id = UUID().uuidString
        self.name = name
        self.network = network
        self.isJade = false
        self.isLedger = false
        self.username = username
        self.password = password
        self.keychain = id
        self.isSingleSig = isSingleSig
    }

    init(name: String, network: String, keychain: String, isSingleSig: Bool) {
        // Migrated account
        id = UUID().uuidString
        self.name = name
        self.network = network
        self.keychain = keychain
        self.isJade = false
        self.isLedger = false
        self.username = nil
        self.password = nil
        self.isSingleSig = isSingleSig
    }

    var isHW: Bool {
        get {
            return isJade || isLedger
        }
    }

    var isWatchonly: Bool {
        get {
            return !(username?.isEmpty ?? true)
        }
    }

    var hasManualPin: Bool {
        get {
            return AuthenticationTypeHandler.findAuth(method: .AuthKeyPIN, forNetwork: keychain)
        }
    }

    var hasBioPin: Bool {
        get {
            AuthenticationTypeHandler.findAuth(method: .AuthKeyBiometric, forNetwork: keychain)
        }
    }

    var hasPin: Bool {
        get {
            return hasManualPin || hasBioPin
        }
    }

    func auth(_ method: AuthenticationTypeHandler.AuthType) throws -> PinData {
        return try AuthenticationTypeHandler.getAuth(method: method, forNetwork: keychain)
    }

    var icon: UIImage {
        get {
            switch network {
            case "mainnet":
                return UIImage(named: "ntw_btc")!
            case "liquid":
                return UIImage(named: "ntw_liquid")!
            case "testnet-liquid":
                return UIImage(named: "ntw_testnet_liquid")!
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

    func removeBioKeychainData() {
        _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyBiometric, forNetwork: keychain)
        try? AuthenticationTypeHandler.removePrivateKey(forNetwork: keychain)
        UserDefaults.standard.set(nil, forKey: "AuthKeyBiometricPrivateKey" + keychain)
    }

    func removePinKeychainData() {
        _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyPIN, forNetwork: keychain)
    }

    func addBioPin(session: SessionManager) -> Promise<Void> {
        let password = String.random(length: 14)
        let authKeyBiometricPrivateKey = UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + keychain)
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) {
                if authKeyBiometricPrivateKey == nil {
                    try AuthenticationTypeHandler.generateBiometricPrivateKey(network: keychain)
                }
            }.then(on: bgq) {
                session.getCredentials(password: "") }
            .then(on: bgq) {
                session.encryptWithPin(pin: password, text: ["mnemonic": $0.mnemonic]) }
            .compactMap(on: bgq) { try AuthenticationTypeHandler.addBiometryType(pinData: $0, extraData: password, forNetwork: keychain) }
    }

    func addPin(session: SessionManager, pin: String, mnemonic: String) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) {
                session.encryptWithPin(pin: pin, text: ["mnemonic": mnemonic]) }
            .map(on: bgq) {
                try AuthenticationTypeHandler.addPIN(pinData: $0, forNetwork: keychain) }
    }

    var networkName: String {
        get {
            let isSingleSig = self.isSingleSig ?? false
            let ntw = self.network

            return (isSingleSig ? Constants.electrumPrefix + ntw : ntw)
        }
    }

    var activeWallet: UInt32 {
        get {
            let pointerKey = String(format: "%@_wallet_pointer", id)
            if UserDefaults.standard.object(forKey: pointerKey) != nil {
                return UInt32(UserDefaults.standard.integer(forKey: pointerKey))
            }
            return UInt32(isSingleSig ?? false ? 1 : 0)
        }
        set {
            let pointerKey = String(format: "%@_wallet_pointer", id)
            UserDefaults.standard.set(Int(newValue), forKey: pointerKey)
            UserDefaults.standard.synchronize()
        }
    }
}
