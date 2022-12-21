import UIKit

class DrawerBarItem: UIView {
    @IBOutlet weak var lblWallet: UILabel!
    @IBOutlet weak var icon: UIImageView!
    
    var onTap: (() -> Void)?

    func configure(img: UIImage, onTap:@escaping (() -> Void)) {
        let account = AccountsManager.shared.current
        lblWallet.text = account?.name ?? ""
        self.icon.image = img
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
