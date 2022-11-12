import UIKit
import PromiseKit

enum AccountArchiveSection: Int, CaseIterable {
    case account = 0
}

class AccountArchiveViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    var viewModel = AccountArchiveViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()

        let account = AccountsManager.shared.current
        navigationItem.title = account?.name ?? ""
        navigationItem.setHidesBackButton(true, animated: false)

        let ntwBtn = UIButton(type: .system)
        ntwBtn.imageView?.contentMode = .scaleAspectFit
        ntwBtn.addTarget(self, action: #selector(AccountArchiveViewController.back), for: .touchUpInside)
        ntwBtn.contentEdgeInsets = UIEdgeInsets(top: 5, left: 10, bottom: 5, right: 10)
        navigationItem.leftBarButtonItems =
            [UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(LoginViewController.back)),
             UIBarButtonItem(customView: ntwBtn)
            ]

        setContent()

        AnalyticsManager.shared.recordView(.archivedAccounts, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
    }

    func reloadSections(_ sections: [AccountArchiveSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
    }

    func setContent() {
        let reloadSections: (([AccountArchiveSection], Bool) -> Void)? = { [weak self] (sections, animated) in
            self?.reloadSections(sections, animated: true)
        }
        viewModel.reloadSections = reloadSections
    }

    func getColor(_ account: WalletItem) -> UIColor {
        if account.network == AvailableNetworks.bitcoin.rawValue { return AvailableNetworks.bitcoin.color() }
        if account.network == AvailableNetworks.liquid.rawValue { return AvailableNetworks.liquid.color() }
        if account.network == AvailableNetworks.testnet.rawValue { return AvailableNetworks.testnet.color() }
        return AvailableNetworks.testnetLiquid.color()
    }

    func presentUnarchiveMenu(frame: CGRect, index: Int) {
        let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
        if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuUnarchiveViewController") as? PopoverMenuUnarchiveViewController {
            popover.delegate = self
            popover.index = index
            popover.modalPresentationStyle = .popover
            let popoverPresentationController = popover.popoverPresentationController
            popoverPresentationController?.backgroundColor = UIColor.customModalDark()
            popoverPresentationController?.delegate = self
            popoverPresentationController?.sourceView = self.tableView
            popoverPresentationController?.sourceRect = CGRect(x: self.tableView.frame.width - 80.0, y: frame.origin.y, width: 60.0, height: 60.0)
            popoverPresentationController?.permittedArrowDirections = .up
            self.present(popover, animated: true)
        }
    }
}

extension AccountArchiveViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return AccountArchiveSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case AccountArchiveSection.account.rawValue:
            return viewModel.subaccounts.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case AccountArchiveSection.account.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AccountArchiveCell") as? AccountArchiveCell {
                var action: VoidToVoid?
                    action = { [weak self] in
                        self?.presentUnarchiveMenu(frame: cell.frame, index: indexPath.row)
                    }
                let account = viewModel.subaccounts[indexPath.row]
                cell.configure(account: account, action: action, color: getColor(account), isLiquid: account.gdkNetwork.liquid)
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 1.0
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1.0
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) { }
}

extension AccountArchiveViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension AccountArchiveViewController: PopoverMenuUnarchiveDelegate {
    func didSelectionMenuOption(option: MenuUnarchiveOption, index: Int) {
        switch option {
        case .unarchive:
            let subaccount = viewModel.subaccounts[index]
            viewModel.unarchiveSubaccount(subaccount)
        }
    }
}
