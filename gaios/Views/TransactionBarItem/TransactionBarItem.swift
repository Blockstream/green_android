import Foundation
import UIKit

class TransactionBarItem: UIView {

    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var lblAccount: UILabel!
    var onTap: (() -> Void)?

    func configure(status: String, account: String, onTap:@escaping (() -> Void)) {
        self.lblStatus.text = status
        self.lblAccount.text = account
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
