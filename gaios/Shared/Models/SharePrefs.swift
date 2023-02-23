import UIKit

enum SharePrefs: Int, CaseIterable {
    case address = 0
    case qr = 1

    var name: String {
        switch self {
        case .address:
            return "id_address".localized
        case .qr:
            return "id_qr_code".localized
        }
    }

    var icon: UIImage {
        switch self {
        case .address:
            return UIImage(named: "ic_dialog_text_Aa")!.maskWithColor(color: .white)
        case .qr:
            return UIImage(named: "ic_any_asset")!.maskWithColor(color: .white)
        }
    }

    static func getItems() -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []
        SharePrefs.allCases.forEach {
            items.append(DialogListCellModel(type: .list,
                                             icon: $0.icon,
                                             title: $0.name))
        }
        return items
    }
}
