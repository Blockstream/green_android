import UIKit

class NetworkSelectorBarItem: UIView {
    @IBOutlet weak var lblNetwork: UILabel!
    @IBOutlet weak var icon: UIImageView!
    var onTap: (() -> Void)?

    func configure(_ onTap:@escaping (() -> Void)) {
        let account = AccountsManager.shared.current
        lblNetwork.text = account?.name ?? ""
        if let image = account?.icon {
            icon.image = image
        }
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
