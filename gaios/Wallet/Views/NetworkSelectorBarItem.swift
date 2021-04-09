import UIKit

class NetworkSelectorBarItem: UIView {
    @IBOutlet weak var lblNetwork: UILabel!
    @IBOutlet weak var icon: UIImageView!
    var onTap: (() -> Void)?

    var network: GdkNetwork { getGdkNetwork(getNetwork()) }

    func configure(_ onTap:@escaping (() -> Void)) {
        lblNetwork.text = network.name
        if let iconName = network.icon {
            icon.image = UIImage(named: iconName)
        }
        self.onTap = onTap
    }

    @IBAction func btn(_ sender: Any) {
        onTap?()
    }
}
