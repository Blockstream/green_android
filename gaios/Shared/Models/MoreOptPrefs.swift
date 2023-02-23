import UIKit

enum MoreOptPrefs: Int, CaseIterable {
    case requestAmount = 0
    case sweep = 1

    var name: String {
        switch self {
        case .requestAmount:
            return "id_request_amount".localized
        case .sweep:
            return "id_sweep_from_paper_wallet".localized
        }
    }

    var icon: UIImage? {
        switch self {
        default:
            return nil
        }
    }

    static func getItems(hideSweep: Bool) -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []

        MoreOptPrefs.allCases.forEach {
            if $0 == .sweep && hideSweep { } else {
                items.append(DialogListCellModel(type: .list,
                                                 icon: $0.icon,
                                                 title: $0.name))
            }
        }
        return items
    }
}
