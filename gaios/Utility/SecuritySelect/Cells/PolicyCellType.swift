import Foundation
import gdk

enum PolicyCellType: String, CaseIterable {
    case Standard
    case Lightning
    case TwoFAProtected
    case TwoOfThreeWith2FA
    case NativeSegwit
    //case Taproot
    case Amp

    var accountType: AccountType {
        switch self {
        case .Standard:
             // singlesig legacy segwit
            return .segwitWrapped
        case .Lightning:
            return .lightning
        case .TwoFAProtected:
            return .standard
        case .TwoOfThreeWith2FA:
            return .twoOfThree
        case .NativeSegwit:
            return .segWit
        case .Amp:
            return .amp
        }
    }

    func getNetwork(testnet: Bool, liquid: Bool) -> NetworkSecurityCase? {
        let btc: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .bitcoinSS, .Lightning: .lightning, .TwoFAProtected: .bitcoinMS,
         .TwoOfThreeWith2FA: .bitcoinMS, .NativeSegwit: .bitcoinSS, .Amp: .bitcoinMS]
        let test: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .testnetSS, .Lightning: .testnetLightning, .TwoFAProtected: .testnetMS,
         .TwoOfThreeWith2FA: .testnetMS, .NativeSegwit: .testnetSS, .Amp: .testnetMS]
        let lbtc: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .liquidSS, .TwoFAProtected: .liquidMS,
         .TwoOfThreeWith2FA: .liquidMS, .NativeSegwit: .liquidSS, .Amp: .liquidMS]
        let ltest: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .testnetLiquidSS, .TwoFAProtected: .testnetLiquidMS,
         .TwoOfThreeWith2FA: .testnetLiquidMS, .NativeSegwit: .testnetLiquidSS, .Amp: .testnetLiquidMS]
        if liquid && testnet { return ltest[self] }
        if liquid && !testnet { return lbtc[self] }
        if !liquid && testnet { return test[self] }
        return btc[self]
    }
}
