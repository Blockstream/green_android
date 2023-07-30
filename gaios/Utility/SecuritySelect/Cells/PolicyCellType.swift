import Foundation
import gdk

enum PolicyCellType: String, CaseIterable {
    case NativeSegwit
    case LegacySegwit
    case Lightning
    case TwoFAProtected
    case TwoOfThreeWith2FA
    //case Taproot
    case Amp

    var accountType: AccountType {
        switch self {
        case .NativeSegwit:
            return .segWit
        case .Lightning:
            return .lightning
        case .TwoFAProtected:
            return .standard
        case .TwoOfThreeWith2FA:
            return .twoOfThree
        case .LegacySegwit:
            return .segwitWrapped
        case .Amp:
            return .amp
        }
    }

    func getNetwork(testnet: Bool, liquid: Bool) -> NetworkSecurityCase? {
        let btc: [PolicyCellType: NetworkSecurityCase] =
        [.LegacySegwit: .bitcoinSS, .Lightning: .lightning, .TwoFAProtected: .bitcoinMS,
         .TwoOfThreeWith2FA: .bitcoinMS, .NativeSegwit: .bitcoinSS, .Amp: .bitcoinMS]
        let test: [PolicyCellType: NetworkSecurityCase] =
        [.LegacySegwit: .testnetSS, .Lightning: .testnetLightning, .TwoFAProtected: .testnetMS,
         .TwoOfThreeWith2FA: .testnetMS, .NativeSegwit: .testnetSS, .Amp: .testnetMS]
        let lbtc: [PolicyCellType: NetworkSecurityCase] =
        [.LegacySegwit: .liquidSS, .TwoFAProtected: .liquidMS,
         .TwoOfThreeWith2FA: .liquidMS, .NativeSegwit: .liquidSS, .Amp: .liquidMS]
        let ltest: [PolicyCellType: NetworkSecurityCase] =
        [.LegacySegwit: .testnetLiquidSS, .TwoFAProtected: .testnetLiquidMS,
         .TwoOfThreeWith2FA: .testnetLiquidMS, .NativeSegwit: .testnetLiquidSS, .Amp: .testnetLiquidMS]
        if liquid && testnet { return ltest[self] }
        if liquid && !testnet { return lbtc[self] }
        if !liquid && testnet { return test[self] }
        return btc[self]
    }
}
