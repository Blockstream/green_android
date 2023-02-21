import UIKit

class DialogEnable2faCellModel: DialogCellModel {

    var type: DialogCellType
    let title: String
    let hint: String
    let actionTitle: String

    init(type: DialogCellType) {
        self.type = type
        self.title = ""
        self.hint = "id_2fa_isnt_set_up_yetnnyou_can".localized
        self.actionTitle = "id_setup_2fa_now".localized
    }

}
