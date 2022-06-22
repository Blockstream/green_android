import Foundation

struct PinData: Codable {
    enum CodingKeys: String, CodingKey {
        case encryptedData = "encrypted_data"
        case pinIdentifier = "pin_identifier"
        case salt
        case encryptedBiometric = "encrypted_biometric"
    }
    let encryptedData: String
    let pinIdentifier: String
    let salt: String
    var encryptedBiometric: String?
}
