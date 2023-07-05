import Foundation

struct ScanListItem {
    let identifier: UUID
    let name: String
    var ledger: Bool { type == .Ledger }
    var jade: Bool { type == .Jade }
    var type: DeviceType
}
