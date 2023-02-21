import Foundation
import UIKit

class UserSettingsCellModel {

    var title: String
    var subtitle: String
    var disclosure: Bool = false
    var disclosureImage: UIImage?
    var switcher: Bool?
    var type: USItem?

    init(_ item: UserSettingsItem) {
        title = item.title
        subtitle = item.subtitle
        type = item.type
        switcher = item.switcher
        switch type {
        case .Logout, .ArchievedAccounts, .ChangePin, .BackUpRecoveryPhrase, .TwoFactorAuthication, .RecoveryTransactions, .WatchOnly, .PgpKey :
            disclosure = true
            disclosureImage = UIImage(named: "ic_settings_disclose")!
        default:
            disclosure = false
        }
    }
}
