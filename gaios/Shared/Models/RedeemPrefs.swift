import UIKit

enum RedeemPrefs: Int, CaseIterable {
    case scan = 0
    case type = 1

    var name: String {
        switch self {
        case .scan:
            return "Scan QR Code"
        case .type:
            return "Enter Code"
        }
    }

    var icon: UIImage {
        switch self {
        case .scan:
            return UIImage(named: "ic_dialog_qr")!.maskWithColor(color: .white)
        case .type:
            return UIImage(named: "ic_dialog_text_Aa")!.maskWithColor(color: .white)
        }
    }

    static func getItems() -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []
        RedeemPrefs.allCases.forEach {
            items.append(DialogListCellModel(type: .list,
                                             icon: $0.icon,
                                             title: $0.name))
        }
        return items
    }
}
