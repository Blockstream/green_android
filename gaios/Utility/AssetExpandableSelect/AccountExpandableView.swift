import UIKit

class AccountExpandableView: UIView {
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var ampTip: UIView!
    @IBOutlet weak var lblAmp: UILabel!

    var reload: (() -> Void)?

    func configure(model: AssetSelectCellModel, open: Bool) {
        let name = model.asset?.name ?? model.asset?.assetId
        title.text = name
        icon?.image = model.icon

        ampTip.cornerRadius = 5.0
        ///fix here if asset is amp
        if name == "Testnet" && open {
            ampTip.isHidden = false
            lblAmp.text = "\(name ?? "") " + "is a Liquid asset. You can receive it directly on a Liquid account."
        } else {
            ampTip.isHidden = true
        }

        if open {
            bg.borderWidth = 2.0
            bg.borderColor = UIColor.gGreenMatrix()
        } else {
            bg.borderWidth = 0.0
        }
    }
}
