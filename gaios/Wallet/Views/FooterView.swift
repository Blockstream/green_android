import Foundation
import UIKit

class FooterView: UIView {

    @IBOutlet weak var lblNoTransactions: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()

        lblNoTransactions.font = UIFont.systemFont(ofSize: 16, weight: .regular)
        lblNoTransactions.textColor = UIColor.customTitaniumLight()
        lblNoTransactions.numberOfLines = 0
        lblNoTransactions.textAlignment = .center
        lblNoTransactions.text = NSLocalizedString("id_your_transactions_will_be_shown", comment: "")
    }

}
