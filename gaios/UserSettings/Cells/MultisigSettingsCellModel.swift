import Foundation
import UIKit

class MultisigSettingsCellModel {

    var title: String
    var subtitle: String
    var disclosure: Bool = false
    var disclosureImage: UIImage?
    var type: MSItem?

    init(_ item: MultisigSettingsItem) {
        title = item.title
        subtitle = item.subtitle
        type = item.type

        switch type {
        case .TwoFactorAuthentication, .RecoveryTransactions:
            disclosure = true
            disclosureImage = UIImage(named: "rightArrow")?.maskWithColor(color: .white)
        default:
            disclosure = false
        }
    }
}
