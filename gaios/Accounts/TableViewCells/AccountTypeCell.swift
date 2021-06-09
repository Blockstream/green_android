import UIKit

protocol AccountTypeInfoDelegate: AnyObject {
    func didTapInfo(for accountInfoType: AccountInfoType)
    func didChange(_ name: String)
}

class AccountTypeCell: UITableViewCell {

    @IBOutlet weak var subtextLabel: UILabel!
    @IBOutlet weak var headlineLabel: UILabel!
    @IBOutlet weak var infoButton: UIButton!
    @IBOutlet weak var nameTextField: UITextField!

    weak var delegate: AccountTypeInfoDelegate?
    var accountType: AccountType!
    var accountInfoType: AccountInfoType!

    override func prepareForReuse() {
        subtextLabel.text = ""
        headlineLabel.text = ""
        accessoryType = .none
    }

    override func selectable(_ selectable: Bool) {
        super.selectable(selectable)
        isUserInteractionEnabled = true
    }

    func configure(for accountType: AccountType, indexPath: IndexPath, delegate: AccountTypeInfoDelegate) {
        self.delegate = delegate
        self.accountType = accountType
        infoButton.addTarget(self, action: #selector(infoTapped), for: .touchUpInside)
        selectionStyle = .none
        accessoryType = .none
        nameTextField.isHidden = true
        isUserInteractionEnabled = true
//        switch accountType {
//        case .simple:
//            accountInfoType = .simple
//            subtextLabel.text = NSLocalizedString("id_for_most_users", comment: "")
//            headlineLabel.text = NSLocalizedString("id_standard_account", comment: "")
//        case .advanced:
//            accountInfoType = .advanced
//            subtextLabel.text = NSLocalizedString("id_for_investors", comment: "")
//            headlineLabel.text = NSLocalizedString("id_amp_account", comment: "")
//        }
    }

    @objc func infoTapped() {
        if let delegate = self.delegate {
            delegate.didTapInfo(for: accountInfoType)
        }
    }

    @IBAction func nameTextChanged(_ sender: UITextField) {
        if let text = sender.text, let delegate = delegate {
            delegate.didChange(text)
        }
    }
}
