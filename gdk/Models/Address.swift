import Foundation

import BreezSDK
import hw

public struct Address: Codable {

    enum CodingKeys: String, CodingKey {
        case address = "address"
        case pointer = "pointer"
        case branch = "branch"
        case userPath = "user_path"
        case subaccount = "subaccount"
        case scriptType = "script_type"
        case addressType = "address_type"
        case script = "script"
        case subtype = "subtype"
    }

    public let address: String?
    public let pointer: Int?
    public let branch: Int?
    public let subtype: UInt32?
    public let userPath: [UInt32]?
    public let subaccount: UInt32?
    public let scriptType: Int?
    public let addressType: String?
    public let script: String?

    public init(address: String? = nil, pointer: Int? = nil, branch: Int? = nil, subtype: UInt32? = nil, userPath: [UInt32]? = nil, subaccount: UInt32? = nil, scriptType: Int? = nil, addressType: String? = nil, script: String? = nil) {
        self.address = address
        self.pointer = pointer
        self.branch = branch
        self.subtype = subtype
        self.userPath = userPath
        self.subaccount = subaccount
        self.scriptType = scriptType
        self.addressType = addressType
        self.script = script
    }

    public static func from(invoice: LnInvoice) -> Address {
        return Address(address: invoice.bolt11)
    }

    public static func from(swapInfo: SwapInfo) -> Address {
        return Address(address: swapInfo.bitcoinAddress)
    }
}
