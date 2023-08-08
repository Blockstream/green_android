import Foundation
import UIKit
import gdk

struct PolicyCellModel {

    var isSS: Bool { policy.accountType.singlesig }
    var isLight: Bool { policy.accountType.lightning }
    var type: String { policy.accountType.network }
    var typeDesc: String { policy.accountType.shortText }
    var name: String { policy.accountType.string }
    var hint: String
    var policy: PolicyCellType

    static func from(policy: PolicyCellType) -> PolicyCellModel {
        switch policy {
        case .LegacySegwit:
            return PolicyCellModel(hint: "Simple, portable, standard account, secured by your key, the recovery phrase.", policy: policy)
        case .Lightning:
            return PolicyCellModel(hint: "Fast transactions on the Lightning Network, powered by Greenlight.", policy: policy)
        case .TwoFAProtected:
            return PolicyCellModel(hint: "Quick setup 2FA account, ideal for active spenders (2FA expires if you don't move funds for 1 year).", policy: policy)
        case .TwoOfThreeWith2FA:
            return PolicyCellModel(hint: "Permanent 2FA account, ideal for long term hodling, optionally with 3rd emergency key on hardware wallet.", policy: policy)
        case .NativeSegwit:
            return PolicyCellModel(hint: "Cheaper singlesig option. Addresses are Native SegWit Bech32.", policy: policy)
        //case .Taproot:
        //    return PolicyCellModel(hint: "Cheaper and more private singlesig option. Addresses are Bech32m.", policy: policy)
        case .Amp:
            return PolicyCellModel(hint: "Account for special assets, monitored or authorized by the asset issuer.", policy: policy)
        }
    }
}
