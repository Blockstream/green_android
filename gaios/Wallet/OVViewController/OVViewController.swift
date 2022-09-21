import UIKit

enum OVSection: Int, CaseIterable {
    case balance = 0
    case asset = 1
    case card = 2
    case transaction = 3
}

class OVViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsBg: UIView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var btnReceive: UIButton!

    var headerH: CGFloat = 54.0

    lazy var viewModel = { OVViewModel() }()

    override func viewDidLoad() {
        super.viewDidLoad()

        ["OVAssetCell", "OVBalanceCell", "OVTransactionCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
        initViewModel()
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
        settingsBtn.accessibilityIdentifier = AccessibilityIdentifiers.OverviewScreen.settingsBtn

        btnSend.setTitle( "id_send".localized, for: .normal )
        btnReceive.setTitle( "id_receive".localized, for: .normal )

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
    }

    func setStyle() {
        actionsBg.layer.cornerRadius = 5.0
    }

    func initViewModel() {
        viewModel.getAssets()
        viewModel.reloadTableView = { [weak self] in
            DispatchQueue.main.async {
                if self?.tableView.refreshControl?.isRefreshing ?? false {
                    self?.tableView.refreshControl?.endRefreshing()
                }
                self?.tableView.reloadData()
            }
        }
    }

    // tableview refresh gesture
    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        viewModel.getAssets()
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

    // open wallet selector drawer
    @objc func switchNetwork() {
        let storyboard = UIStoryboard(name: "DrawerNetworkSelection", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DrawerNetworkSelection") as? DrawerNetworkSelectionViewController {
            vc.transitioningDelegate = self
            vc.modalPresentationStyle = .custom
//            vc.delegate = self
            present(vc, animated: true, completion: nil)
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

    @IBAction func btnSend(_ sender: Any) {
        sendfromWallet()
    }

    @IBAction func btnReceive(_ sender: Any) {
        receiveToWallet()
    }
}

extension OVViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return HomeSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case OVSection.balance.rawValue:
            return 1
        case OVSection.asset.rawValue:
            return viewModel.assetCellModels.count
        case OVSection.card.rawValue:
            return 0
        case OVSection.transaction.rawValue:
            return 2
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case OVSection.balance.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: OVBalanceCell.identifier, for: indexPath) as? OVBalanceCell {
                let cellVm = viewModel.getBalanceCellModel()
                cell.viewModel = cellVm
                cell.selectionStyle = .none
                return cell
            }
        case OVSection.asset.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: OVAssetCell.identifier, for: indexPath) as? OVAssetCell {
                let cellVm = viewModel.getAssetCellModels(at: indexPath)
                cell.viewModel = cellVm
                cell.selectionStyle = .none
                return cell
            }
        case OVSection.card.rawValue:

            /// Fix
            return UITableViewCell()
        case OVSection.transaction.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OVTransactionCell", for: indexPath) as? OVTransactionCell {

                cell.configure(lblStatus: "Sending",
                               lblDate: "May 21",
                               lblAmount: "41,22 USD",
                               lblWallet: "Work BTC")
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch OVSection(rawValue: section) {
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
        switch OVSection(rawValue: section) {
        case .transaction:
            return headerView( "Transactions in Progress" )
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ACViewController") as? ACViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}

extension OVViewController: UIViewControllerTransitioningDelegate {
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

extension OVViewController {

    func headerView(_ txt: String) -> UIView {

        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.clear
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 18.0, weight: .heavy)
        title.text = txt
        title.textColor = .white.withAlphaComponent(0.4)
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
