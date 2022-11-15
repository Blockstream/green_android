import Foundation
import UIKit

class DialogListViewModel {

    var title: String
    var items: [DialogListCellModel] = []

    init(title: String, items: [DialogListCellModel]) {
        self.title = title
        self.items = items
    }
}
