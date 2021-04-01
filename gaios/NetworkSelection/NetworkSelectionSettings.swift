import Foundation
import UIKit

class NetworkSelectionSettings: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var cancelButtonHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var swipeIndicator: UIView!
    @IBOutlet weak var dismissButton: UIButton!
    @IBOutlet weak var dismissButtonHeightConstraint: NSLayoutConstraint!

    var onSelection: ((Account) -> Void)?
    var isLanding = false

    private var selectedAccount =  AccountsManager.shared.current
    private var accounts = AccountsManager.shared.list

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.separatorStyle = .none
        tableView.estimatedRowHeight = 85
        tableView.rowHeight = UITableView.automaticDimension
        titleLabel.text = NSLocalizedString("id_choose_your_network", comment: "")
        configureHeader()
        let downSwipe = UISwipeGestureRecognizer(target: self, action: #selector(dismissModal))
        downSwipe.direction = .down
        view.addGestureRecognizer(downSwipe)
        configureCancelButton()
    }

    func configureHeader() {
        swipeIndicator.isHidden = isLanding
        dismissButton.isHidden = !isLanding
        dismissButtonHeightConstraint.constant = isLanding ? 18 : 0
        dismissButton.addTarget(self, action: #selector(dismissModal), for: .touchUpInside)
    }

    func configureCancelButton() {
        cancelButton.isHidden = isLanding
        cancelButtonHeightConstraint.constant = isLanding ? 0 : 42
        cancelButton.setTitle(NSLocalizedString("id_cancel", comment: ""), for: .normal)
        cancelButton.addTarget(self, action: #selector(dismissModal), for: .touchUpInside)
        self.view.layoutIfNeeded()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        view.roundCorners([.topRight, .topLeft], radius: 12)
        guard let footerView = tableView.tableFooterView else { return }
        let height = footerView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).height
        if height != footerView.frame.size.height {
            footerView.frame.size.height = height
            tableView.tableFooterView = footerView
        }
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        view.endEditing(true)
        return true
    }

    @objc func changeProxy(_ sender: UISwitch) {
        guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
        content.socks5Hostname.isEnabled = content.proxySwitch.isOn
        content.socks5Port.isEnabled = content.proxySwitch.isOn
    }

    @objc func dismissModal() {
        self.dismiss(animated: true, completion: nil)
    }

    @objc func saveAndDismiss() {
       self.dismiss(animated: true, completion: nil)
    }
}

extension NetworkSelectionSettings: UITableViewDataSource, UITableViewDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return accounts.count
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        return 84
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "networkSelectionCell",
                                                       for: indexPath as IndexPath) as? NetworkSelectionTableCell else { return UITableViewCell() }
        let account = accounts[indexPath.row]
        cell.configure(with: account, selected: account.id == selectedAccount?.id)
        cell.setNeedsLayout()
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let account = accounts[indexPath.row]
        if selectedAccount?.id != account.id {
            if let onSelection = onSelection {
                onSelection(account)
            }
            self.dismiss(animated: true, completion: nil)
        }
    }
}
