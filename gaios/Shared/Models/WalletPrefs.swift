import UIKit

enum WalletPrefs: Int, CaseIterable {
    case settings = 0
    case createAccount = 1

    var name: String {
        switch self {
        case .settings:
            return "Settings"
        case .createAccount:
            return "Create Account"
        }
    }

    var icon: UIImage {
        switch self {
        case .settings:
            return UIImage(named: "ic_dialog_gear_six")!
        case .createAccount:
            return UIImage(named: "ic_dialog_plus_circle")!
        }
    }

    static func getItems() -> [DialogListCellModel] {
        let isWatchOnly = AccountsManager.shared.current?.isWatchonly ?? false
        let prefs: [WalletPrefs] = isWatchOnly ? [ .settings ] : [ .settings, .createAccount ]
        return prefs.map { DialogListCellModel(type: .list,
                                               icon: $0.icon,
                                               title: $0.name) }
    }
}
