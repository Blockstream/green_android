import UIKit

enum PhrasePrefs: Int, CaseIterable {
    case _12 = 0
    case _24 = 1

    var name: String {
        switch self {
        case ._12:
            return String(format: NSLocalizedString("id_d_words", comment: ""), 12)
        case ._24:
            return String(format: NSLocalizedString("id_d_words", comment: ""), 24)
        }
    }

    var icon: UIImage? {
        switch self {
        case ._12:
            return nil
        case ._24:
            return nil
        }
    }

    static func getItems() -> [DialogListCellModel] {
        var items: [DialogListCellModel] = []
        PhrasePrefs.allCases.forEach {
            items.append(DialogListCellModel(type: .list,
                                             icon: $0.icon,
                                             title: $0.name))
        }
        return items
    }
}
