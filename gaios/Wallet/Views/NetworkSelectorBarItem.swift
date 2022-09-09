import UIKit

class NetworkSelectorBarItem: UIView {
    @IBOutlet weak var lblNetwork: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var iconPassphrase: UIImageView!

    var onTap: (() -> Void)?

    func configure(isEphemeral: Bool, _ onTap:@escaping (() -> Void)) {
        let account = AccountDao.shared.current
        lblNetwork.text = account?.name ?? ""
        if let image = account?.icon {
            icon.image = image
        }
        iconPassphrase.isHidden = !isEphemeral
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
