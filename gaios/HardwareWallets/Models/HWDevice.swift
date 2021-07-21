import Foundation

struct HWDevice: Codable {
    enum CodingKeys: String, CodingKey {
        case name
        case supportsArbitraryScripts = "supports_arbitrary_scripts"
        case supportsLowR = "supports_low_r"
        case supportsLiquid = "supports_liquid"
        case supportsAntiExfilProtocol = "supports_ae_protocol"
    }

    let name: String
    let supportsArbitraryScripts: Bool
    let supportsLowR: Bool
    let supportsLiquid: Int
    let supportsAntiExfilProtocol: Int

    var isJade: Bool { "jade" == name.lowercased() }
    var isTrezor: Bool { "trezor" == name.lowercased() }
    var isLedger: Bool { "ledger" == name.lowercased() }
}
