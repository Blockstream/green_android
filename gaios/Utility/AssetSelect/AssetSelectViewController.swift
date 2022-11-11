import UIKit

protocol AssetSelectViewControllerDelegate: AnyObject {
    func didSelectAssetAt(_ index: Int)
}

class AssetSelectViewController: UIViewController {

    @IBOutlet weak var searchCard: UIView!
    @IBOutlet weak var btnSearch: UIButton!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var searchField: UITextField!

    var viewModel: AssetSelectViewModel?
    weak var delegate: AssetSelectViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        ["AssetSelectCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        searchField.delegate = self
        title = "Choose Asset"
        setContent()
        setStyle()
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
        if let cell = tableView.dequeueReusableCell(withIdentifier: AssetSelectCell.identifier, for: indexPath) as? AssetSelectCell,
           let model = viewModel?.assetSelectCellModelsFilter[indexPath.row] {
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
        guard let index = viewModel?.getAssetIndex(indexPath.row) else { return }

        if (self.navigationController?.viewControllers ?? [])
            .contains(where: {
            return $0 is SecuritySelectViewController
        }) {
            delegate?.didSelectAssetAt(index)
            navigationController?.popViewController(animated: true)
        } else {
            let storyboard = UIStoryboard(name: "Utility", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AccountSelectViewController") as? AccountSelectViewController {
                guard let viewModel = viewModel else { return }
                vc.viewModel = AccountSelectViewModel(accounts: viewModel.accounts, cachedBalance: viewModel.cachedBalance)
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
