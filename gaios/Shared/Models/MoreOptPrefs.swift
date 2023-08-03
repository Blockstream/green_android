import UIKit
import gdk

enum MoreOptPrefs: Int, CaseIterable {
    case requestAmount = 0
    case sweep = 1
    case addressAuth = 2

    var name: String {
        switch self {
        case .requestAmount:
            return "id_request_amount".localized
        case .sweep:
            return "id_sweep_from_paper_wallet".localized
        case .addressAuth:
            return "List of Addresses"
        }
    }

    var icon: UIImage {
        switch self {
        case .requestAmount:
            return UIImage(named: "ic_dialog_arrow_down_square")!.maskWithColor(color: .white)
        case .sweep:
            return UIImage(named: "ic_dialog_sweep_wallet")!.maskWithColor(color: .white)
        case .addressAuth:
            return UIImage(named: "ic_address_auth_list")!.maskWithColor(color: .white)
        }
    }

    static func getPrefs(account: WalletItem) -> [MoreOptPrefs] {

        let hideSweep = account.gdkNetwork.liquid || account.gdkNetwork.electrum  || account.gdkNetwork.lightning
        let hideSign = account.gdkNetwork.lightning
        
        var prefs: [MoreOptPrefs] = [ .requestAmount ]

        if hideSweep == false {
            prefs.append(.sweep)
        }
        if hideSign == false {
            prefs.append(.addressAuth)
        }
        return prefs
    }

    static func getItems(account: WalletItem) -> [DialogListCellModel] {

        return MoreOptPrefs.getPrefs(account: account).map { DialogListCellModel(type: .list,
                                                                icon: $0.icon,
                                                                title: $0.name) }
    }
}
