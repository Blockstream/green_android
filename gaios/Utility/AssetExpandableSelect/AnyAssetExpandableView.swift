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
        title.text = "id_receive_any_liquid_asset".localized

        lblAccountTip.text = "id_you_need_a_liquid_account_in".localized
        accountTip.isHidden = !open

        createNew.isHidden = true
        if open {
            bg.borderWidth = 2.0
            bg.borderColor = UIColor.gGreenMatrix()
            bg.layer.cornerRadius = 5
            bg.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
            // always closed, added action in section footer
            createNew.isHidden = true // hasAccounts
            accountTip.isHidden = hasAccounts
        } else {
            bg.borderWidth = 0.0
        }
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
        lblTitle.text = "id_create_new_account".localized
        self.onCreate = onCreate
    }

    @IBAction func btnOnCreate(_ sender: Any) {
        onCreate?()
    }
}
