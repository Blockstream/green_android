import UIKit

protocol PopoverMenuHomeDelegate: AnyObject {
    func didSelectionMenuOption(menuOption: MenuWalletOption, index: String?)
}

enum MenuWalletOption {
    case passphrase
    case emergency
    case edit
    case delete
}

class PopoverMenuHomeViewController: UIViewController {

    @IBOutlet weak var menuTableView: UITableView!

    weak var delegate: PopoverMenuHomeDelegate?
    private var isLiquid: Bool!
    private var kvoContext = 0
    var menuOptions: [MenuWalletOption] = []
    var index: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        menuTableView.delegate = self
        menuTableView.dataSource = self
        menuTableView.estimatedRowHeight = 44
        menuTableView.rowHeight = UITableView.automaticDimension
    }
}

extension PopoverMenuHomeViewController: UITableViewDataSource, UITableViewDelegate {

    override var preferredContentSize: CGSize {
        get {
            return CGSize(width: super.preferredContentSize.width, height: CGFloat(Double(menuOptions.count) * 44.0) + 25.0)
        }
        set {
            super.preferredContentSize = newValue
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 44.0// UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menuOptions.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "MenuOptionCell") {
            let option = menuOptions[indexPath.row]
            switch option {
            case .emergency:
                cell.textLabel?.text = "id_show_recovery_phrase".localized
            case .passphrase:
                cell.textLabel?.text = "id_login_with_bip39_passphrase".localized
            case .edit:
                cell.textLabel?.text = "id_rename_wallet".localized
            case .delete:
                cell.textLabel?.text = "id_remove_wallet".localized
            }
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.dismiss(animated: true) {
            self.delegate?.didSelectionMenuOption(menuOption: self.menuOptions[indexPath.row], index: self.index)
        }
    }
}
