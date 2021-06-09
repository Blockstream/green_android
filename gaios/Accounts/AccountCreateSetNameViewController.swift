import UIKit
import PromiseKit

class AccountCreateSetNameViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var fieldName: UITextField!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var lblAccountTypeTitle: UILabel!
    @IBOutlet weak var lblAccountTypeHint: UILabel!

    @IBOutlet weak var btnNext: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        fieldName.delegate = self
        setContent()
        setStyle()
        updateUI()
        hideKeyboardWhenTappedAround()
    }

    func setContent() {
        lblTitle.text = "Rivedi le informazioni account"
        lblHint.text = "Nome account"
        lblAccountTypeTitle.text = "TIPO DI ACCOUNT"
        lblAccountTypeHint.text = "Legacy Account"
        btnNext.setTitle("Aggiungi Nuovo Account", for: .normal)
    }

    func setStyle() {
        fieldName.setLeftPaddingPoints(10.0)
        fieldName.setRightPaddingPoints(10.0)
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

    @IBAction func btnNext(_ sender: Any) {
        next()
    }

    func next() {

        print("create account ...")
    }

}

extension AccountCreateSetNameViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}
