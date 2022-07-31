import UIKit
import PromiseKit

enum AccountArchiveSection: Int, CaseIterable {
    case account = 0
}

class AccountArchiveViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    private var subAccounts = [WalletItem]()

    var isLoading = false
    var accounts: [WalletItem] {
        get {
            if subAccounts.count == 0 {
                return []
            }
            let activeWallet = account?.activeWallet ?? 0
            return subAccounts.filter { $0.pointer == activeWallet} + subAccounts.filter { $0.pointer != activeWallet}
        }
    }
    var account = AccountsManager.shared.current
    private var isLiquid: Bool { account?.gdkNetwork?.liquid ?? false }
//    private var isAmp: Bool {
//        guard let wallet = presentingWallet else { return false }
//        return AccountType(rawValue: wallet.type) == AccountType.amp
//    }

    var color: UIColor = .clear

    override func viewDidLoad() {
        super.viewDidLoad()

        navigationItem.title = account?.name ?? ""
        navigationItem.setHidesBackButton(true, animated: false)

        let ntwBtn = UIButton(type: .system)
        let img = account?.icon ?? UIImage()
        ntwBtn.setImage(img.withRenderingMode(.alwaysOriginal), for: .normal)
        ntwBtn.imageView?.contentMode = .scaleAspectFit
        ntwBtn.addTarget(self, action: #selector(AccountArchiveViewController.back), for: .touchUpInside)
        ntwBtn.contentEdgeInsets = UIEdgeInsets(top: 5, left: 10, bottom: 5, right: 10)
        navigationItem.leftBarButtonItems =
            [UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(LoginViewController.back)),
             UIBarButtonItem(customView: ntwBtn)
            ]

        setContent()
        setStyle()

        reloadData()

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
    }

    func setStyle() {
        if account?.network == AvailableNetworks.bitcoin.rawValue { color = AvailableNetworks.bitcoin.color() }
        if account?.network == AvailableNetworks.liquid.rawValue { color = AvailableNetworks.liquid.color() }
        if account?.network == AvailableNetworks.testnet.rawValue { color = AvailableNetworks.testnet.color() }
        if account?.network == AvailableNetworks.testnetLiquid.rawValue { color = AvailableNetworks.testnetLiquid.color() }
    }

    func reloadData() {
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = SessionsManager.current else { return }
        Guarantee().then(on: bgq) {
                session.subaccounts()
            }.then(on: bgq) { wallets -> Promise<[WalletItem]> in
                let balances = wallets.map { wallet in { wallet.getBalance() } }
                return Promise.chain(balances).compactMap { _ in wallets }
            }.map { wallets in
                self.subAccounts = wallets.filter { $0.hidden == true }
                self.reloadSections([AccountArchiveSection.account], animated: false)
            }.done {
                if self.accounts.count == 0 {
                    self.navigationController?.popViewController(animated: true)
                }
            }.catch { e in
                DropAlert().error(message: e.localizedDescription)
                print(e.localizedDescription)
            }
    }

    func unarchiveAccount(_ index: Int) {

        let bgq = DispatchQueue.global(qos: .background)
        guard let session = SessionsManager.current else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.updateSubaccount(subaccount: self.accounts[index].pointer, hidden: false)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
        }.catch { e in
            DropAlert().error(message: e.localizedDescription)
            print(e.localizedDescription)
        }
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
            return accounts.count
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
                cell.configure(account: accounts[indexPath.row], action: action, color: color, isLiquid: isLiquid)
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
            unarchiveAccount(index)
        }
    }
}
