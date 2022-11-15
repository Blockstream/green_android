import UIKit

enum WalletPrefs: Int, CaseIterable {
    case settings = 0
    case createAccount = 1

    var name: String {
        switch self {
        case .settings:
            return "Network Settings"
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
        var items: [DialogListCellModel] = []
        WalletPrefs.allCases.forEach {
            items.append(DialogListCellModel(icon: $0.icon, title: $0.name))
        }
        return items
    }
}
