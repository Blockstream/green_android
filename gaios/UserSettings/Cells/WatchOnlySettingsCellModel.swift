import Foundation
import UIKit

class WatchOnlySettingsCellModel {

    var title: String
    var subtitle: String
    var network: String?

    init(title: String, subtitle: String, network: String?) {
        self.title = title
        self.subtitle = subtitle
        self.network = network
    }
}
