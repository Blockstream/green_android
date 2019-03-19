import Foundation
import UIKit

class TOSViewController: UIViewController {

    @IBOutlet var content: TOSView!

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.reload()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.nextButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
    }

    @objc func click(_ sender: UIButton) {
        self.performSegue(withIdentifier: "next", sender: self)
    }
}

@IBDesignable
class TOSView: UIView {
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var subtitleLabel: UILabel!
    @IBOutlet weak var tosTextView: UITextView!
    @IBOutlet weak var tosButton: DesignableButton!
    @IBOutlet weak var nextButton: UIButton!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    @IBAction func agreeTOSClicked(_ sender: UIButton) {
        nextButton.isEnabled = !nextButton.isEnabled
        updateButtons()
    }

    func reload() {
        let whiteString = NSLocalizedString("id_i_agree_to_the", comment: "")
        let linkString = NSLocalizedString("id_terms_of_service", comment: "")
        let tosString = NSMutableAttributedString(string: whiteString + " " + linkString)
        tosString.addAttribute(.link, value: "https://greenaddress.it/tos", range: NSRange(location: whiteString.count + 1, length: linkString.count))
        tosString.setColor(color: UIColor.white, forText: whiteString)
        let linkAttributes: [String: Any] = [
            NSAttributedStringKey.foregroundColor.rawValue: UIColor.customMatrixGreen(),
            NSAttributedStringKey.underlineColor.rawValue: UIColor.customMatrixGreen(),
            NSAttributedStringKey.underlineStyle.rawValue: NSUnderlineStyle.styleSingle.rawValue
        ]
        tosTextView.linkTextAttributes = linkAttributes
        tosTextView.attributedText = tosString
        tosTextView.font = UIFont.systemFont(ofSize: 16)
        tosTextView.isUserInteractionEnabled = true
        let stringLocalized = NSLocalizedString("id_welcome_to", comment: "") + " GREEN"
        let topString = NSMutableAttributedString(string: stringLocalized)
        topString.setColor(color: UIColor.customMatrixGreen(), forText: "GREEN")
        titleLabel.attributedText = topString
        subtitleLabel.text = NSLocalizedString("id_lets_get_you_set_up", comment: "")
        nextButton.setTitle(NSLocalizedString("id_next", comment: ""), for: .normal)
        nextButton.isEnabled = false
        updateButtons()
    }

    func updateButtons() {
        let accept = nextButton.isEnabled
        tosButton.backgroundColor = accept ? UIColor.customMatrixGreen() : UIColor.clear
        tosButton.layer.borderColor =  UIColor.customTitaniumLight().cgColor
        tosButton.setImage(accept ? UIImage(named: "check") : nil, for: UIControlState.normal)
        tosButton.tintColor = UIColor.white
        nextButton.setGradient(accept)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        nextButton.updateGradientLayerFrame()
    }
}
