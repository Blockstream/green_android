import Foundation
import UIKit

class LTSuccessViewModel {
    var account: String
    var amount: String
    var denom: String
    var hint: String {
        String(format: "You have just funded your %@ account with %@ %@.", account, amount, denom)
    }

    init(account: String, amount: String, denom: String) {
        self.account = account
        self.amount = amount
        self.denom = denom
    }
}
