import Foundation

public struct PinData: Codable {
    enum CodingKeys: String, CodingKey {
        case encryptedData = "encrypted_data"
        case pinIdentifier = "pin_identifier"
        case salt
        case encryptedBiometric = "encrypted_biometric"
        case plaintextBiometric = "plaintext_biometric"
    }
    public let encryptedData: String
    public let pinIdentifier: String
    public let salt: String
    public var encryptedBiometric: String?
    public var plaintextBiometric: String?
}
