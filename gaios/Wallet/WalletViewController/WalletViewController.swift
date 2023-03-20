import UIKit
import PromiseKit

enum WalletSection: Int, CaseIterable {
    case card
    case balance
    case account
    case transaction
    case footer
}

class WalletViewController: UIViewController {

    enum FooterType {
        case noTransactions
        case none
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsBg: UIView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var btnReceive: UIButton!
    @IBOutlet weak var welcomeLayer: UIView!
    @IBOutlet weak var lblWelcomeTitle: UILabel!
    @IBOutlet weak var lblWelcomeHint: UILabel!
    @IBOutlet weak var btnWelcomeCreate: UIButton!

    //    var assetId: String?

    private var headerH: CGFloat = 54.0
    private var footerH: CGFloat = 54.0
    private var cardH: CGFloat = 64.0
    private var cardHc: CGFloat = 184.0
    private var hideBalance: Bool {
        get {
            return UserDefaults.standard.bool(forKey: AppStorage.hideBalance)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: AppStorage.hideBalance)
        }
    }
    private var sIdx: Int = 0
    private var userWillLogout = false

    var viewModel: WalletViewModel = WalletViewModel()
    var cachedAccount: WalletItem?
    private var notificationObservers: [NSObjectProtocol] = []

    let drawerItem = ((Bundle.main.loadNibNamed("DrawerBarItem", owner: WalletViewController.self, options: nil)![0] as? DrawerBarItem)!)

    override func viewDidLoad() {
        super.viewDidLoad()

        drawerItem.configure(img: viewModel.headerIcon, onTap: {[weak self] () in
                self?.switchNetwork()
        })
        ["AccountCell", "BalanceCell", "TransactionCell", "AlertCardCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        let reloadSections: (([WalletSection], Bool) -> Void)? = { [weak self] (sections, animated) in
            self?.reloadSections(sections, animated: animated)
        }
        viewModel.reloadSections = reloadSections
        viewModel.reloadAccountView = reloadAccountView
        viewModel.welcomeLayerVisibility = welcomeLayerVisibility
        viewModel.preselectAccount = {[weak self] idx in
            self?.sIdx = idx
        }
        setContent()
        setStyle()
        welcomeLayer.isHidden = true

        AnalyticsManager.shared.recordView(.walletOverview, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
        AnalyticsManager.shared.getSurvey { [weak self] widget in
            if let widget = widget {
                DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 1.0) {
                    self?.surveyUI(widget)
                }
            }
        }
    }

    func surveyUI(_ widget: CountlyWidget) {
        let storyboard = UIStoryboard(name: "Survey", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SurveyViewController") as? SurveyViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.widget = widget
            present(vc, animated: false, completion: nil)
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        if userWillLogout == true { return }
        viewModel.loadSubaccounts()
        viewModel.reloadAlertCards()

        [EventType.Transaction, .Block, .AssetsUpdated, .Network, .Settings, .Ticker, .TwoFactorReset].forEach {
            let observer = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: $0.rawValue),
                                                                  object: nil,
                                                                  queue: .main,
                                                                  using: { [weak self] data in
                self?.viewModel.handleEvent(data)
            })
            notificationObservers.append(observer)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        notificationObservers.forEach { observer in
            NotificationCenter.default.removeObserver(observer)
        }
        notificationObservers = []
        drawerIcon(false)
    }

    func drawerIcon(_ show: Bool) {
        if let bar = navigationController?.navigationBar {
            if show {
                let i = UIImageView(frame: CGRect(x: 0.0, y: bar.frame.height / 2.0 - 5.0, width: 7.0, height: 10.0))
                i.image = UIImage(named: "ic_drawer")
                i.tag = 999
                bar.addSubview(i)
            } else {
                bar.subviews.forEach { if $0.tag == 999 { $0.removeFromSuperview()} }
            }
        }
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

        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
            self.tableView.refreshControl?.endRefreshing()
        }
    }

    func setContent() {
        lblWelcomeTitle.text = NSLocalizedString("id_welcome_to_your_wallet", comment: "")
        lblWelcomeHint.text = NSLocalizedString("id_create_your_first_account_to", comment: "")
        btnWelcomeCreate.setTitle(NSLocalizedString("id_create_account", comment: ""), for: .normal)

        btnSend.setTitle( "id_send".localized, for: .normal )
        btnReceive.setTitle( "id_receive".localized, for: .normal )

        if viewModel.watchOnly {
            btnSend.setTitle( "id_sweep".localized, for: .normal )
            btnSend.setImage(UIImage(named: "qr_sweep"), for: .normal)
        }

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(callPullToRefresh(_:)), for: .valueChanged)
    }

    func loadNavigationBtns() {
        let leftItem: UIBarButtonItem = UIBarButtonItem(customView: drawerItem)
        navigationItem.leftBarButtonItem = leftItem

        let desiredWidth = 135.0
        let desiredHeight = 35.0
        let widthConstraint = NSLayoutConstraint(item: drawerItem, attribute: .width, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: desiredWidth)
        let heightConstraint = NSLayoutConstraint(item: drawerItem, attribute: .height, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: desiredHeight)
        drawerItem.addConstraints([widthConstraint, heightConstraint])

        // setup right menu bar: settings
        let settingsBtn = UIButton(type: .system)
        settingsBtn.contentEdgeInsets = UIEdgeInsets(top: 7.0, left: 7.0, bottom: 7.0, right: 7.0)
        settingsBtn.setImage(UIImage(named: "ic_gear"), for: .normal)
        settingsBtn.addTarget(self, action: #selector(settingsBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    func setStyle() {
        actionsBg.layer.cornerRadius = 5.0
        btnWelcomeCreate.setStyle(.primary)
    }

    // tableview refresh gesture
    @objc func callPullToRefresh(_ sender: UIRefreshControl? = nil) {
        viewModel.loadSubaccounts()
        // viewModel.reloadAlertCards()
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
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: NSLocalizedString("Wallet Preferences".localized, comment: ""), type: .walletPrefs, items: WalletPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    // open send flow
    func sendfromWallet() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            guard let model = viewModel.accountCellModels[safe: sIdx] else { return }
            vc.viewModel = SendViewModel(account: model.account, inputType: .transaction, transaction: nil)
            vc.accounts = viewModel.cachedSubaccounts
            vc.fixedWallet = false
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // open receive screen
    func receiveScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ReceiveViewController") as? ReceiveViewController {
            guard let model = viewModel.accountCellModels[safe: sIdx] else { return }
            vc.viewModel = ReceiveViewModel(account: model.account,
                                            accounts: viewModel.subaccounts)
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func assetsScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AssetsViewController") as? AssetsViewController {
            vc.viewModel = AssetsViewModel(assetCellModels: viewModel.walletAssetCellModels)
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func reloadAccountView() {
        guard let model = viewModel.accountCellModels[safe: sIdx] else { return }
        if let vc = navigationController?.viewControllers.last as? AccountViewController {
            vc.reloadFromParent(model)
        }
    }

    func welcomeLayerVisibility() {
        navigationItem.leftBarButtonItem = nil
        navigationItem.rightBarButtonItems = []
        drawerIcon(false)
        if viewModel.accountCellModels.count > 0 {
            welcomeLayer.isHidden = true
            loadNavigationBtns()
            drawerIcon(true)
        } else {
            welcomeLayer.isHidden = false
        }
    }

    func accountDetail(model: AccountCellModel?) {
        guard let model = model else { return }
        print(sIdx)
        print(model)
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountViewController") as? AccountViewController {
            vc.viewModel = AccountViewModel(model: model, account: model.account, cachedBalance: viewModel.cachedBalance)
            vc.delegate = self
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func createAccount() {
        let storyboard = UIStoryboard(name: "Utility", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SecuritySelectViewController") as? SecuritySelectViewController {
            vc.viewModel = SecuritySelectViewModel(asset: "btc")
            vc.delegate = self
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // dismiss remote alert
    func remoteAlertDismiss() {
        viewModel.remoteAlert = nil
        viewModel.reloadAlertCards()
    }

    // open system message view
    func systemMessageScreen(msg: SystemMessage) {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SystemMessageViewController") as? SystemMessageViewController {
            vc.msg = msg
            navigationController?.pushViewController(vc, animated: true)
        }
    }
    // open Learn 2fa controller for reset/dispute wallet
    func twoFactorResetMessageScreen(msg: TwoFactorResetMessage) {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "Learn2faViewController") as? Learn2faViewController {
            vc.message = msg
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func onShieldBtn(_ idx: Int) {
        let account = viewModel.accountCellModels[safe: idx]?.account
        cachedAccount = account
        twoFactorAuthenticatorDialog()
    }

    func twoFactorAuthenticatorDialog() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Enable 2FA", type: .enable2faPrefs, items: Enable2faPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func navigateTo2fa(_ account: WalletItem) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "TwoFactorAuthenticationViewController") as? TwoFactorAuthenticationViewController {
            vc.showBitcoin = !account.gdkNetwork.liquid
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnSend(_ sender: Any) {
        sendfromWallet()
    }

    @IBAction func btnReceive(_ sender: Any) {
        receiveScreen()
    }

    @IBAction func btnWelcomeCreate(_ sender: Any) {
        AnalyticsManager.shared.onAccountFirst(account: AccountsRepository.shared.current)
        createAccount()
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
            return viewModel.accountCellModels.count
        case .card:
            return viewModel.alertCardCellModel.count
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
                               hideBalance: hideBalance,
                               onHide: {[weak self] value in
                    self?.hideBalance = value

                    self?.reloadSections([WalletSection.account, WalletSection.transaction], animated: false)
                },
                               onAssets: {[weak self] in
                    self?.assetsScreen()
                }, onConvert: { [weak self] in
                    self?.viewModel.rotateBalanceDisplayMode()
                })
                cell.selectionStyle = .none
                return cell
            }
        case .account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AccountCell.identifier, for: indexPath) as? AccountCell {

                let onShield: ((Int) -> Void)? = { [weak self] idx in
                    self?.onShieldBtn(idx)
                }
                cell.configure(model: viewModel.accountCellModels[indexPath.row],
                               cIdx: indexPath.row,
                               sIdx: sIdx,
                               hideBalance: hideBalance,
                               isLast: indexPath.row == viewModel.accountCellModels.count - 1,
                               onSelect: {[weak self] in
                    self?.accountDetail(model: self?.viewModel.accountCellModels[indexPath.row])
                }, onCopy: nil,
                               onShield: onShield
                )
                cell.selectionStyle = .none
                return cell
            }
        case .card:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell {
                let alertCard = viewModel.alertCardCellModel[indexPath.row]
                switch alertCard.type {
                case .reset(let msg), .dispute(let msg):
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: {[weak self] in
                                        self?.twoFactorResetMessageScreen(msg: msg)
                                    }, onDismiss: nil)
                case .reactivate:
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .systemMessage(let msg):
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: {[weak self] in
                                        self?.systemMessageScreen(msg: msg)
                                    },
                                   onDismiss: nil)
                case .fiatMissing:
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .testnetNoValue:
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .ephemeralWallet:
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .remoteAlert:
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: (viewModel.remoteAlert?.link ?? "" ).isEmpty ? nil : {[weak self] in
                        if let url = URL(string: self?.viewModel.remoteAlert?.link ?? "") {
                            UIApplication.shared.open(url)
                        }
                    },
                                   onDismiss: {[weak self] in
                        self?.remoteAlertDismiss()
                    })
                case .login:
                    cell.configure(viewModel.alertCardCellModel[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                }
                cell.selectionStyle = .none
                return cell
            }
        case .transaction:
            if let cell = tableView.dequeueReusableCell(withIdentifier: TransactionCell.identifier, for: indexPath) as? TransactionCell {
                cell.configure(model: viewModel.txCellModels[indexPath.row], hideBalance: hideBalance)
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
        case .transaction:
            return viewModel.cachedTransactions.count == 0 ? footerH : 1.0
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
            return headerView(NSLocalizedString("id_latest_transactions", comment: ""))
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        switch WalletSection(rawValue: section) {
        case .account:
            return footerView(.none)
        case .transaction:
            return viewModel.cachedTransactions.count == 0 ? footerView(.noTransactions) : footerView(.none)
        default:
            return footerView(.none)
        }
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        switch WalletSection(rawValue: indexPath.section) {
        case .balance, .card, .footer:
            return nil
        default:
            return indexPath
        }
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
                vc.wallet = transaction.subaccountItem
                navigationController?.pushViewController(vc, animated: true)
            }
            tableView.deselectRow(at: indexPath, animated: false)
            tableView.selectRow(at: IndexPath(row: sIdx, section: WalletSection.account.rawValue), animated: false, scrollPosition: .none)
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
        userWillLogout = true
        self.presentedViewController?.dismiss(animated: true, completion: {
            let account = self.viewModel.wm?.account
            if account?.isHW ?? false {
                BLEManager.shared.dispose()
            }
            DispatchQueue.main.async {
                WalletsRepository.shared.delete(for: account?.id ?? "")
                let storyboard = UIStoryboard(name: "Home", bundle: nil)
                let nav = storyboard.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController
                UIApplication.shared.keyWindow?.rootViewController = nav
            }
        })
    }

    func refresh() {
        viewModel.loadSubaccounts()
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
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 25),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 20)
        ])

        return section
    }

    func footerView(_ type: FooterType) -> UIView {

        switch type {
        case .none:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        case .noTransactions:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerH))
            section.backgroundColor = .clear

            let lblNoTransactions = UILabel(frame: .zero)
            lblNoTransactions.font = UIFont.systemFont(ofSize: 14, weight: .regular)
            lblNoTransactions.textColor = UIColor.gGrayTxt()
            lblNoTransactions.numberOfLines = 0
            lblNoTransactions.text = NSLocalizedString("id_your_transactions_will_be_shown", comment: "")
            lblNoTransactions.translatesAutoresizingMaskIntoConstraints = false
            section.addSubview(lblNoTransactions)

            var padding: CGFloat = 50.0
            lblNoTransactions.textAlignment = .left

            if !viewModel.isTxLoading {
                padding = 25.0
            }

            NSLayoutConstraint.activate([
                lblNoTransactions.topAnchor.constraint(equalTo: section.topAnchor, constant: 0.0),
                lblNoTransactions.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: 0.0),
                lblNoTransactions.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: padding),
                lblNoTransactions.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 0.0)
            ])

            if viewModel.isTxLoading {
                let loader = UIActivityIndicatorView(style: .white)
                section.addSubview(loader)
                loader.startAnimating()
                loader.translatesAutoresizingMaskIntoConstraints = false
                let horizontalConstraint = NSLayoutConstraint(item: loader,
                                                              attribute: .left,
                                                              relatedBy: .equal,
                                                              toItem: section,
                                                              attribute: .left,
                                                              multiplier: 1,
                                                              constant: 25.0)
                let verticalConstraint = NSLayoutConstraint(item: loader,
                                                            attribute: .centerY,
                                                            relatedBy: .equal,
                                                            toItem: lblNoTransactions,
                                                            attribute: .centerY,
                                                            multiplier: 1,
                                                            constant: 0)
                NSLayoutConstraint.activate([horizontalConstraint, verticalConstraint])
            }
            return section
        }

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
        if account.id == viewModel.wm?.account.id ?? "" {
            return
        }
        AccountNavigator.goLogin(account: account)
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

extension WalletViewController: DialogListViewControllerDelegate {
    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch type {
        case .walletPrefs:
            if let item = WalletPrefs.getPrefs()[safe: index] {
                switch item {
                case .settings:
                    let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
                    let nvc = storyboard.instantiateViewController(withIdentifier: "UserSettingsNavigationController")
                    if let nvc = nvc as? UINavigationController {
                        if let vc = nvc.viewControllers.first as? UserSettingsViewController {
                            vc.delegate = self
                            nvc.modalPresentationStyle = .fullScreen
                            present(nvc, animated: true, completion: nil)
                        }
                    }
                case .createAccount:
                    AnalyticsManager.shared.newAccount(account: AccountsRepository.shared.current)
                    createAccount()
                case .logout:
                    userLogout()
                }
            }
        case .enable2faPrefs:
            switch Enable2faPrefs(rawValue: index) {
            case .add:
                if let account = cachedAccount {
                    let session = account.session
                    let enabled2FA = session?.twoFactorConfig?.anyEnabled ?? false
                    let isSS = session?.gdkNetwork.electrum ?? false
                    if isSS {
                        showError("Two-Factor authentication not available for singlesig accounts")
                        return
                    } else if enabled2FA {
                        showError("Two factor authentication is already enabled")
                        return
                    }
                    navigateTo2fa(account)
                }
            default:
                break
            }
        default:
            break
        }
    }
}
extension WalletViewController: SecuritySelectViewControllerDelegate {
    func didCreatedWallet(_ wallet: WalletItem) {

        AnalyticsManager.shared.createAccount(account: AccountsRepository.shared.current, walletType: wallet.type)
        viewModel.onCreateAccount(wallet)
    }
}
extension WalletViewController: AccountViewControllerDelegate {
    func didArchiveAccount() {
        sIdx = 0
    }
}
