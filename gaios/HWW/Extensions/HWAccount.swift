import Foundation
import UIKit

extension Account {

    func deviceImage() -> UIImage {
        if isJade {
            return UIImage(named: "ic_hww_jade")!
        } else {
            return UIImage(named: "ic_hww_ledger")!
        }
    }

    func alignConstraint() -> CGFloat {
        if self.isLedger {
            return UIScreen.main.bounds.width * 0.27
        }
        return 0.0
    }
}
