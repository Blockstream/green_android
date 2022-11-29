import Foundation
import UIKit

enum DialogType: CaseIterable {
    case walletPrefs
    case phrasePrefs
    case accountPrefs
    case networkPrefs
    case enable2faPrefs
    case watchOnlyPrefs
}

enum DialogCellType: CaseIterable {
    case list
    case enable2fa
}

protocol DialogCellModel: AnyObject {
    var type: DialogCellType { get }
}

class DialogListViewModel {

    var title: String
    var items: [DialogCellModel] = []
    var type: DialogType

    init(title: String, type: DialogType, items: [DialogCellModel] ) {
        self.title = title
        self.items = items
        self.type = type
    }
}
