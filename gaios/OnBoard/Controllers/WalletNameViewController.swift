import UIKit
import PromiseKit

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

        fieldName.delegate = self
        fieldName.text = OnBoardManager.shared.params?.walletName ?? ""
        setContent()
        setStyle()
        updateUI()
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

        btnSettings.cornerRadius = 4.0
        btnSettings.borderWidth = 1.0
        btnSettings.borderColor = UIColor.customGrayLight()
        btnSettings.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
    }

    func updateUI() {
        if fieldName.text?.count ?? 0 > 2 {
            btnNext.setStyle(.primary)
        } else {
            btnNext.setStyle(.primaryDisabled)
        }
    }

    @IBAction func nameDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
            //
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        OnBoardManager.shared.params?.walletName = fieldName.text ?? ""
        if LandingViewController.flowType == .add {
            next()
        } else {
            // Test credential on server side
            checkCredential()
        }
    }

    func checkCredential() {
        let params = OnBoardManager.shared.params
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startLoader(message: "Setting Up Your Wallet")
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate?.disconnect()
            return try appDelegate?.connect(params?.network ?? "mainnet")
        }.then(on: bgq) {
            try getSession().login(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword ?? "").resolve()
        }.ensure {
            self.stopLoader()
        }.done { _ in
            self.next()
        }.catch { error in
            if let err = error as? GaError, err != GaError.GenericError {
                self.showError(NSLocalizedString("id_connection_failed", comment: ""))
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                self.showError(err.localizedDescription)
            } else if !error.localizedDescription.isEmpty {
                self.showError(NSLocalizedString(error.localizedDescription, comment: ""))
            } else {
                self.showError(NSLocalizedString("id_login_failed", comment: ""))
            }
        }
    }

    func next() {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController")
        self.navigationController?.pushViewController(vc, animated: true)
    }

}

extension WalletNameViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}
