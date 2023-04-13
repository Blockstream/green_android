import Foundation

public struct Credentials: Codable {
    public enum CodingKeys: String, CodingKey {
        case mnemonic
        case bip39Passphrase = "bip39_passphrase"
        case pin
        case pinData = "pin_data"
        case username
        case password
    }
    public let mnemonic: String?
    public let bip39Passphrase: String?
    public let pin: String?
    public let pinData: PinData?
    public let username: String?
    public let password: String?

    public init(mnemonic: String? = nil,
         bip39Passphrase: String? = nil,
         pin: String? = nil,
         pinData: PinData? = nil,
         username: String? = nil,
         password: String? = nil) {
        self.mnemonic = mnemonic
        self.bip39Passphrase = bip39Passphrase
        self.pin = pin
        self.pinData = pinData
        self.username = username
        self.password = password
    }
}
