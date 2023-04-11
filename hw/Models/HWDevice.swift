import Foundation

public struct HWDevice: Codable {
    enum CodingKeys: String, CodingKey {
        case name
        case supportsArbitraryScripts = "supports_arbitrary_scripts"
        case supportsLowR = "supports_low_r"
        case supportsLiquid = "supports_liquid"
        case supportsAntiExfilProtocol = "supports_ae_protocol"
        case supportsHostUnblinding = "supports_host_unblinding"
        case deviceType = "device_type"
    }

    public let name: String
    public let supportsArbitraryScripts: Bool
    public let supportsLowR: Bool
    public let supportsLiquid: Int
    public let supportsAntiExfilProtocol: Int
    public let supportsHostUnblinding: Bool
    public let deviceType: String = "hardware"

    public var isJade: Bool { "jade" == name.lowercased() }
    public var isTrezor: Bool { "trezor" == name.lowercased() }
    public var isLedger: Bool { "ledger" == name.lowercased() }

    public static func defaultLedger() -> HWDevice {
        return HWDevice(name: "Ledger",
                        supportsArbitraryScripts: true,
                        supportsLowR: false,
                        supportsLiquid: 0,
                        supportsAntiExfilProtocol: 0,
                        supportsHostUnblinding: false)
    }

    public static func defaultJade(fmwVersion: String?) -> HWDevice {
        let supportUnblinding = fmwVersion == nil || fmwVersion! >= "0.1.27"
        return HWDevice(name: "Jade",
                        supportsArbitraryScripts: true,
                        supportsLowR: true,
                        supportsLiquid: 1,
                        supportsAntiExfilProtocol: 1,
                        supportsHostUnblinding: supportUnblinding)
    }
}
