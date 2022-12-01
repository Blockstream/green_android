import UIKit

class AccountCreateFooterView: UIView {
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var createNew: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var lblTitle: UILabel!

    var onTap: (() -> Void)?

    func configure(onTap: (() -> Void)?
    ) {
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
        lblTitle.text = "Create new account"
        self.onTap = onTap
    }

    @IBAction func btnOnCreate(_ sender: Any) {
        onTap?()
    }
}
