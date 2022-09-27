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

    var assetId: String?

    private var showAll = true
    private var headerH: CGFloat = 54.0

    private lazy var viewModel = { ACViewModel() }()

    override func viewDidLoad() {
        super.viewDidLoad()

        ["ACAccountCell", "ACEditCell", "ACTransactionCell", "ACMoreCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
        initViewModel()
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

    func initViewModel() {
        viewModel.getSubaccounts(assetId: assetId ?? "btc")
        viewModel.getTransactions(assetId: assetId ?? "btc", max: 10)
        viewModel.reloadTableView = { [weak self] in
            DispatchQueue.main.async {
                if self?.tableView.refreshControl?.isRefreshing ?? false {
                    self?.tableView.refreshControl?.endRefreshing()
                }
                self?.tableView.reloadData()
            }
        }
        let reloadSections: (([ACSection], Bool) -> Void)? = { [weak self] (sections, animated) in
            self?.reloadSections(sections, animated: true)
        }
        viewModel.reloadSections = reloadSections
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
            let num = viewModel.accountCellModels.count
            return showAll ? num : ( num == 0 ? 0 : 1)
//            return showAll ? viewModel.accountCellModels.count : 1
        case .edit:
            return 1
        case .transaction:
            return viewModel.txCellModels.count
        case .more:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch ACSection(rawValue: indexPath.section) {
        case .account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: ACAccountCell.identifier, for: indexPath) as? ACAccountCell {
                let cellVm = viewModel.getAccountCellModels(at: indexPath)
                cell.configure(viewModel: cellVm, showAll: showAll, action: { [weak self] in
                    self?.accountPrefs()
                })
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
            if let cell = tableView.dequeueReusableCell(withIdentifier: ACTransactionCell.identifier, for: indexPath) as? ACTransactionCell {
                let cellVm = viewModel.getTransactionCellModels(at: indexPath)
                cell.viewModel = cellVm
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
                let subaccount = viewModel.cachedSubaccounts[indexPath.row]
                viewModel.getTransactions(assetId: assetId ?? "btc", subaccounts: [subaccount], page: 0)
            } else {
                viewModel.getTransactions(assetId: assetId ?? "btc", page: 0, max: 10)
            }
        case .edit:
            break
        case .transaction:
            let transaction = viewModel.cachedTransactions[indexPath.row]
            let storyboard = UIStoryboard(name: "Transaction", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionViewController") as? TransactionViewController {
                vc.transaction = transaction
                navigationController?.pushViewController(vc, animated: true)
            }
        case .more:
            break
        default:
            break
        }
    }
}

extension ACViewController: DialogWalletNameViewControllerDelegate {

    func didRename(name: String, index: Int?) {
        //...
    }
    func didCancel() {
    }
}

extension ACViewController: UserSettingsViewControllerDelegate, Learn2faViewControllerDelegate {
    func userLogout() {
        // ...
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
