import Foundation

struct HWDevice: Codable {
    enum CodingKeys: String, CodingKey {
        case name
        case supportsArbitraryScripts = "supports_arbitrary_scripts"
        case supportsLowR = "supports_low_r"
        case supportsLiquid = "supports_liquid"
        case supportsAntiExfilProtocol = "supports_ae_protocol"
        case supportsHostUnblinding = "supports_host_unblinding"
        case deviceType = "device_type"
    }

    let name: String
    let supportsArbitraryScripts: Bool
    let supportsLowR: Bool
    let supportsLiquid: Int
    let supportsAntiExfilProtocol: Int
    let supportsHostUnblinding: Bool
    let deviceType: String = "hardware"

    var isJade: Bool { "jade" == name.lowercased() }
    var isTrezor: Bool { "trezor" == name.lowercased() }
    var isLedger: Bool { "ledger" == name.lowercased() }
}
