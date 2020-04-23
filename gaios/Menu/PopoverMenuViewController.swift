import UIKit

enum MenuOption {
    case watchOnly
    case hardwareWallets
    case tempRestore
    case help
}

extension MenuOption: CaseIterable {}

protocol PopoverMenuDelegate: class {
    func didSelectionMenuOption(_ menuOption: MenuOption)
}

class PopoverMenuViewController: UIViewController {

    @IBOutlet weak var menuTableView: UITableView!

    weak var delegate: PopoverMenuDelegate?
    private var isLiquid: Bool!
    private var kvoContext = 0
    private var menuOptions = MenuOption.allCases

    override func viewDidLoad() {
        super.viewDidLoad()
        isLiquid = getGdkNetwork(getNetwork()).liquid
        if isLiquid {
            _ = menuOptions.remove(at: menuOptions.firstIndex(of: .watchOnly)!)
            //_ = menuOptions.remove(at: menuOptions.firstIndex(of: .hardwareWallets)!)
        }
        menuTableView.delegate = self
        menuTableView.dataSource = self
        menuTableView.estimatedRowHeight = 44
        menuTableView.rowHeight = UITableView.automaticDimension
    }
}

extension PopoverMenuViewController: UITableViewDataSource, UITableViewDelegate {

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
            cell.textLabel?.textColor = isLiquid ?  UIColor.customMatrixGreen() : UIColor.customTitaniumLight()
            let option = menuOptions[indexPath.row]
            switch option {
            case .watchOnly:
                cell.textLabel?.text = NSLocalizedString("id_watchonly", comment: "")
            case .hardwareWallets:
                cell.textLabel?.text = NSLocalizedString("id_connect_hardware_wallet", comment: "")
            case .tempRestore:
                cell.textLabel?.text = NSLocalizedString("id_restore_temporary_wallet", comment: "")
            case .help:
                cell.textLabel?.text = NSLocalizedString("id_help", comment: "")
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
