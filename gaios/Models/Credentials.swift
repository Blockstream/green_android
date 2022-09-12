import Foundation

struct Credentials: Codable {
    enum CodingKeys: String, CodingKey {
        case mnemonic
        case bip39Passphrase = "bip39_passphrase"
        case pin
        case pinData = "pin_data"
        case username
        case password
    }
    let mnemonic: String?
    let bip39Passphrase: String?
    let pin: String?
    let pinData: PinData?
    let username: String?
    let password: String?

    init(mnemonic: String? = nil,
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
