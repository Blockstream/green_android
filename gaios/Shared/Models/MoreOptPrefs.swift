import UIKit

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
            return "Authenticate Address"
        }
    }

    var icon: UIImage {
        switch self {
        case .requestAmount:
            return UIImage(named: "ic_dialog_arrow_down_square")!.maskWithColor(color: .white)
        case .sweep:
            return UIImage(named: "ic_dialog_sweep_wallet")!.maskWithColor(color: .white)
        case .addressAuth:
            return UIImage(named: "ic_address_auth")!.maskWithColor(color: .white)
        }
    }

    static func getPrefs(hideSweep: Bool) -> [MoreOptPrefs] {
        let prefs: [MoreOptPrefs] = hideSweep ? [ .requestAmount, .addressAuth ] : [ .requestAmount, .sweep, .addressAuth ]
        return prefs
    }

    static func getItems(hideSweep: Bool) -> [DialogListCellModel] {
        return MoreOptPrefs.getPrefs(hideSweep: hideSweep).map { DialogListCellModel(type: .list,
                                                                icon: $0.icon,
                                                                title: $0.name) }
    }
}
