enum AccountType: String, CaseIterable, Codable, Comparable {
    /// multiSig
    case standard = "2of2"
    case amp = "2of2_no_recovery"
    case twoOfThree = "2of3"

    /// singleSig
    case legacy = "p2pkh"
    case segwitWrapped = "p2sh-p2wpkh" // former legacy
    case segWit = "p2wpkh"
    case taproot = "p2tr"

    var typeStringId: String {
        get {
            switch self {
            case .standard:
                return "id_standard"
            case .amp:
                return "id_amp"
            case .twoOfThree:
                return "id_2of3"
            case .legacy:
                return "id_legacy_bip44"
            case .segwitWrapped:
                return "id_legacy_segwit_bip49"
            case .segWit:
                return "id_segwit_bip84"
            case .taproot:
                return "id_taproot_bip86"
            }
        }
    }

    var nameStringId: String {
        get {
            switch self {
            case .standard:
                return "id_standard_account"
            case .amp:
                return "id_amp_account"
            case .twoOfThree:
                return "id_2of3_account"
            case .legacy:
                return "id_legacy_segwit_account"
            case .segwitWrapped:
                return "id_legacy_segwit_account"
            case .segWit:
                return "id_segwit_account"
            case .taproot:
                return "id_taproot_account"
            }
        }
    }

    var shortNameStringId: String {
        get {
            switch self {
            case .standard:
                return "id_standard"
            case .amp:
                return "id_amp"
            case .twoOfThree:
                return "id_2of3"
            case .legacy:
                return "Legacy"
            case .segwitWrapped:
                return "Legacy SegWit"
            case .segWit:
                return "SegWit"
            case .taproot:
                return "Taproot"
            }
        }
    }

    static func < (a: AccountType, b: AccountType) -> Bool {
        if a == .segWit {
            return true
        } else if b == .segWit {
            return false
        } else {
           return a.rawValue < b.rawValue
        }
    }
}

enum RecoveryKeyType {
    case hw
    case newPhrase(lenght: Int)
    case existingPhrase
    case publicKey
}
