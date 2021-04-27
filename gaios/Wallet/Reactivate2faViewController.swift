import UIKit

class Reactivate2faViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var toolBar: UIToolbar!

    @IBOutlet weak var lblWhyTitle: UILabel!
    @IBOutlet weak var lblWhyHint: UILabel!
    @IBOutlet weak var btnWhy: UIButton!
    @IBOutlet weak var lblRiskTitle: UILabel!
    @IBOutlet weak var lblRiskHint: UILabel!
    @IBOutlet weak var btnRisk: UIButton!
    @IBOutlet weak var lblReactTitle: UILabel!
    @IBOutlet weak var lblReactHint: UILabel!
    @IBOutlet weak var lblExpTitle: UILabel!
    @IBOutlet weak var lblExpHint: UILabel!
    @IBOutlet weak var btnReactivate: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        title = ""
        lblTitle.text = "2FA expired"
        lblWhyTitle.text = "Why does 2FA expires?"
        lblWhyHint.text = "To keep you in control! If you ever loose your 2FA, or Blockstream service becomes permanently unavailable, you have a guarantee that your recovery phrase alone is enough to recover your founds!"
        lblRiskTitle.text = "Are my funds at risk?"
        lblRiskHint.text = "Your funds are safe , but they are no longer protectedby 2FA."
        lblReactTitle.text = "How is 2FA re-activated"
        lblReactHint.text = "Funds with expired 2FA are re-deposited to yourself in a new transaction, and as such is subject to the usual transaction fees."
        lblExpTitle.text = "Is 2FA expiring too often?"
        lblExpHint.text = "2FA on your funds expires %d months after receiving funds.This includes any change of outgoing transactions.\n\nIf you don't regularly spend, you might consider setting a longer 2FA expiry time from settings"
        btnReactivate.setTitle("Reactivate 2FA", for: .normal)
    }

    func setStyle() {

        let flexButton = UIBarButtonItem(barButtonSystemItem: UIBarButtonItem.SystemItem.flexibleSpace, target: nil, action: nil)
        let doneButton = UIBarButtonItem(image: UIImage(named: "cancel"),
                                     style: .plain,
                                     target: self,
                                     action: #selector(self.donePressed))

        doneButton.tintColor = UIColor.customGrayLight()
        toolBar.setItems([flexButton, doneButton], animated: true)

        let attr: [NSAttributedString.Key: Any] = [
            .underlineStyle: NSUnderlineStyle.single.rawValue
        ]
        let attrWhyString = NSMutableAttributedString(
                string: "Learn about Blockstream Green multisig 2FA model here",
                attributes: attr)
        let attrRiskString = NSMutableAttributedString(
                string: "Learn what could go wrong without 2FA here",
                attributes: attr)
        btnWhy.setAttributedTitle(attrWhyString, for: .normal)
        btnRisk.setAttributedTitle(attrRiskString, for: .normal)
        btnWhy.titleLabel?.lineBreakMode = .byWordWrapping
        btnReactivate.setStyle(.primary)
    }

    func setActions() {

    }

    @objc func donePressed() {
        dismiss(animated: true, completion: nil)
    }

    @IBAction func btnWhy(_ sender: Any) {
        print("on btn why")
    }

    @IBAction func btnRisk(_ sender: Any) {
        print("on btn risk")
    }

    @IBAction func btnReactivate(_ sender: Any) {
        print("on btn reactivate")
    }
}
