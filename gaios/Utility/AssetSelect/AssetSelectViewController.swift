import UIKit

protocol AssetSelectViewControllerDelegate: AnyObject {
    func didSelectAsset(_ assetId: String)
}

class AssetSelectViewController: UIViewController {

    @IBOutlet weak var searchCard: UIView!
    @IBOutlet weak var btnSearch: UIButton!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var searchField: UITextField!

    var viewModel: AssetSelectViewModel!
    weak var delegateAsset: AssetSelectViewControllerDelegate?
    weak var delegateAccount: AccountSelectViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        ["AssetSelectCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        searchField.delegate = self
        title = "Choose Asset"
        setContent()
        setStyle()

        viewModel.reload = tableView.reloadData
        viewModel.loadAssets()
    }

    func setContent() {
        searchField.attributedPlaceholder = NSAttributedString(string: "Search Asset", attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.4)])
    }

    func setStyle() {
        searchCard.cornerRadius = 5.0
    }

    @objc func triggerTextChange() {
        viewModel?.search(searchField.text ?? "")
        tableView.reloadData()
    }

    @IBAction func onEditingChange(_ sender: Any) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }
}

extension AssetSelectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel?.assetSelectCellModelsFilter.count ?? 0
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: AssetSelectCell.identifier, for: indexPath) as? AssetSelectCell {
            let model = viewModel.assetSelectCellModelsFilter[indexPath.row]
            cell.configure(model: model)
            cell.selectionStyle = .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
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

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        guard let assetCellModel = viewModel?.assetSelectCellModelsFilter[indexPath.row] as? AssetSelectCellModel else { return }
        let asset = assetCellModel.asset?.assetId
        delegateAsset?.didSelectAsset(asset ?? "")
        if (self.navigationController?.viewControllers ?? [])
            .contains(where: {
            return $0 is SecuritySelectViewController
        }) {
            navigationController?.popViewController(animated: true)
        } else {
            let storyboard = UIStoryboard(name: "Utility", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AccountSelectViewController") as? AccountSelectViewController {
                var accounts = [WalletItem]() //viewModel.accounts
                if let asset = asset, asset == "btc" {
                    accounts.removeAll(where: { $0.gdkNetwork.liquid })
                } else {
                    accounts.removeAll(where: { !$0.gdkNetwork.liquid })
                }
                ///ampWarn: can we remove?
                vc.viewModel = AccountSelectViewModel(accounts: accounts, ampWarn: assetCellModel.ampWarn)
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }
}

extension AssetSelectViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}

extension AssetSelectViewController: AccountSelectViewControllerDelegate {
    func didSelectAccount(_ account: WalletItem) {
        delegateAccount?.didSelectAccount(account)
    }
}
