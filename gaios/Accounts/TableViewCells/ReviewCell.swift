import UIKit

final class ReviewCell: AccountTypeCell {

    override func configure(for accountType: AccountType, indexPath: IndexPath, delegate: AccountTypeInfoDelegate) {
        self.delegate = delegate
        self.accountType = accountType
        infoButton.addTarget(self, action: #selector(infoTapped), for: .touchUpInside)
        selectionStyle = .none
        accessoryType = .none
        nameTextField.isHidden = true
        isUserInteractionEnabled = true
        separatorInset = UIEdgeInsets.zero
        let isSimple = accountType == .simple
        switch indexPath.row {
        case 0:
            subtextLabel.text = NSLocalizedString("id_account_name", comment: "")
            nameTextField.text = isSimple ? "" : NSLocalizedString("id_liquid_securities_account", comment: "")
            nameTextField.isEnabled = isSimple
            nameTextField.isHidden = false
            nameTextField.delegate = self
            nameTextField.becomeFirstResponder()
            headlineLabel.isHidden = true
            infoButton.isHidden = true
            nameTextChanged(nameTextField)
        case 1:
            accountInfoType = isSimple ? .simple : .advanced
            subtextLabel.text = NSLocalizedString("id_account_type", comment: "")
            headlineLabel.text = isSimple ?
                NSLocalizedString("id_standard_account", comment: "") :
                NSLocalizedString("id_liquid_securities_account", comment: "")
            infoButton.isHidden = false
        case 2:
            guard !isSimple else {
                for view in [subtextLabel, headlineLabel, infoButton] {
                    view?.isHidden = true
                }
                break
            }
            accountInfoType = .accountID
            infoButton.isHidden = isSimple
            subtextLabel.text = NSLocalizedString("id_account_id", comment: "")
            headlineLabel.text = NSLocalizedString("id_go_to_receive_to_get_your", comment: "")
            infoButton.isHidden = false
        default:
            break
        }
    }
}

extension ReviewCell: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.endEditing(true)
        return false
    }
}
