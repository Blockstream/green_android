import Foundation
import UIKit

protocol DrawerNetworkSelectionDelegate: class {
    func didSelectAccount(account: Account)
    func didSelectHDW()
}

class DrawerNetworkSelectionViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    var onSelection: ((Account) -> Void)?

    private var selectedAccount =  AccountsManager.shared.current
    private var accounts = AccountsManager.shared.list
    weak var delegate: DrawerNetworkSelectionDelegate?

    var headerH: CGFloat = 44.0

    override func viewDidLoad() {
        super.viewDidLoad()
    }

//    override func viewDidLayoutSubviews() {
//        super.viewDidLayoutSubviews()
//        view.roundCorners([.topRight, .topLeft], radius: 12)
//        guard let footerView = tableView.tableFooterView else { return }
//        let height = footerView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).height
//        if height != footerView.frame.size.height {
//            footerView.frame.size.height = height
//            tableView.tableFooterView = footerView
//        }
//    }

    @objc func dismissModal() {
        self.dismiss(animated: true, completion: nil)
    }

    @objc func saveAndDismiss() {
       self.dismiss(animated: true, completion: nil)
    }
}

extension DrawerNetworkSelectionViewController: UITableViewDataSource, UITableViewDelegate {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 2
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        accounts.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case 0:
            let account = accounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletDrawerCell") as? WalletDrawerCell {
                cell.configure(account, account.id == selectedAccount?.id)
                cell.selectionStyle = .none
                return cell
            }
        case 1:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletDrawerHDCell") as? WalletDrawerHDCell {
                let hw = SupportedHW.allCases[indexPath.row]
                cell.configure(hw.name(), hw.icon())
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return headerH
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch section {
        case 0:
            return headerView(NSLocalizedString("id_all_wallets", comment: "").uppercased())
        case 1:
            return headerView(NSLocalizedString("id_devices", comment: "").uppercased())
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        switch indexPath.section {
        case 0:
            let account = accounts[indexPath.row]
            if selectedAccount?.id != account.id {
                self.delegate?.didSelectAccount(account: account)
            }
        case 1:
            self.delegate?.didSelectHDW()
        default:
            break
        }
        self.dismiss(animated: true, completion: nil)
    }
}

extension DrawerNetworkSelectionViewController {
    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 14.0, weight: .semibold)
        title.text = txt
        title.textColor = UIColor.customGrayLight()
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        return section
    }

}
