public enum AccountType: String, CaseIterable, Codable, Comparable, Equatable {
    /// multiSig
    case standard = "2of2"
    case amp = "2of2_no_recovery"
    case twoOfThree = "2of3"

    /// singleSig
    case legacy = "p2pkh"
    case segwitWrapped = "p2sh-p2wpkh" // former legacy
    case segWit = "p2wpkh"
    case taproot = "p2tr"
    case lightning = "lightning"
    
    public var multisig: Bool {
        [AccountType.standard, AccountType.amp, AccountType.twoOfThree].contains(self)
    }

    public var singlesig: Bool {
        [.legacy, AccountType.segwitWrapped, AccountType.segWit, AccountType.taproot].contains(self)
    }

    public var lightning: Bool {
        AccountType.lightning == self
    }

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
                return "Legacy SegWit"
            case .segWit:
                return "Standard"
            case .taproot:
                return "Taproot"
            case .lightning:
                return "Lightning"
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
            case .lightning:
                return "Lightning"
            }
        }
    }

    public static func < (a: AccountType, b: AccountType) -> Bool {
        let rules: [AccountType] = [.segwitWrapped, .segWit, .taproot, .standard, .amp, .twoOfThree, .lightning]
        return rules.firstIndex(of: a) ?? 0 < rules.firstIndex(of: b) ?? 0
    }
}

public enum RecoveryKeyType {
    case hw
    case newPhrase
    case existingPhrase
    case publicKey
}
