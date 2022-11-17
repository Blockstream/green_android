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

    var viewModel = AssetExpandableSelectViewModel()
    weak var delegate: AssetExpandableSelectViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        ["AccountSelectSubCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        searchField.delegate = self
        setContent()
        setStyle()

        viewModel.loadAssets()
        tableView.reloadData()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

    }

    func setContent() {
        title = "Select asset"
        searchField.attributedPlaceholder = NSAttributedString(string: "Search Asset", attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.4)])
    }

    func setStyle() {
        searchCard.cornerRadius = 5.0
    }

    @objc func triggerTextChange() {
        viewModel.search(searchField.text ?? "")
        tableView.reloadData()
    }

    @IBAction func onEditingChange(_ sender: Any) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }
}

extension AssetExpandableSelectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return viewModel.assetSelectCellModelsFilter.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if viewModel.selectedSection != section {
            return 0
        }
        /// need value for specific asset
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
        return 0.1
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        if let accountView = Bundle.main.loadNibNamed("AccountExpandableView", owner: self, options: nil)?.first as? AccountExpandableView {
            accountView.configure(model: viewModel.assetSelectCellModelsFilter[section], open: viewModel.selectedSection == section)

            let handler = UIButton(frame: accountView.frame)
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
        } else {
            viewModel.selectedSection = -1
            tableView.reloadData()
            viewModel.selectedSection = section
            if let asset = viewModel.assetSelectCellModelsFilter[section].asset?.assetId {
                viewModel.loadAccountsForAsset(asset)
            }
        }
        tableView.reloadSections(IndexSet([section]), with: .automatic)
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let assetId = viewModel.assetSelectCellModelsFilter[indexPath.section].asset?.assetId {
            viewModel.loadAccountsForAsset(assetId)
        }
        //delegate?.didSelectReceiver(assetId: assetId, account: viewModel.accounts[indexPath.row])
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
