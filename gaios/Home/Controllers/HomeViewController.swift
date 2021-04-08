import UIKit

enum SupportedHW: String, CaseIterable {
    case Jade = "Blockstream Jade"
    case LedgerNanoX = "Ledger Nano X"

    func name() -> String {
        switch self {
        case .Jade:
            return "Blockstream Jade"
        case .LedgerNanoX:
            return "Ledger"
        }
    }

    func icon() -> UIImage {
        switch self {
        case .Jade:
            return UIImage(named: "blockstreamIcon")!
        case .LedgerNanoX:
            return UIImage(named: "ledgerIcon")!
        }
    }
}

class HomeViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var lblVersion: UILabel!

    var accounts: [Account] { get { AccountsManager.shared.list } }

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        updateUI()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tableView.reloadData()
    }

    func setContent() {
        lblVersion.text = String(format: NSLocalizedString("id_version_1s", comment: ""), "\(Bundle.main.versionNumber)")
    }

    func setStyle() {
    }

    func updateUI() {
    }

    func enterWallet(_ index: Int) {
        // watch only wallet
        let account = accounts[index]
        if account.isWatchonly {
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "WatchOnlyLoginViewController") as? WatchOnlyLoginViewController
            vc?.account = accounts[index]
            navigationController?.pushViewController(vc!, animated: true)
        } else {
            let storyboard = UIStoryboard(name: "Home", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "LoginViewController") as? LoginViewController
            vc?.account = accounts[index]
            navigationController?.pushViewController(vc!, animated: true)
        }
    }

    func showHardwareWallet() {
        let storyboard = UIStoryboard(name: "HardwareWallet", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "HardwareWalletScanViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

    @objc func didPressAddWallet() {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "LandingViewController")
        navigationController?.pushViewController(vc, animated: true)
//        let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
//        let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryInstructionViewController")
//        navigationController?.pushViewController(vc, animated: true)
    }

}

extension HomeViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 2
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case 0:
            return accounts.count == 0 ? 1 : accounts.count
        case 1:
            return SupportedHW.allCases.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case 0:
            if accounts.count == 0 {
                if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletEmptyCell") as? WalletEmptyCell {
                    cell.configure(NSLocalizedString("id_it_looks_like_you_have_no", comment: ""), UIImage(named: "ic_logo_green")!)
                    cell.selectionStyle = .none
                    return cell
                }
            } else {
                if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletCell") as? WalletCell {
                    cell.configure(accounts[indexPath.row])
                    cell.selectionStyle = .none
                    return cell
                }
            }
        case 1:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletHDCell") as? WalletHDCell {
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
            if accounts.count > 0 {
                enterWallet(indexPath.row)
            }
        case 1:
            showHardwareWallet()
        default:
            break
        }
    }
}

extension HomeViewController {
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
        section.backgroundColor = .clear

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

        return section
    }
}
