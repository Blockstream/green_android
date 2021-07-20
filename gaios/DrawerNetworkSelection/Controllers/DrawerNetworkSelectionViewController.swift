import Foundation
import UIKit

protocol DrawerNetworkSelectionDelegate: AnyObject {
    func didSelectAccount(account: Account)
    func didSelectHW(account: Account)
    func didSelectAddWallet()
}

class DrawerNetworkSelectionViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var lblVersion: UILabel!

    var onSelection: ((Account) -> Void)?

    private var selectedAccount =  AccountsManager.shared.current
    private let swAccounts = AccountsManager.shared.swAccounts
    private let hwAccounts = AccountsManager.shared.hwAccounts
    weak var delegate: DrawerNetworkSelectionDelegate?

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()

        view.accessibilityIdentifier = AccessibilityIdentifiers.DrawerMenuScreen.view
    }

    func setContent() {
        lblVersion.text = String(format: NSLocalizedString("id_version_1s", comment: ""), "\(Bundle.main.versionNumber)")
    }

    @objc func dismissModal() {
        self.dismiss(animated: true, completion: nil)
    }

    @objc func saveAndDismiss() {
       self.dismiss(animated: true, completion: nil)
    }

    @objc func didPressAddWallet() {
        delegate?.didSelectAddWallet()
    }
}

extension DrawerNetworkSelectionViewController: UITableViewDataSource, UITableViewDelegate {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 2
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0:
            return swAccounts.count
        case 1:
            return hwAccounts.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case 0:
            let account = swAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletDrawerCell") as? WalletDrawerCell {
                cell.configure(account, account.id == selectedAccount?.id)
                cell.selectionStyle = .none
                return cell
            }
        case 1:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletDrawerHDCell") as? WalletDrawerHDCell {
                let hw = hwAccounts[indexPath.row]
                cell.configure(hw.name, hw.icon)
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
        switch section {
        case 0:
            return footerH
        default:
            return 0
        }
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
        switch section {
        case 0:
            return footerView(NSLocalizedString("id_add_wallet", comment: ""))
        case 1:
            return nil
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        switch indexPath.section {
        case 0:
            let account = swAccounts[indexPath.row]
            if selectedAccount?.id != account.id {
                self.delegate?.didSelectAccount(account: account)
            }
        case 1:
            let account = hwAccounts[indexPath.row]
            self.delegate?.didSelectHW(account: account)
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

    func footerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerH))
        section.backgroundColor = UIColor.customTitaniumDark()

        let icon = UIImageView(frame: .zero)
        icon.image = UIImage(named: "ic_plus")?.maskWithColor(color: .white)
        icon.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(icon)

        let title = UILabel(frame: .zero)
        title.text = txt
        title.textColor = .white
        title.font = .systemFont(ofSize: 17.0, weight: .semibold)
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            icon.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            icon.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 16),
            icon.widthAnchor.constraint(equalToConstant: 40.0),
            icon.heightAnchor.constraint(equalToConstant: 40.0)
        ])

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: (40 + 16 * 2)),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(didPressAddWallet))
        section.addGestureRecognizer(tapGesture)
        section.accessibilityIdentifier = AccessibilityIdentifiers.DrawerMenuScreen.addWalletView
        return section
    }
}
