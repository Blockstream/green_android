import UIKit

enum AccountType: String, CaseIterable {
    case standard = "2of2"
    case amp = "2of2_no_recovery"
    case threeSig = "2of3"
    case legacy = "p2sh-p2wpkh"
    case segWit = "p2wpkh"

    var name: String {
        get {
            switch self {
            case .standard:
                return NSLocalizedString("id_standard_account", comment: "")
            case .amp:
                return NSLocalizedString("id_amp_account", comment: "")
            case .threeSig:
                return NSLocalizedString("id_2of3_account", comment: "")
            case .legacy:
                return "Legacy"
            case .segWit:
                return "SegWit"
            }
        }
    }
}

enum RecoveryKeyType {
    case hw
    case newPhrase(lenght: Int)
    case existingPhrase
    case publicKey
}
