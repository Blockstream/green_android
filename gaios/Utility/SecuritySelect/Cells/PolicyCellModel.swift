import Foundation
import UIKit

struct PolicyCellModel {

    var isSS: Bool
    var isLight: Bool
    var type: String
    var name: String
    var hint: String
    var policy: PolicyCellType
    var accountType: AccountType

    static func from(policy: PolicyCellType) -> PolicyCellModel {
        switch policy {
        case .Standard:
            return PolicyCellModel(isSS: true, isLight: false, type: "SINGLESIG / STANDARD", name: "Standard".uppercased(), hint: "Simple, portable, standard account, secured by your key, the recovery phrase.", policy: policy, accountType: .segwitWrapped)
        //case .Instant:
        //    return PolicyCellModel(isSS: false, isLight: true, type: "Lightning", name: "Instant", hint: "Fast transactions on the Lightning Network, powered by Greenlight.", policy: policy)
        case .TwoFAProtected:
            return PolicyCellModel(isSS: false, isLight: false, type: "MULTISIG / 2FA Protected".uppercased(), name: "2FA Protected", hint: "Quick setup 2FA account, ideal for active spenders (2FA expires if you don't move funds every 6 months).", policy: policy, accountType: .standard)
        case .TwoOfThreeWith2FA:
            return PolicyCellModel(isSS: false, isLight: false, type: "MULTISIG / 2OF3 WITH 2FA PROTECTED".uppercased(), name: "2of3 with 2FA", hint: "Permanent 2FA account, ideal for long term hodling, optionally with 3rd emergency key on hardware wallet.", policy: policy, accountType: .twoOfThree)
        case .NativeSegwit:
            return PolicyCellModel(isSS: true, isLight: false, type: "SINGLESIG / NATIVE SEGWIT".uppercased(), name: "Native Segwit", hint: "Cheaper singlesig option. Addresses are Native SegWit Bech32.", policy: policy, accountType: .segWit)
        //case .Taproot:
        //    return PolicyCellModel(isSS: true, isLight: false, type: "SINGLESIG / TAPROOT", name: "Taproot", hint: "Cheaper and more private singlesig option. Addresses are Bech32m.", policy: policy)
        case .Amp:
            return PolicyCellModel(isSS: false, isLight: false, type: "MULTISIG / AMP".uppercased(), name: "Amp", hint: "Account for special assets, monitored or authorized by the asset issuer.", policy: policy, accountType: .amp)
        }
    }
}
