import UIKit

class WalletNameViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var fieldName: UITextField!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblSubtitle: UILabel!
    @IBOutlet weak var lblSubtitleHint: UILabel!
    @IBOutlet weak var btnSettings: UIButton!
    @IBOutlet weak var btnNext: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        hideKeyboardWhenTappedAround()
    }

    func setContent() {
        lblTitle.text = "Wallet Name"
        lblHint.text = "Choose a name for your wallet"
        lblSubtitle.text = "Connection & Validation Settings"
        lblSubtitleHint.text = "You can change these later on."
    }

    func setStyle() {
        fieldName.setLeftPaddingPoints(10.0)
        fieldName.setRightPaddingPoints(10.0)
        btnNext.cornerRadius = 4.0
        btnSettings.cornerRadius = 4.0
        btnSettings.borderWidth = 1.0
        btnSettings.borderColor = UIColor.customGrayLight()
        btnSettings.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
            //
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController")
        self.navigationController?.pushViewController(vc, animated: true)
    }

}
