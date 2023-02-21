import UIKit

enum WalletPrefs: Int, CaseIterable {
    case settings = 0
    case createAccount = 1
    case logout = 2

    var name: String {
        switch self {
        case .settings:
            return "id_settings".localized
        case .createAccount:
            return "Create Account"
        case .logout:
            return "id_logout".localized
        }
    }

    var icon: UIImage {
        switch self {
        case .settings:
            return UIImage(named: "ic_dialog_gear_six")!
        case .createAccount:
            return UIImage(named: "ic_dialog_plus_circle")!
        case .logout:
            return UIImage(named: "ic_logout")!
        }
    }

    static func getItems() -> [DialogListCellModel] {
        let isWatchOnly = AccountsManager.shared.current?.isWatchonly ?? false
        let prefs: [WalletPrefs] = isWatchOnly ? [ .settings, .logout ] : [ .settings, .createAccount, .logout ]
        return prefs.map { DialogListCellModel(type: .list,
                                               icon: $0.icon,
                                               title: $0.name) }
    }
}
