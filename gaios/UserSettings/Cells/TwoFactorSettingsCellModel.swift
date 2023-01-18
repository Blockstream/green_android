import Foundation
import UIKit

class TwoFactorSettingsCellModel {

    let item: TwoFactorItem
    let onActionSwitch: (() -> Void)?

    init(item: TwoFactorItem, onActionSwitch: (() -> Void)? = nil) {
        self.item = item
        self.onActionSwitch = onActionSwitch
    }
}
