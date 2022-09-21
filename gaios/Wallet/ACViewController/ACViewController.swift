import UIKit
import PromiseKit

enum ACSection: Int, CaseIterable {
    case account = 0
    case edit = 1
    case transaction = 2
    case more = 3
}

class ACViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsBg: UIView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var btnReceive: UIButton!

    private var showAll = false
    var headerH: CGFloat = 54.0

    var presentingWallet: WalletItem! = WalletManager.current!.currentSubaccount!

    lazy var viewModel = { ACViewModel() }()

    override func viewDidLoad() {
        super.viewDidLoad()

        ["ACAccountCell", "ACEditCell", "ACTransactionCell", "ACMoreCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
    }

    func reloadSections(_ sections: [ACSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
    }

    func setContent() {

        // setup right menu bar: settings
        let settingsBtn = UIButton(type: .system)
        settingsBtn.setImage(UIImage(named: "ic_gear"), for: .normal)
        settingsBtn.addTarget(self, action: #selector(settingsBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)

        btnSend.setTitle( "id_send".localized, for: .normal )
        btnReceive.setTitle( "id_receive".localized, for: .normal )

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
    }

    func setStyle() {
        actionsBg.layer.cornerRadius = 5.0
    }

    // tableview refresh gesture
    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {

    }

    // open settings
    @objc func settingsBtnTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        let nvc = storyboard.instantiateViewController(withIdentifier: "UserSettingsNavigationController")
        if let nvc = nvc as? UINavigationController {
            if let vc = nvc.viewControllers.first as? UserSettingsViewController {
                /// Fix
                ///vc.delegate = self
                vc.wallet = presentingWallet
                nvc.modalPresentationStyle = .fullScreen
                present(nvc, animated: true, completion: nil)
            }
        }
    }

    // open send flow
    func sendfromWallet() {

        /// ... ...
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            vc.wallet = presentingWallet
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // open receive flow
    func receiveToWallet() {
        receiveScreen()
    }

    // open receive screen
    func receiveScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ReceiveViewController") as? ReceiveViewController {
            vc.wallet = presentingWallet
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnSend(_ sender: Any) {
        sendfromWallet()
    }

    @IBAction func btnReceive(_ sender: Any) {
        receiveToWallet()
    }

}

extension ACViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return ACSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch ACSection(rawValue: section) {
        case .account:
            return showAll ? 3 : 1
        case .edit:
            return 1
        case .transaction:
            return 5
        case .more:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch ACSection(rawValue: indexPath.section) {
        case .account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ACAccountCell") as? ACAccountCell {
                var action: VoidToVoid?
                if showAll {
                    action = { [weak self] in
                        self?.presentAccountMenu(frame: cell.frame, index: indexPath.row)
                    }
                }
                cell.configure(showAll: showAll)
                cell.selectionStyle = .none
                return cell
            }
        case .edit:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ACEditCell") as? ACEditCell {

                cell.configure(onAdd: nil, onArchive: nil)
                cell.selectionStyle = .none
                return cell
            }
        case .transaction:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ACTransactionCell", for: indexPath) as? ACTransactionCell {
                cell.configure(lblStatus: "Sending",
                               lblDate: "May 21",
                               lblAmount: "41,22 USD",
                               lblWallet: "Work BTC")
                cell.selectionStyle = .none
                return cell
            }
        case .more:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ACMoreCell") as? ACMoreCell {

                cell.configure(onTap: nil)
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch ACSection(rawValue: section) {
        case .transaction:
            return headerH
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        switch ACSection(rawValue: section) {
        case .transaction:
            return headerView( "Latest 30 transactions" )
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {

        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch ACSection(rawValue: indexPath.section) {
        case .account:
            UIView.setAnimationsEnabled(true)
            showAll = !showAll
            reloadSections([ACSection.account], animated: true)
            if indexPath.row > 0 {
//                wm?.currentSubaccount = subaccounts[indexPath.row]
//                assets.removeAll()
//                transactions.removeAll()
//                reloadSections([.asset, .transaction], animated: false)
//                reloadData(scrollTop: true)
            }
        case .edit:
            break
        case .transaction:
            break
//            let transaction = transactions[indexPath.row]
//            let storyboard = UIStoryboard(name: "Transaction", bundle: nil)
//            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionViewController") as? TransactionViewController {
//                vc.transaction = transaction
//                vc.wallet = presentingWallet
//                navigationController?.pushViewController(vc, animated: true)
//            }
        case .more:
            break
        default:
            break
        }
    }
}

extension ACViewController: DialogWalletNameViewControllerDelegate {

    func didRename(name: String, index: Int?) {
//        guard let index = index else {
//            return
//        }
//        let bgq = DispatchQueue.global(qos: .background)
//        guard let session = WalletManager.current?.currentSession else { return }
//        firstly {
//            self.startAnimating()
//            return Guarantee()
//        }.then(on: bgq) {
//            session.renameSubaccount(subaccount: self.subaccounts[index].pointer, newName: name)
//        }.ensure {
//            self.stopAnimating()
//        }.done { _ in
//            self.reloadData()
//            AnalyticsManager.shared.renameAccount(account: self.account, walletType: self.presentingWallet?.type)
//        }.catch { e in
//            DropAlert().error(message: e.localizedDescription)
//            print(e.localizedDescription)
//        }
    }
    func didCancel() {
    }
}

extension ACViewController: UserSettingsViewControllerDelegate, Learn2faViewControllerDelegate {
    func userLogout() {
//        userWillLogout = true
//        self.presentedViewController?.dismiss(animated: true, completion: {
//            DispatchQueue.main.async {
//                self.startLoader(message: NSLocalizedString("id_logout", comment: ""))
//                WalletManager.delete(for: self.account)
//                self.stopLoader()
//                let storyboard = UIStoryboard(name: "Home", bundle: nil)
//                let nav = storyboard.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController
//                UIApplication.shared.keyWindow?.rootViewController = nav
//            }
//        })
    }
}

extension ACViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension ACViewController: PopoverMenuAccountDelegate {
    // subaccounts section: select menu options
    func didSelectionMenuOption(option: MenuAccountOption, index: Int) {
        switch option {
        case .rename:
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletNameViewController") as? DialogWalletNameViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.isAccountRename = true
                vc.delegate = self
                vc.index = index
                present(vc, animated: false, completion: nil)
            }
        case .archive:
            archiveSubaccount(index)
        }
    }

    // subaccounts section: popup on subaccounts
    func presentAccountMenu(frame: CGRect, index: Int) {
//        let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
//        if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuAccountViewController") as? PopoverMenuAccountViewController {
//            popover.delegate = self
//            popover.index = index
//            popover.canArchive = (subaccounts.filter { $0.hidden == false }).count > 1
//            popover.modalPresentationStyle = .popover
//            let popoverPresentationController = popover.popoverPresentationController
//            popoverPresentationController?.backgroundColor = UIColor.customModalDark()
//            popoverPresentationController?.delegate = self
//            popoverPresentationController?.sourceView = self.tableView
//            popoverPresentationController?.sourceRect = CGRect(x: self.tableView.frame.width - 80.0, y: frame.origin.y, width: 60.0, height: 60.0)
//            popoverPresentationController?.permittedArrowDirections = .up
//            self.present(popover, animated: true)
//        }
    }

    // subaccounts section: archive a subaccount
    func archiveSubaccount(_ index: Int) {
//        let bgq = DispatchQueue.global(qos: .background)
//        guard let session = WalletManager.current?.currentSession else { return }
//        firstly {
//            self.startAnimating()
//            return Guarantee()
//        }.then(on: bgq) {
//            session.updateSubaccount(subaccount: self.subaccounts[index].pointer, hidden: true)
//        }.ensure {
//            self.stopAnimating()
//        }.done { _ in
//            let present = (index == 0 ? self.subaccounts[1] : self.subaccounts[0])
//            self.wm?.currentSubaccount = present
//            self.reloadData()
//        }.catch { e in
//            DropAlert().error(message: e.localizedDescription)
//            print(e.localizedDescription)
//        }
    }
}

extension ACViewController {

    func headerView(_ txt: String) -> UIView {

        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.clear
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 18.0, weight: .heavy)
        title.text = txt
        title.textColor = .white
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor, constant: 10.0),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 20)
        ])

        return section
    }
}
