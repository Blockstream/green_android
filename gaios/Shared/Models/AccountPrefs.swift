import UIKit

enum AccountPrefs: Int, CaseIterable {
    case rename = 0
    case archive = 1
    //case enhanceSecurity = 2

    var name: String {
        switch self {
        case .rename:
            return "Rename"
        case .archive:
            return "Archive"
        //case .enhanceSecurity:
        //    return "Enhance Security"
        }
    }

    var icon: UIImage {
        switch self {
        case .rename:
            return UIImage(named: "ic_dialog_text_Aa")!
        case .archive:
            return UIImage(named: "ic_dialog_arrow_down")!
        //case .enhanceSecurity:
        //    return UIImage(named: "ic_dialog_shield_check")!
        }
    }

    static func getItems() -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []
        items += [DialogListCellModel(type: .list,
                                      icon: AccountPrefs.rename.icon,
                                      title: AccountPrefs.rename.name)]
        if let subaccount = WalletManager.current?.subaccounts,
           subaccount.filter({ !($0.hidden ?? false) }).count > 1 {
            items += [DialogListCellModel(type: .list,
                                          icon: AccountPrefs.archive.icon,
                                          title: AccountPrefs.archive.name)]
        }
        return items
    }
}
