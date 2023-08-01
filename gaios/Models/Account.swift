import Foundation
import UIKit

import gdk

struct Account: Codable, Equatable {

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case isJade
        case isLedger
        case username
        case password
        case keychain
        case chain = "network"
        case network = "gdknetwork"
        case isSingleSig
        case walletHashId = "wallet_hash_id"
        case askEphemeral = "ask_ephemeral"
        case xpubHashId = "xpub_hash_id"
        case hidden
        case uuid
    }

    var name: String
    let id: String
    let isJade: Bool
    let isLedger: Bool
    let username: String?
    var password: String?
    let keychain: String
    private var network: String? // use NetworkType for retro-compatibility
    private var chain: String? // legacy field
    private var isSingleSig: Bool? // legacy field
    var walletHashId: String?
    var xpubHashId: String?
    var hidden: Bool? = false
    var uuid: UUID?
    var isEphemeral: Bool = false
    var askEphemeral: Bool?
    var ephemeralId: Int?

    init(id: String? = nil, name: String, network: NetworkSecurityCase, isJade: Bool = false, isLedger: Bool = false, isSingleSig: Bool? = nil, isEphemeral: Bool = false, askEphemeral: Bool = false, xpubHashId: String? = nil, uuid: UUID? = nil, hidden: Bool = false) {
        // Software / Hardware wallet account
        self.id = id ?? UUID().uuidString
        self.name = name
        self.network = network.network
        self.isJade = isJade
        self.isLedger = isLedger
        self.username = nil
        self.password = nil
        self.keychain = self.id
        self.isSingleSig = isSingleSig
        self.isEphemeral = isEphemeral
        self.askEphemeral = askEphemeral
        self.xpubHashId = xpubHashId
        self.uuid = uuid
        self.hidden = hidden
        if isEphemeral {
            let ephAccounts = AccountsRepository.shared.ephAccounts
            if ephAccounts.count == 0 {
                self.ephemeralId = 1
            } else {
                if let last = ephAccounts.sorted(by: { ($0.ephemeralId ?? 0) > ($1.ephemeralId ?? 0) }).first, let id = last.ephemeralId {
                    self.ephemeralId = id + 1
                }
            }
        }
    }

    init(name: String, network: NetworkSecurityCase, username: String, password: String? = nil) {
        // Watchonly account
        id = UUID().uuidString
        self.name = name
        self.network = network.network
        self.isJade = false
        self.isLedger = false
        self.username = username
        self.password = password
        self.keychain = id
    }

    init(name: String, network: NetworkSecurityCase, keychain: String) {
        // Migrated account
        id = UUID().uuidString
        self.name = name
        self.network = network.network
        self.keychain = keychain
        self.isJade = false
        self.isLedger = false
        self.username = nil
        self.password = nil
    }

    var isHW: Bool { isJade || isLedger }
    var isWatchonly: Bool { username != nil }

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

    func removeLightningCredentials() {
        if let walletHashId = walletHashId {
            LightningRepository.shared.remove(for: walletHashId)
        }
    }
    
    func addBiometrics(session: SessionManager, credentials: Credentials) async throws {
        let password = String.random(length: 14)
        let params = EncryptWithPinParams(pin: password, credentials: credentials)
        let encrypted = try await session.encryptWithPin(params)
        try AuthenticationTypeHandler.addBiometry(pinData: encrypted.pinData, extraData: password, forNetwork: keychain)
    }

    func addPin(session: SessionManager, pin: String, mnemonic: String) async throws {
        let params = EncryptWithPinParams(pin: pin, credentials: Credentials(mnemonic: mnemonic))
        let encrypted = try await session.encryptWithPin(params)
        try AuthenticationTypeHandler.addPIN(pinData: encrypted.pinData, forNetwork: keychain)
    }

    var gdkNetwork: GdkNetwork { networkType.gdkNetwork }
    var networkType: NetworkSecurityCase {
        get {
            if let network = network {
                return NetworkSecurityCase(rawValue: network) ?? .bitcoinSS
            }
            let chain = self.chain ?? "mainnet"
            let name =  isSingleSig ?? false ? Constants.electrumPrefix + chain : chain
            return NetworkSecurityCase(rawValue: name) ?? .bitcoinSS
        }
        set {
            self.network = newValue.rawValue
        }
    }
}
