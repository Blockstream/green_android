import UIKit

class DialogEnable2faCellModel: DialogCellModel {

    var type: DialogCellType
    let title: String
    let hint: String
    let actionTitle: String

    init(type: DialogCellType) {
        self.type = type
        self.title = "2FA isn’t set up yet."
        self.hint = "You can choose your favourite 2FA method among an authenticator app, email, SMS or a call."
        self.actionTitle = "Setup 2FA Now"
    }

}
