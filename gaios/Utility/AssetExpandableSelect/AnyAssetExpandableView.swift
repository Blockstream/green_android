import UIKit

class AnyAssetExpandableView: UIView {
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var tapView: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var accountTip: UIView!
    @IBOutlet weak var lblAccountTip: UILabel!
    @IBOutlet weak var createNew: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var lblTitle: UILabel!

    var reload: (() -> Void)?
    var onCreate: (() -> Void)?

    func configure(open: Bool,
                   hasAccounts: Bool,
                   onCreate: (() -> Void)?
    ) {
        title.text = "Receive any Liquid Asset"

        lblAccountTip.text = "You need a liquid account in order to receive it."
        accountTip.isHidden = !open

        createNew.isHidden = true
        if open {
            bg.borderWidth = 2.0
            bg.borderColor = UIColor.gGreenMatrix()
            createNew.isHidden = hasAccounts
            accountTip.isHidden = hasAccounts
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
