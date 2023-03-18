import Foundation
import UIKit

protocol DrawerNetworkSelectionDelegate: AnyObject {
    func didSelectAccount(account: Account)
    func didSelectAddWallet()
    func didSelectSettings()
    func didSelectAbout()
}

class DrawerNetworkSelectionViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnAbout: UIButton!
    @IBOutlet weak var btnSettings: UIButton!

    var onSelection: ((Account) -> Void)?
    weak var delegate: DrawerNetworkSelectionDelegate?

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    private var ephAccounts: [Account] {
        AccountsRepository.shared.ephAccounts.filter { account in
            account.isEphemeral && !WalletsRepository.shared.wallets.filter {$0.key == account.id }.isEmpty
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()

        tableView.register(UINib(nibName: "WalletListCell", bundle: nil), forCellReuseIdentifier: "WalletListCell")
        tableView.register(UINib(nibName: "WalletListHDCell", bundle: nil), forCellReuseIdentifier: "WalletListHDCell")

        view.accessibilityIdentifier = AccessibilityIdentifiers.DrawerMenuScreen.view
    }

    func setContent() {
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
        btnSettings.setTitleColor(.lightGray, for: .normal)
        btnAbout.setTitle(NSLocalizedString("id_about", comment: ""), for: .normal)
        btnAbout.setImage(UIImage(named: "ic_about")!, for: .normal)
        btnAbout.setTitleColor(.lightGray, for: .normal)
    }

    @objc func didPressAddWallet() {
        delegate?.didSelectAddWallet()
    }

    @IBAction func btnAbout(_ sender: Any) {
        delegate?.didSelectAbout()
    }

    @IBAction func btnSettings(_ sender: Any) {
        delegate?.didSelectSettings()
    }
}

extension DrawerNetworkSelectionViewController: UITableViewDataSource, UITableViewDelegate {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 4
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0:
            return AccountsRepository.shared.swAccounts.count
        case 1:
            return ephAccounts.count
        case 2:
            return AccountsRepository.shared.hwAccounts.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case 0:
            let account = AccountsRepository.shared.swAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListCell") as? WalletListCell {
                let selected = { () -> Bool in
                    return WalletsRepository.shared.get(for: account.id)?.activeSessions.count ?? 0 > 0
                }
                cell.configure(item: account, isSelected: selected())
                cell.selectionStyle = .none
                return cell
            }
        case 1: /// EPHEMERAL
            let account = ephAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListCell") as? WalletListCell {
                let selected = { () -> Bool in
                    return WalletsRepository.shared.get(for: account.id)?.activeSessions.count ?? 0 > 0
                }
                cell.configure(item: account, isSelected: selected() /* , isEphemeral: true */ )
                cell.selectionStyle = .none
                return cell
            }
        case 2:
            let account = AccountsRepository.shared.hwAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListCell") as? WalletListCell {
                let selected = { () -> Bool in
                    return WalletsRepository.shared.get(for: account.id)?.activeSessions.count ?? 0 > 0
                }
                cell.configure(item: account, isSelected: selected())
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        if section == 0 && AccountsRepository.shared.swAccounts.isEmpty {
            return 0.1
        }
        if section == 1 && ephAccounts.isEmpty {
            return 0.1
        }
        if section == 2 && AccountsRepository.shared.hwAccounts.isEmpty {
            return 0.1
        }
        return headerH
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch section {
        case 0:
            if AccountsRepository.shared.swAccounts.isEmpty {
                return nil
            }
            return headerView(NSLocalizedString("id_digital_wallets", comment: ""))
        case 1:
            if ephAccounts.isEmpty {
                return nil
            }
            return headerView(NSLocalizedString("id_ephemeral_wallets", comment: ""))
        case 2:
            if AccountsRepository.shared.hwAccounts.isEmpty {
                return nil
            }
            return headerView(NSLocalizedString("id_hardware_wallets", comment: ""))
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        switch indexPath.section {
        case 0:
            let account = AccountsRepository.shared.swAccounts[indexPath.row]
            self.delegate?.didSelectAccount(account: account)
        case 1:
            let account = ephAccounts[indexPath.row]
            self.delegate?.didSelectAccount(account: account)
        case 2:
            let account = AccountsRepository.shared.hwAccounts[indexPath.row]
            self.delegate?.didSelectAccount(account: account)
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
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20)
        ])

        return section
    }
}
