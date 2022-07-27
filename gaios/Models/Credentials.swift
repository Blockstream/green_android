import Foundation

struct Credentials: Codable {
    enum CodingKeys: String, CodingKey {
        case mnemonic = "mnemonic"
        case password = "password"
        case bip39Passphrase = "bip39_passphrase"
    }
    let mnemonic: String
    let password: String?
    let bip39Passphrase: String?
}
