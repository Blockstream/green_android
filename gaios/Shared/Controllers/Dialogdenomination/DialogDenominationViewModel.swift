import Foundation
import gdk

class DialogDenominationViewModel {

    var denomination: DenominationType
    var denominations: [DenominationType]
    var network: NetworkSecurityCase

    init(denomination: DenominationType,
         denominations: [DenominationType],
         network: NetworkSecurityCase) {
        self.denomination = denomination
        self.denominations = denominations
        self.network = network
    }

    func symbol(_ denom: DenominationType) -> String {
        return denom.string(for: network.gdkNetwork)
    }
}
