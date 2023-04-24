import Foundation

public struct Credentials: Codable {
    public enum CodingKeys: String, CodingKey {
        case mnemonic
        case bip39Passphrase = "bip39_passphrase"
        case pin
        case pinData = "pin_data"
        case username
        case password
        case coreDescriptors = "core_descriptors"
        case slip132ExtendedPubkeys = "slip132_extended_pubkeys"
    }
    public let mnemonic: String?
    public let bip39Passphrase: String?
    public let pin: String?
    public let pinData: PinData?
    public let username: String?
    public let password: String?
    public let coreDescriptors: [String]?
    public let slip132ExtendedPubkeys: [String]?

    public init(mnemonic: String? = nil,
                bip39Passphrase: String? = nil,
                pin: String? = nil,
                pinData: PinData? = nil,
                username: String? = nil,
                password: String? = nil,
                coreDescriptors: [String]? = nil,
                slip132ExtendedPubkeys: [String]? = nil) {
        self.mnemonic = mnemonic
        self.bip39Passphrase = bip39Passphrase
        self.pin = pin
        self.pinData = pinData
        self.username = username
        self.password = password
        self.coreDescriptors = coreDescriptors
        self.slip132ExtendedPubkeys = slip132ExtendedPubkeys
    }

    public static func watchonlyMultisig(username: String, password: String) -> Credentials {
        return Credentials(username: username, password: password)
    }

    public static func watchonlySinglesig(coreDescriptors: [String]? = nil, slip132ExtendedPubkeys: [String]? = nil) -> Credentials {
        return Credentials(coreDescriptors: coreDescriptors, slip132ExtendedPubkeys: slip132ExtendedPubkeys)
    }
}
