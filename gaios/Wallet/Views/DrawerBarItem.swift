import UIKit

class DrawerBarItem: UIView {
    @IBOutlet weak var lblWallet: UILabel!

    var onTap: (() -> Void)?

    func configure(_ onTap:@escaping (() -> Void)) {
        lblWallet.text = "Work Wallet"
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
