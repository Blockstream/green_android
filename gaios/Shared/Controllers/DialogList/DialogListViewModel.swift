import Foundation
import UIKit

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

    init(title: String, items: [DialogCellModel]) {
        self.title = title
        self.items = items
    }
}
