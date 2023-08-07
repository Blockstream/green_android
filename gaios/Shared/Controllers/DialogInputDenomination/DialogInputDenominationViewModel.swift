import Foundation
import gdk

class DialogInputDenominationViewModel {

    var denomination: DenominationType
    var denominations: [DenominationType]
    var network: NetworkSecurityCase
    var isFiat = false
    
    init(denomination: DenominationType,
         denominations: [DenominationType],
         network: NetworkSecurityCase,
         isFiat: Bool) {
        self.denomination = denomination
        self.denominations = denominations
        self.network = network
        self.isFiat = isFiat
    }

    func symbol(_ denom: DenominationType) -> String {
        return denom.string(for: network.gdkNetwork)
    }
}
