import UIKit

class AccountCreateFooterView: UIView {
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var createNew: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var topMask: UIView!

    var onTap: (() -> Void)?

    func configure(hasAccounts: Bool, onTap: (() -> Void)?
    ) {
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
        lblTitle.text = "id_create_new_account".localized
        self.onTap = onTap

        createNew.borderWidth = 2.0
        createNew.borderColor = UIColor.gGreenMatrix()
        createNew.layer.cornerRadius = 5
        createNew.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        topMask.isHidden = false //hasAccounts
    }

    @IBAction func btnOnCreate(_ sender: Any) {
        onTap?()
    }
}
