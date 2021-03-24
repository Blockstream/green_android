import UIKit

enum MenuWalletOption {
    case edit
    case delete
}

extension MenuWalletOption: CaseIterable {}

protocol PopoverMenuWalletDelegate: class {
    func didSelectionMenuOption(_ menuOption: MenuWalletOption)
}

class PopoverMenuWalletViewController: UIViewController {

    @IBOutlet weak var menuTableView: UITableView!

    weak var delegate: PopoverMenuWalletDelegate?
    private var isLiquid: Bool!
    private var kvoContext = 0
    private var menuOptions = MenuWalletOption.allCases

    override func viewDidLoad() {
        super.viewDidLoad()
        menuTableView.delegate = self
        menuTableView.dataSource = self
        menuTableView.estimatedRowHeight = 44
        menuTableView.rowHeight = UITableView.automaticDimension
    }
}

extension PopoverMenuWalletViewController: UITableViewDataSource, UITableViewDelegate {

    override var preferredContentSize: CGSize {
        get {
            return CGSize(width: super.preferredContentSize.width, height: menuTableView.rect(forSection: 0).height)
        }
        set {
            super.preferredContentSize = newValue
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menuOptions.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "MenuOptionCell") {
            let option = menuOptions[indexPath.row]
            switch option {
            case .edit:
                cell.textLabel?.text = "Rename Wallet"
            case .delete:
                cell.textLabel?.text = "Remove Wallet"
            }
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.dismiss(animated: true) {
            self.delegate?.didSelectionMenuOption(self.menuOptions[indexPath.row])
        }
    }
}
