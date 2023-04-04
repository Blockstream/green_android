import UIKit

enum LoginPrefs: Int, CaseIterable {
    case passphrase
    case emergency
    case edit
    case delete

    var name: String {
        switch self {
        case .passphrase:
            return "id_login_with_bip39_passphrase".localized
        case .emergency:
            return "id_show_recovery_phrase".localized
        case .edit:
            return "id_rename_wallet".localized
        case .delete:
            return "id_remove_wallet".localized
        }
    }

    var icon: UIImage {
        switch self {
        case .passphrase:
            return UIImage(named: "ic_dialog_pwd39")!.maskWithColor(color: .white)
        case .emergency:
            return UIImage(named: "ic_dialog_show_phrase")!.maskWithColor(color: .white)
        case .edit:
            return UIImage(named: "ic_dialog_text_Aa")!.maskWithColor(color: .white)
        case .delete:
            return UIImage(named: "ic_dialog_remove")!.maskWithColor(color: .white)
        }
    }

    static func getItems(isWatchOnly: Bool) -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []

        let data: [LoginPrefs] = isWatchOnly ? [.edit, .delete] : LoginPrefs.allCases
        data.forEach {
            items.append(DialogListCellModel(type: .list,
                                             icon: $0.icon,
                                             title: $0.name))
        }
        return items
    }
}
