import UIKit
import PromiseKit

enum WalletSection: Int, CaseIterable {
    case balance
    case account
    case transaction
    case footer
}

class WalletViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsBg: UIView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var btnReceive: UIButton!

//    var assetId: String?

    private var headerH: CGFloat = 54.0
    private var cardH: CGFloat = 64.0
    private var cardHc: CGFloat = 184.0

    private var sIdx: Int = 0

    var viewModel: WalletViewModel = WalletViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()

        viewModel.loadSubaccounts()
        //        viewModel.getTransactions(assetId: assetId ?? "btc", max: 10)
//                viewModel.reloadTableView = { [weak self] in
//                    DispatchQueue.main.async {
//                        if self?.tableView.refreshControl?.isRefreshing ?? false {
//                            self?.tableView.refreshControl?.endRefreshing()
//                        }
//                        self?.tableView.reloadData()
//                    }
//                }
        let reloadSections: (([WalletSection], Bool) -> Void)? = { [weak self] (sections, animated) in
            self?.reloadSections(sections, animated: true)
        }
        viewModel.reloadSections = reloadSections

        ["AccountCell", "BalanceCell", "TransactionCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

    }

    func reloadSections(_ sections: [WalletSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
        if sections.contains(WalletSection.account) {
            tableView.selectRow(at: IndexPath(row: sIdx, section: WalletSection.account.rawValue), animated: false, scrollPosition: .none)
        }
    }

    func setContent() {

        let drawerItem = ((Bundle.main.loadNibNamed("DrawerBarItem", owner: self, options: nil)![0] as? DrawerBarItem)!)
        drawerItem.configure {
            [weak self] () in
                self?.switchNetwork()
        }
        let leftItem: UIBarButtonItem = UIBarButtonItem(customView: drawerItem)
        navigationItem.leftBarButtonItem = leftItem

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

    // open wallet selector drawer
    @objc func switchNetwork() {
        let storyboard = UIStoryboard(name: "DrawerNetworkSelection", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DrawerNetworkSelection") as? DrawerNetworkSelectionViewController {
            vc.transitioningDelegate = self
            vc.modalPresentationStyle = .custom
            vc.delegate = self
            present(vc, animated: true, completion: nil)
        }
    }

    // open settings
    @objc func settingsBtnTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        let nvc = storyboard.instantiateViewController(withIdentifier: "UserSettingsNavigationController")
        if let nvc = nvc as? UINavigationController {
            if let vc = nvc.viewControllers.first as? UserSettingsViewController {
                /// Fix
                ///vc.delegate = self
                nvc.modalPresentationStyle = .fullScreen
                present(nvc, animated: true, completion: nil)
            }
        }
    }

    // open send flow
    func sendfromWallet() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
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
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func accountPrefs() {

        print("accaount prefs")
    }

    func assetsScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AssetsViewController") as? AssetsViewController {
            vc.viewModel = AssetsViewModel(assetCellModels: viewModel.walletAssetCellModels)
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func accountDetail(model: AccountCellModel?) {
        guard let model = model else { return }

        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountViewController") as? AccountViewController, let account = viewModel.presentingWallet {
            vc.viewModel = AccountViewModel(model: model, account: account)
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

extension WalletViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return WalletSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch WalletSection(rawValue: section) {
        case .balance:
            return viewModel.balanceCellModel == nil ? 0 : 1
        case .account:
//            let num = viewModel.accountCellModels.count
            return viewModel.accountCellModels.count //showAll ? num : ( num == 0 ? 0 : 1)
//            return showAll ? viewModel.accountCellModels.count : 1
        case .transaction:
            return viewModel.txCellModels.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch WalletSection(rawValue: indexPath.section) {
        case .balance:
            if let cell = tableView.dequeueReusableCell(withIdentifier: BalanceCell.identifier, for: indexPath) as? BalanceCell, let model = viewModel.balanceCellModel {
                cell.configure(model: model,
                               cachedBalance: viewModel.cachedBalance,
                               onAssets: {[weak self] in
                    self?.assetsScreen()
                })
                cell.selectionStyle = .none
                return cell
            }
        case .account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AccountCell.identifier, for: indexPath) as? AccountCell {
                cell.configure(model: viewModel.accountCellModels[indexPath.row],
                               cIdx: indexPath.row,
                               sIdx: sIdx,
                               isLast: indexPath.row == viewModel.accountCellModels.count - 1,
                               onSelect: {[weak self] in
                    self?.accountDetail(model: self?.viewModel.accountCellModels[indexPath.row])
                })
                cell.selectionStyle = .none
                return cell
            }
        case .transaction:
            if let cell = tableView.dequeueReusableCell(withIdentifier: TransactionCell.identifier, for: indexPath) as? TransactionCell {
                cell.configure(model: viewModel.txCellModels[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch WalletSection(rawValue: section) {
        case .transaction:
            return headerH
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch WalletSection(rawValue: section) {
        case .footer:
            return 100.0
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        switch WalletSection(rawValue: indexPath.section) {
        case .account:
            return indexPath.row == sIdx ? cardHc : cardH
        default:
            return UITableView.automaticDimension
        }
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        switch WalletSection(rawValue: section) {
        case .transaction:
            return headerView( "Latest transactions" )
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {

        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch WalletSection(rawValue: indexPath.section) {
        case .account:
            sIdx = indexPath.row
            tableView.beginUpdates()
            tableView.endUpdates()
        case .transaction:
            let transaction = viewModel.cachedTransactions[indexPath.row]
            let storyboard = UIStoryboard(name: "Transaction", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionViewController") as? TransactionViewController {
                vc.transaction = transaction
                navigationController?.pushViewController(vc, animated: true)
            }
        default:
            break
        }
    }
}

extension WalletViewController: DialogWalletNameViewControllerDelegate {

    func didRename(name: String, index: Int?) {
        //...
    }
    func didCancel() {
    }
}

extension WalletViewController: UserSettingsViewControllerDelegate, Learn2faViewControllerDelegate {
    func userLogout() {
        // ...
    }
}

extension WalletViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension WalletViewController {

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

extension WalletViewController: UIViewControllerTransitioningDelegate {
    func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        if let presented = presented as? DrawerNetworkSelectionViewController {
            return DrawerPresentationController(presentedViewController: presented, presenting: presenting)
        }
        return ModalPresentationController(presentedViewController: presented, presenting: presenting)
    }

    func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        if presented as? DrawerNetworkSelectionViewController != nil {
            return DrawerAnimator(isPresenting: true)
        } else {
            return ModalAnimator(isPresenting: true)
        }
    }

    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        if dismissed as? DrawerNetworkSelectionViewController != nil {
            return DrawerAnimator(isPresenting: false)
        } else {
            return ModalAnimator(isPresenting: false)
        }
    }
}

extension WalletViewController: DrawerNetworkSelectionDelegate {

    // accounts drawer: add new waller
    func didSelectAddWallet() {
        AccountNavigator.goCreateRestore()
    }

    // accounts drawer: select another account
    func didSelectAccount(account: Account) {
        // don't switch if same account selected
//        if account.id == self.account?.id ?? "" {
//            return
//        }
//        AccountNavigator.goLogin(account: account)
    }

    // accounts drawer: select hw account
    func didSelectHW(account: Account) {
        AccountNavigator.goHWLogin(isJade: account.isJade)
    }

    // accounts drawer: select app settings
    func didSelectSettings() {
        self.presentedViewController?.dismiss(animated: true, completion: {
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
                self.present(vc, animated: true) {}
            }
        })
    }

    func didSelectAbout() {
        self.presentedViewController?.dismiss(animated: true, completion: {
            let storyboard = UIStoryboard(name: "About", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AboutViewController") as? AboutViewController {
                self.navigationController?.pushViewController(vc, animated: true)
            }
        })
    }
}
