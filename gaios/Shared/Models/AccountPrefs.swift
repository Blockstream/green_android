import UIKit

enum AccountPrefs: Int, CaseIterable {
    case rename = 0
    case archive = 1
    case enhanceSecurity = 2

    var name: String {
        switch self {
        case .rename:
            return "Rename"
        case .archive:
            return "Archive"
        case .enhanceSecurity:
            return "Enhance Security"
        }
    }

    var icon: UIImage {
        switch self {
        case .rename:
            return UIImage(named: "ic_dialog_text_Aa")!
        case .archive:
            return UIImage(named: "ic_dialog_arrow_down")!
        case .enhanceSecurity:
            return UIImage(named: "ic_shield_check")!
        }
    }

    static func getItems() -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []
        AccountPrefs.allCases.forEach {
            items.append(DialogListCellModel(type: .list,
                                             icon: $0.icon,
                                             title: $0.name))
        }
        return items
    }
}
