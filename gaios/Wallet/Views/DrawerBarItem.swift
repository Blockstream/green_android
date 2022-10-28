import UIKit

class DrawerBarItem: UIView {
    @IBOutlet weak var lblWallet: UILabel!

    var onTap: (() -> Void)?

    func configure(_ onTap:@escaping (() -> Void)) {
        let account = AccountsManager.shared.current
        lblWallet.text = account?.name ?? ""
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
