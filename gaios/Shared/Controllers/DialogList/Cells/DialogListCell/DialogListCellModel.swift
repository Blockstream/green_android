import UIKit

class DialogListCellModel: DialogCellModel {

    var type: DialogCellType
    var icon: UIImage?
    let title: String

    init(type: DialogCellType, icon: UIImage?, title: String) {
        self.type = type
        self.icon = icon
        self.title = title
    }

}
