import UIKit
import PromiseKit

protocol AssetExpandableSelectViewControllerDelegate: AnyObject {
    func didSelectReceiver(assetId: String, account: WalletItem)
}

class AssetExpandableSelectViewController: UIViewController {

    enum FooterType {
        case none
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var searchCard: UIView!
    @IBOutlet weak var btnSearch: UIButton!
    @IBOutlet weak var searchField: UITextField!

    private var headerH: CGFloat = 54.0
    private var footerH: CGFloat = 54.0

    var viewModel: AssetExpandableSelectViewModel!
    weak var delegate: AssetExpandableSelectViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        ["AccountSelectSubCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        searchField.delegate = self
        setContent()
        setStyle()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        viewModel.loadAssets()
        tableView.reloadData()
    }

    func setContent() {
        title = "Select asset"
        searchField.attributedPlaceholder = NSAttributedString(string: "Search Asset", attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.4)])
    }

    func setStyle() {
        searchCard.cornerRadius = 5.0
    }

    @objc func triggerTextChange() {
        viewModel.selectedSection = -1
        viewModel.search(searchField.text ?? "")
        tableView.reloadData()
    }

    func onCreate(asset: AssetInfo?) {
        AnalyticsManager.shared.newAccount(account: AccountsManager.shared.current)
        let storyboard = UIStoryboard(name: "Utility", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SecuritySelectViewController") as? SecuritySelectViewController {
            let asset = asset?.assetId ?? getGdkNetwork("liquid").getFeeAsset()
            vc.viewModel = SecuritySelectViewModel(asset: asset)
            vc.delegate = self
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func onEditingChange(_ sender: Any) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }
}

extension AssetExpandableSelectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        let cnt = viewModel.assetSelectCellModelsFilter.count
        return cnt + 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if viewModel.selectedSection != section {
            return 0
        }
        return viewModel.accountSelectSubCellModels.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let cell = tableView.dequeueReusableCell(withIdentifier: AccountSelectSubCell.identifier, for: indexPath) as? AccountSelectSubCell {
           cell.configure(model: viewModel.accountSelectSubCellModels[indexPath.row], isLast: viewModel.accountSelectSubCellModels.count - 1 == indexPath.row)
            cell.selectionStyle = .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        if viewModel.selectedSection == section {
            return UITableView.automaticDimension
        }
        return 0.1
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {

        var assetInfo = AssetInfo.lbtc
        if viewModel.selectedSection == section {
            let cnt = viewModel.assetSelectCellModelsFilter.count
            if cnt == section && viewModel.enableAnyAsset {
            } else {
                let cellModel = viewModel.assetSelectCellModelsFilter[section]
                assetInfo = cellModel.asset ?? AssetInfo.lbtc
            }
            if let createView = Bundle.main.loadNibNamed("AccountCreateFooterView", owner: self, options: nil)?.first as? AccountCreateFooterView {
                createView.configure(hasAccounts: viewModel.accountSelectSubCellModels.count > 0,
                                     onTap: { [weak self] in
                    self?.onCreate(asset: assetInfo)
                })
                return createView
            }
        }
        return nil
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        let cnt = viewModel.assetSelectCellModelsFilter.count
        if cnt == section && viewModel.enableAnyAsset {
            if let accountView = Bundle.main.loadNibNamed("AnyAssetExpandableView", owner: self, options: nil)?.first as? AnyAssetExpandableView {
                accountView.configure(open: viewModel.selectedSection == section,
                                      hasAccounts: viewModel.accountSelectSubCellModels.count > 0,
                                      onCreate: {[weak self] in
                    self?.onCreate(asset: nil)
                })

                let handler = UIButton(frame: accountView.tapView.frame)
                handler.tag = section
                handler.borderColor = .red
                handler.addTarget(self,
                                        action: #selector(hideSection(sender:)),
                                        for: .touchUpInside)
                accountView.addSubview(handler)
                return accountView
            }
        }
        if let accountView = Bundle.main.loadNibNamed("AssetExpandableView", owner: self, options: nil)?.first as? AssetExpandableView {
            let cellModel = viewModel.assetSelectCellModelsFilter[section]
            accountView.configure(model: cellModel,
                                  hasAccounts: viewModel.accountSelectSubCellModels.count > 0,
                                  open: viewModel.selectedSection == section,
                                  onCreate: {[weak self] in
                self?.onCreate(asset: cellModel.asset)
            })

            let handler = UIButton(frame: accountView.tapView.frame)
            handler.tag = section
            handler.borderColor = .red
            handler.addTarget(self,
                                    action: #selector(hideSection(sender:)),
                                    for: .touchUpInside)
            accountView.addSubview(handler)
            return accountView
        }
        return nil
    }

    @objc
    private func hideSection(sender: UIButton) {
        let section = sender.tag
        if viewModel.selectedSection == section {
            viewModel.selectedSection = -1
            tableView.reloadSections(IndexSet([section]), with: .fade)
        } else {
            if viewModel.selectedSection != -1 {
                let old = viewModel.selectedSection
                viewModel.selectedSection = -1
                tableView.reloadSections(IndexSet([old]), with: .fade)

                viewModel.selectedSection = section
                let cnt = viewModel.assetSelectCellModelsFilter.count
                if cnt == section && viewModel.enableAnyAsset {
                    viewModel.loadAccountsForAsset(nil)
                } else {
                    if let asset = viewModel.assetSelectCellModelsFilter[section].asset?.assetId {
                        viewModel.loadAccountsForAsset(asset)
                    }
                }
                tableView.reloadSections(IndexSet([section]), with: .fade)
            } else {
                viewModel.selectedSection = section
                let cnt = viewModel.assetSelectCellModelsFilter.count
                if cnt == section && viewModel.enableAnyAsset {
                    viewModel.loadAccountsForAsset(nil)
                } else {
                    if let asset = viewModel.assetSelectCellModelsFilter[section].asset?.assetId {
                        viewModel.loadAccountsForAsset(asset)
                    }
                }
                tableView.reloadSections(IndexSet([section]), with: .fade)
            }
        }
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.2) {
            if self.viewModel.accountSelectSubCellModels.count > 0 && self.viewModel.selectedSection > 0 {
                self.tableView?.scrollToRow(at: IndexPath(row: 0, section: section), at: .middle, animated: true)
            }
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        var assetId = AssetInfo.lbtcId
        let cnt = viewModel.assetSelectCellModelsFilter.count
        if !(cnt == indexPath.section && viewModel.enableAnyAsset) {
            if let asset = viewModel.assetSelectCellModelsFilter[indexPath.section].asset {
                assetId = asset.assetId
            }
        }
        let account = viewModel.accountSelectSubCellModels[indexPath.row].account
        AnalyticsManager.shared.selectAccount(account: AccountsManager.shared.current, walletType: account.type)
        delegate?.didSelectReceiver(assetId: assetId, account: account)
        navigationController?.popViewController(animated: true)
    }
}

extension AssetExpandableSelectViewController {

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

extension AssetExpandableSelectViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}

extension AssetExpandableSelectViewController: SecuritySelectViewControllerDelegate {
    func didCreatedWallet(_ wallet: WalletItem) {

        AnalyticsManager.shared.createAccount(account: AccountsManager.shared.current, walletType: wallet.type)
        delegate?.didSelectReceiver(assetId: getAssetId(), account: wallet)
        navigationController?.popViewController(animated: true)
    }

    func getAssetId() -> String {
        let defaultAssetId = viewModel.wm.testnet ? AssetInfo.ltestId : AssetInfo.lbtcId
        let cnt = viewModel.assetSelectCellModelsFilter.count
        if cnt == 0 && viewModel.selectedSection == 0 && viewModel.enableAnyAsset {
            return defaultAssetId
        } else {
            if let asset = viewModel.assetSelectCellModelsFilter[safe: viewModel.selectedSection]?.asset?.assetId {
                return asset
            }
        }
        return defaultAssetId
    }
}
