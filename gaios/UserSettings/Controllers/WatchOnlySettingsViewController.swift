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

        AnalyticsManager.shared.recordView(.walletSettings, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        Task {
            await viewModel.load()
            await MainActor.run {  tableView.reloadData() }
        }
    }

    @objc func close() {
        dismiss(animated: true, completion: nil)
    }

    func showQR(_ item: QRDialogInfo) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogQRViewController") as? DialogQRViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.qrDialogInfo = item
            present(vc, animated: false, completion: nil)
        }
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
        return headerView(
            txt: NSLocalizedString(viewModel.sections[section].rawValue, comment: ""),
            img: viewModel.sections[section].icon
        )
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
                cell.configure(viewModel: vm, onCopy: { item in
                    if !item.isEmpty {
                        UIPasteboard.general.string = item
                        DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 2.0)
                        UINotificationFeedbackGenerator().notificationOccurred(.success)
                    }
                }, onQR: {[weak self] item in
                    self?.showQR(item)
                })
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

    func headerView(txt: String, img: UIImage) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.gBlackBg()

        let icon = UIImageView(frame: .zero)
        icon.image = img.maskWithColor(color: .white.withAlphaComponent(0.6))
        icon.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(icon)

        NSLayoutConstraint.activate([
            icon.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            icon.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 16),
            icon.widthAnchor.constraint(equalToConstant: 20.0),
            icon.heightAnchor.constraint(equalToConstant: 20.0)
        ])

        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 16.0, weight: .bold)
        title.text = txt
        title.textColor = .white.withAlphaComponent(0.6)
        title.numberOfLines = 0
        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24 + 20),
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
                Task {
                    await viewModel.load()
                    await MainActor.run { tableView.reloadData() }
                }
            default:
                break
            }
    }
}
