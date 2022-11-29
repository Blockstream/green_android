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
    var sender: Int

    init(title: String, items: [DialogCellModel], sender: Int) {
        self.title = title
        self.items = items
        self.sender = sender
    }
}
