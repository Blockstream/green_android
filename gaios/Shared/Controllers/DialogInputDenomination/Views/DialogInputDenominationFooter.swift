import UIKit

class DialogInputDenominationFooter: UIView {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var icon: UIImageView!

    var onTap: (() -> Void)?

    func configure(title: String,
                   hint: String,
                   isSelected: Bool,
                   onTap: (() -> Void)?
    ) {
        lblTitle.setStyle(.titleCard)
        lblHint.setStyle(.txtCard)
        lblTitle.text = title
        icon.isHidden = isSelected == false
        lblTitle.textColor = isSelected ? UIColor.gGreenMatrix() : .white
        lblHint.text = hint
        self.onTap = onTap
    }

    @IBAction func btnFiat(_ sender: Any) {
        onTap?()
    }
}
