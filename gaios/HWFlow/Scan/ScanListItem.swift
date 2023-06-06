import Foundation

struct ScanListItem {
    let identifier: UUID
    let name: String
    var ledger: Bool { name.contains("Nano") }
    var jade: Bool { name.contains("Jade") }
    var type: DeviceType? { jade ? .Jade : ledger ? .Ledger : nil }
}
