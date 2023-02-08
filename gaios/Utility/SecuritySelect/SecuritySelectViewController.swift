import UIKit
import PromiseKit

enum SecuritySelectSection: Int, CaseIterable {
    case asset
    case policy
    case footer
}

protocol SecuritySelectViewControllerDelegate: AnyObject {
    func didCreatedWallet(_ wallet: WalletItem)
}

class SecuritySelectViewController: UIViewController {

    enum FooterType {
        case noTransactions
        case none
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnAdvanced: UIButton!

    private var headerH: CGFloat = 54.0
    private var footerH: CGFloat = 54.0

    var viewModel: SecuritySelectViewModel!
    weak var delegate: SecuritySelectViewControllerDelegate?
    var visibilityState: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()

        let reloadSections: (([SecuritySelectSection], Bool) -> Void)? = { [weak self] (sections, animated) in
            //self?.reloadSections(sections, animated: true)
            self?.tableView.reloadData()
        }
        viewModel.reloadSections = reloadSections
        viewModel.success = { [weak self] in
            self?.navigationController?.popViewController(animated: true)
        }
        viewModel.error = showError
        viewModel.unarchiveCreateDialog = unarchiveCreateDialog

        ["PolicyCell", "AssetSelectCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
    }

    func unarchiveCreateDialog() -> Promise<Bool> {
        return Promise { result in
            let alert = UIAlertController(title: NSLocalizedString("Archived Account", comment: ""),
                                          message: NSLocalizedString("There is already an archived account. Do you want to create a new one?", comment: ""),
                                          preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("Unarchived Account", comment: ""),
                                          style: .cancel) { (_: UIAlertAction) in
                result.fulfill(false)
            })
            alert.addAction(UIAlertAction(title: NSLocalizedString("Create", comment: ""),
                                          style: .default) { (_: UIAlertAction) in
                result.fulfill(true)
            })
            DispatchQueue.main.async {
                self.present(alert, animated: true, completion: nil)
            }
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

    }

    func reloadSections(_ sections: [SecuritySelectSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
    }

    func setContent() {
        title = "Create New Account"
        btnAdvanced.setTitle( visibilityState ? "Hide Advanced Options" : "See Advanced Options", for: .normal)
    }

    func setStyle() {
    }

    @IBAction func btnAdvanced(_ sender: Any) {
        viewModel?.showAll.toggle()
        visibilityState = !visibilityState
        setContent()
    }
}

extension SecuritySelectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return SecuritySelectSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch SecuritySelectSection(rawValue: section) {
        case .asset:
            return 1
        case .policy:
            return viewModel?.getPolicyCellModels().count ?? 0
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        btnAdvanced.isHidden = !viewModel.isAdvancedEnable()

        switch SecuritySelectSection(rawValue: indexPath.section) {
        case .asset:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AssetSelectCell.identifier, for: indexPath) as? AssetSelectCell,
               let model = viewModel?.assetCellModel {
                cell.configure(model: model, showEditIcon: true)
                cell.selectionStyle = .none
                return cell
            }
        case .policy:
            if let cell = tableView.dequeueReusableCell(withIdentifier: PolicyCell.identifier, for: indexPath) as? PolicyCell,
               let model = viewModel {
                cell.configure(model: model.getPolicyCellModels()[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch SecuritySelectSection(rawValue: section) {
        default:
            return headerH
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch SecuritySelectSection(rawValue: section) {
        case .footer:
            return 100.0
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        switch SecuritySelectSection(rawValue: indexPath.section) {
        default:
            return UITableView.automaticDimension
        }
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        switch SecuritySelectSection(rawValue: section) {
        case .asset:
            return headerView( "Asset" )
        case .policy:
            return headerView( "Security Policy" )
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        switch SecuritySelectSection(rawValue: section) {
        default:
            return footerView(.none)
        }
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        switch SecuritySelectSection(rawValue: indexPath.section) {
        default:
            return indexPath
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch SecuritySelectSection(rawValue: indexPath.section) {
        case .asset:
            let storyboard = UIStoryboard(name: "Utility", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetSelectViewController") as? AssetSelectViewController {
                let assets = WalletManager.current?.registry.all ?? []
                vc.viewModel = AssetSelectViewModel(assets: assets, enableAnyAsset: true)
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        case .policy:
            let policy = viewModel.getPolicyCellModels()[indexPath.row].policy
            if policy == .TwoOfThreeWith2FA {
                let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreateRecoveryKeyViewController") as? AccountCreateRecoveryKeyViewController {
                    if let network = viewModel.getNetwork(for: policy, liquid: viewModel.asset != "btc"),
                       let session = viewModel.getSession(for: network) {
                        vc.session = session
                        vc.delegate = self
                        navigationController?.pushViewController(vc, animated: true)
                    }
                }
            } else {
                firstly { self.startLoader(message: "Creating new account"); return Guarantee() }
                    .then { self.viewModel.create(policy: policy, asset: self.viewModel.asset, params: nil) }
                    .ensure { self.stopLoader() }
                    .done { wallet in
                        DropAlert().success(message: "Account created")
                        self.navigationController?.popViewController(animated: true)
                        self.delegate?.didCreatedWallet(wallet)
                    }.catch { err in self.showError(err)}
            }
        default:
            break
        }
    }

    func createSubaccount(policy: PolicyCellType, asset: String, params: CreateSubaccountParams?) {
        firstly { self.startLoader(message: "Creating new account"); return Guarantee() }
            .then { self.viewModel.create(policy: policy, asset: asset, params: params) }
            .ensure { self.stopLoader() }
            .done { wallet in
                DropAlert().success(message: "Account created")
                self.navigationController?.popToViewController(ofClass: WalletViewController.self, animated: true)
                self.delegate?.didCreatedWallet(wallet)
            }.catch { err in self.showError(err)}
    }
}

extension SecuritySelectViewController {

    func headerView(_ txt: String) -> UIView {

        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.clear
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 14.0, weight: .semibold)
        title.text = txt
        title.textColor = .white.withAlphaComponent(0.6)
        title.numberOfLines = 1

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor, constant: 10.0),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 30),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 30)
        ])

        return section
    }

    func footerView(_ type: FooterType) -> UIView {

        switch type {
        default:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        }
    }
}

extension SecuritySelectViewController: AssetSelectViewControllerDelegate {
    func didSelectAsset(_ assetId: String) {
        viewModel?.asset = assetId
    }

    func didSelectAnyAsset() {
        /// handle any asset case
        print("didSelectAnyAsset")
        viewModel?.asset = AssetInfo.lbtcId
    }
}

extension SecuritySelectViewController: SecuritySelectViewControllerDelegate {
    func didCreatedWallet(_ wallet: WalletItem) {
        self.navigationController?.popViewController(animated: true)
        self.delegate?.didCreatedWallet(wallet)
    }
}

extension SecuritySelectViewController: AccountCreateRecoveryKeyDelegate {
    func didPublicKey(_ key: String) {
        let cellModel = PolicyCellModel.from(policy: .TwoOfThreeWith2FA)
        let params = CreateSubaccountParams(name: viewModel.uniqueName(cellModel.name),
                                            type: .twoOfThree,
                                            recoveryMnemonic: nil,
                                            recoveryXpub: key)
        createSubaccount(policy: .TwoOfThreeWith2FA, asset: viewModel.asset, params: params)
    }

    func didNewRecoveryPhrase(_ mnemonic: String) {
        let cellModel = PolicyCellModel.from(policy: .TwoOfThreeWith2FA)
        let params = CreateSubaccountParams(name: viewModel.uniqueName(cellModel.name),
                                            type: .twoOfThree,
                                            recoveryMnemonic: mnemonic,
                                            recoveryXpub: nil)
        createSubaccount(policy: .TwoOfThreeWith2FA, asset: viewModel.asset, params: params)
    }

    func didExistingRecoveryPhrase(_ mnemonic: String) {
        let cellModel = PolicyCellModel.from(policy: .TwoOfThreeWith2FA)
        let params = CreateSubaccountParams(name: viewModel.uniqueName(cellModel.name),
                                            type: .twoOfThree,
                                            recoveryMnemonic: mnemonic,
                                            recoveryXpub: nil)
        createSubaccount(policy: .TwoOfThreeWith2FA, asset: viewModel.asset, params: params)
    }
}
