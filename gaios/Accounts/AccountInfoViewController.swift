import UIKit

enum AccountInfoType: CaseIterable {
    case simple, advanced, accountID
}

class AccountInfoViewController: UIViewController {

    var accountInfoType: AccountInfoType!

    @IBOutlet weak var headlineLabel: UILabel!
    @IBOutlet weak var detailLabel: UILabel!

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if let accountInfoType = accountInfoType {
            switch accountInfoType {
            case .simple:
                headlineLabel.text = NSLocalizedString("id_standard_account", comment: "")
                detailLabel.text = NSLocalizedString("id_standard_accounts_allow_you_to", comment: "")
            case .advanced:
                headlineLabel.text = NSLocalizedString("id_liquid_securities_account", comment: "")
                detailLabel.text = NSLocalizedString("id_liquid_securities_accounts_are", comment: "") + "\n\n" + NSLocalizedString("id_twofactor_protection_does_not", comment: "")
            case .accountID:
                headlineLabel.text = NSLocalizedString("id_account_id", comment: "")
                detailLabel.text = NSLocalizedString("id_provide_this_id_to_the_issuer", comment: "")
            }
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        let downSwipe = UISwipeGestureRecognizer(target: self, action: #selector(dismissModal))
        downSwipe.direction = .down
        view.addGestureRecognizer(downSwipe)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        view.roundCorners([.topRight, .topLeft], radius: 12)
    }

    @objc func dismissModal() {
        dismiss(animated: true, completion: nil)
    }
}
