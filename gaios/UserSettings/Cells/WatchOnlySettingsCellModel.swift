import Foundation
import UIKit

class WatchOnlySettingsCellModel {

    var title: String
    var subtitle: String
    var network: String?
    var isExtended: Bool

    init(title: String, subtitle: String, network: String?, isExtended: Bool = false) {
        self.title = title
        self.subtitle = subtitle
        self.network = network
        self.isExtended = isExtended
    }
}
