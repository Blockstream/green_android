import UIKit

class AccountExpandableView: UIView {
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var tapView: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var ampTip: UIView!
    @IBOutlet weak var lblAmp: UILabel!
    @IBOutlet weak var createNew: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var lblTitle: UILabel!

    var reload: (() -> Void)?
    var onCreate: (() -> Void)?

    func configure(model: AssetSelectCellModel,
                   hasAccounts: Bool,
                   open: Bool,
                   onCreate: (() -> Void)?
    ) {
        let name = model.asset?.name ?? model.asset?.assetId
        title.text = name
        icon?.image = model.icon

        ampTip.cornerRadius = 5.0
        lblAmp.text = "\(name ?? "") " + "is a Liquid asset. You can receive it directly on a Liquid account."
        ampTip.isHidden = !(model.asset?.amp ?? false && open)

        createNew.isHidden = true
        if open {
            bg.borderWidth = 2.0
            bg.borderColor = UIColor.gGreenMatrix()
            if hasAccounts {
                createNew.isHidden = false
            }
        } else {
            bg.borderWidth = 0.0
        }
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
        lblTitle.text = "Create new account"
        self.onCreate = onCreate
    }

    @IBAction func btnOnCreate(_ sender: Any) {
        onCreate?()
    }
}
