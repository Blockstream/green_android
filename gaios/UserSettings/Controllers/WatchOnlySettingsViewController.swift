import Foundation
import UIKit

class WatchOnlySettingsViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    var headerH: CGFloat = 54.0
    var session: SessionManager!
    private var viewModel = WatchOnlySettingsViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "id_watchonly".localized
        view.accessibilityIdentifier = AccessibilityIdentifiers.SettingsScreen.view

        AnalyticsManager.shared.recordView(.walletSettings, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))

        initViewModel()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        viewModel.load()
    }

    func initViewModel() {
        viewModel.reloadTableView = { [weak self] in
            DispatchQueue.main.async {
                self?.tableView.reloadData()
            }
        }
        viewModel.error = { [weak self] text in
            DispatchQueue.main.async {
                self?.showError(text)
            }
        }
    }

    @objc func close() {
        dismiss(animated: true, completion: nil)
    }
}

extension WatchOnlySettingsViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return viewModel.sections.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.getCellModelsForSection(at: section)?.count ?? 0
    }

    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return viewModel.sections[section].rawValue
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return headerView(NSLocalizedString(viewModel.sections[section].rawValue, comment: ""))
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let vm = viewModel.getCellModel(at: indexPath)
        let section = viewModel.sections[indexPath.section]
        if section == .Multisig {
            if let cell = tableView.dequeueReusableCell(withIdentifier: WatchOnlyMultisigSettingsCell.identifier) as? WatchOnlyMultisigSettingsCell {
                cell.viewModel = vm
                cell.selectionStyle = .none
                return cell
            }
        } else if vm?.network != nil {
            if let cell = tableView.dequeueReusableCell(withIdentifier: WatchOnlySinglesigSettingsCell.identifier) as? WatchOnlySinglesigSettingsCell {
                cell.viewModel = vm
                cell.selectionStyle = .none
                return cell
            }
        } else {
            if let cell = tableView.dequeueReusableCell(withIdentifier: WatchOnlyHeaderSettingsCell.identifier) as? WatchOnlyHeaderSettingsCell {
                cell.viewModel = vm
                cell.selectionStyle = .none
                return cell
            }
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        switch viewModel.sections[indexPath.section] {
        case .Multisig:
            if let item = viewModel.getCellModel(at: indexPath),
            let wm = WalletManager.current,
            let network = item.network,
            let session = wm.sessions[network] {
                openWatchOnly(session: session)
            }
        case .Singlesig:
            break
        }
    }

    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 20.0, weight: .heavy)
        title.text = txt
        title.textColor = .white
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: -10),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        return section
    }
}

extension WatchOnlySettingsViewController: DialogWatchOnlySetUpViewControllerDelegate {

    func openWatchOnly(session: SessionManager) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWatchOnlySetUpViewController") as? DialogWatchOnlySetUpViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.session = session
            present(vc, animated: false, completion: nil)
        }
    }

    func watchOnlyDidUpdate(_ action: WatchOnlySetUpAction) {
            switch action {
            case .save, .delete:
                viewModel.load()
            default:
                break
            }
    }
}
