public enum AccountType: String, CaseIterable, Codable, Comparable {
    /// multiSig
    case standard = "2of2"
    case amp = "2of2_no_recovery"
    case twoOfThree = "2of3"

    /// singleSig
    case legacy = "p2pkh"
    case segwitWrapped = "p2sh-p2wpkh" // former legacy
    case segWit = "p2wpkh"
    case taproot = "p2tr"

    public var string: String {
        get {
            switch self {
            case .standard:
                return "2FA Protected"
            case .amp:
                return "AMP"
            case .twoOfThree:
                return "2of3 with 2FA"
            case .legacy:
                return "Legacy"
            case .segwitWrapped:
                return "Standard"
            case .segWit:
                return "SegWit"
            case .taproot:
                return "Taproot"
            }
        }
    }

    public var shortString: String {
        get {
            switch self {
            case .standard:
                return "2of2"
            case .amp:
                return "Amp"
            case .twoOfThree:
                return "2of3"
            case .legacy:
                return "Legacy"
            case .segwitWrapped:
                return "Legacy SegWit"
            case .segWit:
                return "Native SegWit"
            case .taproot:
                return "Taproot"
            }
        }
    }

    public static func < (a: AccountType, b: AccountType) -> Bool {
        if a == .legacy || a == .standard {
            return true
        } else if b == .legacy || b == .standard {
            return false
        } else {
           return a.rawValue < b.rawValue
        }
    }
}

public enum RecoveryKeyType {
    case hw
    case newPhrase
    case existingPhrase
    case publicKey
}
