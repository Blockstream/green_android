import Foundation
import UIKit
import PromiseKit

enum SecurityOption: String {
    case single = "SingleSig"
    case multi = "MultiSig"
}

class WatchOnlyViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var rememberSwitch: UISwitch!
    @IBOutlet weak var rememberTitle: UILabel!
    @IBOutlet weak var loginButton: UIButton!
    @IBOutlet weak var warningLabel: UILabel!
    @IBOutlet weak var cardTestnet: UIView!
    @IBOutlet weak var lblTestnet: UILabel!
    @IBOutlet weak var testnetSwitch: UISwitch!
    @IBOutlet weak var btnSettings: UIButton!

    private var buttonConstraint: NSLayoutConstraint?
    private var progressToken: NSObjectProtocol?
    private var networks = [NetworkSecurityCase]()

    var network: AvailableNetworks?
    var watchOnlySecurityOption: SecurityOption = .multi

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = NSLocalizedString("id_login", comment: "")
        lblHint.text = NSLocalizedString("id_log_in_via_watchonly_to_receive", comment: "")
        rememberTitle.text = NSLocalizedString("id_remember_me", comment: "")
        warningLabel.text = NSLocalizedString("id_watchonly_mode_can_be_activated", comment: "")
        lblTestnet.text = "Testnet"
        rememberSwitch.addTarget(self, action: #selector(rememberSwitchChange), for: .valueChanged)
        testnetSwitch.addTarget(self, action: #selector(testnetSwitchChange), for: .valueChanged)
        loginButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        loginButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        loginButton.setStyle(.primary)
        usernameTextField.attributedPlaceholder = NSAttributedString(
            string: NSLocalizedString("id_username", comment: ""),
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        passwordTextField.attributedPlaceholder = NSAttributedString(
            string: NSLocalizedString("id_password", comment: ""),
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)

        usernameTextField.setLeftPaddingPoints(10.0)
        usernameTextField.setRightPaddingPoints(10.0)
        passwordTextField.setLeftPaddingPoints(10.0)
        passwordTextField.setRightPaddingPoints(10.0)

        usernameTextField.leftViewMode = .always
        passwordTextField.leftViewMode = .always

        view.accessibilityIdentifier = AccessibilityIdentifiers.WatchOnlyScreen.view
        usernameTextField.accessibilityIdentifier = AccessibilityIdentifiers.WatchOnlyScreen.usernameField
        passwordTextField.accessibilityIdentifier = AccessibilityIdentifiers.WatchOnlyScreen.passwordField
        testnetSwitch.accessibilityIdentifier = AccessibilityIdentifiers.WatchOnlyScreen.testnetSwitch
        loginButton.accessibilityIdentifier = AccessibilityIdentifiers.WatchOnlyScreen.loginBtn

        cardTestnet.isHidden = true

        AnalyticsManager.shared.recordView(.onBoardWatchOnlyCredentials)
    }

    @objc func rememberSwitchChange(_ sender: UISwitch) {
        if sender.isOn {
            let alert = UIAlertController(title: NSLocalizedString("id_warning_watchonly_credentials", comment: ""), message: NSLocalizedString("id_your_watchonly_username_and", comment: ""), preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in
                sender.isOn = false
            })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in
                sender.isOn = true
            })
            DispatchQueue.main.async {
                self.present(alert, animated: true, completion: nil)
            }
        }
    }

    @objc func testnetSwitchChange(_ sender: UISwitch) {
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        progressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil, queue: .main, using: progress)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = progressToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    @objc func progress(_ notification: Notification) {
        if let json = try? JSONSerialization.data(withJSONObject: notification.userInfo!, options: []),
           let tor = try? JSONDecoder().decode(TorNotification.self, from: json) {
            var text = NSLocalizedString("id_tor_status", comment: "") + " \(tor.progress)%"
            if tor.progress == 100 {
                text = NSLocalizedString("id_logging_in", comment: "")
            }
            DispatchQueue.main.async {
                self.startLoader(message: text)
            }
        }
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.loginButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height)
            self.buttonConstraint?.isActive = true
            if UIScreen.main.nativeBounds.height <= 1334 {
                self.lblHint.isHidden = true
            }
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            if UIScreen.main.nativeBounds.height <= 1334 {
                self.lblHint.isHidden = false
            }
        })
    }

    func selectNetwork() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            let testnet = LandingViewController.chainType == .testnet
            networks = testnet ? [.testnetMS, .testnetLiquidMS] : [.bitcoinMS, .liquidMS]
            let cells = networks.map { DialogListCellModel(type: .list,
                                                           icon: nil,
                                                           title: $0.chain) }
            vc.viewModel = DialogListViewModel(title: "Select Network", items: cells)
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @objc func click(_ sender: Any) {
        selectNetwork()
    }

    func login(for network: GdkNetwork) {
        let username = self.usernameTextField.text ?? ""
        let password = self.passwordTextField.text ?? ""
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!

        let name = AccountsManager.shared.getUniqueAccountName(
            testnet: !network.mainnet,
            watchonly: true)

        var account = Account(name: name, network: network.network, username: username, isSingleSig: network.electrum)
        if self.rememberSwitch.isOn {
            account.password = password
        }
        firstly {
            dismissKeyboard()
            self.startLoader(message: NSLocalizedString("id_logging_in", comment: ""))
            return Guarantee()
        }.compactMap {
            WalletManager.getOrAdd(for: account)
        }.then(on: bgq) {
            $0.loginWatchOnly(username, password)
        }.ensure {
            self.stopLoader()
        }.done { _ in
            AccountsManager.shared.current = account
            AnalyticsManager.shared.loginWallet(loginType: .watchOnly, ephemeralBip39: false, account: AccountsManager.shared.current)
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            var prettyError: String?
            switch error {
            case LoginError.connectionFailed:
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
                prettyError = "id_connection_failed"
            default:
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
                prettyError = "id_login_failed"
            }
            AnalyticsManager.shared.failedWalletLogin(account: AccountsManager.shared.current, error: error, prettyError: prettyError)
        }
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            vc.delegate = self
            present(vc, animated: true) {}
        }
    }
}

extension WatchOnlyViewController: WalletSettingsViewControllerDelegate {
    func didSet(tor: Bool) {
        //
    }
    func didSet(testnet: Bool) {
        // cardTestnet.isHidden = !testnet
    }
}

extension WatchOnlyViewController: DialogListViewControllerDelegate {
    func didSelectRowAtIndex(_ index: Int) {
        login(for: getGdkNetwork(networks[index].rawValue))
    }
}
