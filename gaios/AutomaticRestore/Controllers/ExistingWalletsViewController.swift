import UIKit
import PromiseKit
import simd

enum FailingState {
    case notFound
    case isJustRestored
    case disconnect
    case invalid
}

enum ExistingWalletSection: Int, CaseIterable {
    case remoteAlerts = 0
    case wallet = 1
}

struct ExistingWallet {
    let isSingleSig: Bool
    let failure: FailingState?
}

class ExistingWalletsViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnManualRestore: UIButton!
    @IBOutlet weak var lblLoading: UILabel!

    var wallets: [ExistingWallet] = []

    var mnemonic = ""
    var mnemonicPassword = ""

    private var remoteAlert: RemoteAlert?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()

        btnManualRestore.isHidden = true
        tableView.isHidden = true
        lblLoading.isHidden = false

        view.accessibilityIdentifier = AccessibilityIdentifiers.ExistingWalletsScreen.view
        btnManualRestore.accessibilityIdentifier = AccessibilityIdentifiers.ExistingWalletsScreen.manualRestoreBtn

        checkWallets()

        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")

        self.remoteAlert = RemoteAlertManager.shared.getAlert(screen: .onBoardScan, network: AccountsManager.shared.current?.network)

        AnalyticsManager.shared.recordView(.onBoardScan, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
    }

    func remoteAlertDismiss() {
        remoteAlert = nil
        reloadSections([ExistingWalletSection.remoteAlerts], animated: true)
    }

    func reloadSections(_ sections: [ExistingWalletSection], animated: Bool) {
        DispatchQueue.main.async {
            if animated {
                self.tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            } else {
                UIView.performWithoutAnimation {
                    self.tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
                }
            }
        }
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_existing_wallets", comment: "")
        lblHint.text = NSLocalizedString("id_any_wallet_found_will_be", comment: "")
        btnManualRestore.setTitle(NSLocalizedString("id_manual_restore", comment: ""), for: .normal)
        lblLoading.text = NSLocalizedString("id_looking_for_wallets", comment: "")
    }

    func checkWallets() {
        self.wallets = []
        firstly {
            startLoader(message: NSLocalizedString("id_looking_for_wallets", comment: ""))
            return Guarantee()
        }.then {
            self.checkWallet(isSinglesig: true)
        }.map { wallet in
            self.wallets += [wallet]
        }.then {
            self.checkWallet(isSinglesig: false)
        }.map { wallet in
            self.wallets += [wallet]
        }.ensure {
            self.tableView.reloadData {
                self.stopLoader()
                self.lblLoading.isHidden = true
                self.tableView.isHidden = false
                self.btnManualRestore.isHidden = self.wallets.filter { $0.failure != .notFound }.count > 0 ? false : true
            }
        }.catch { error in
            print(error)
        }
    }

    func checkWallet(isSinglesig: Bool) -> Promise<ExistingWallet> {
        OnBoardManager.shared.params?.singleSig = isSinglesig
        let params = OnBoardManager.shared.params
        let session = SessionManager(OnBoardManager.shared.gdkNetwork)
        return Promise { seal in
            session.discover(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword)
            .done { _ in
                seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, failure: nil))
            }.catch { error in
                switch error {
                case LoginError.walletNotFound:
                    seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, failure: .notFound))
                case LoginError.walletsJustRestored:
                    seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, failure: .isJustRestored))
                case LoginError.invalidMnemonic:
                    seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, failure: .invalid))
                case LoginError.connectionFailed:
                    seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, failure: .disconnect))
                default:
                    DropAlert().error(message: error.localizedDescription)
                    seal.reject(error)
                }
            }
        }
    }

    @IBAction func btnManualRestore(_ sender: Any) {
        let storyboard = UIStoryboard(name: "AutomaticRestore", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "ManualRestoreViewController")
        navigationController?.pushViewController(vc, animated: true)
    }
}

extension ExistingWalletsViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return ExistingWalletSection.allCases.count
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case ExistingWalletSection.remoteAlerts.rawValue:
            return self.remoteAlert != nil ? 1 : 0
        case ExistingWalletSection.wallet.rawValue:
            return wallets.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case ExistingWalletSection.remoteAlerts.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell, let remoteAlert = self.remoteAlert {
                cell.configure(AlertCardType.remoteAlert(remoteAlert),
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: {[weak self] in
                                 self?.remoteAlertDismiss()
                    },
                               onLink: { [weak self] in
                    if let url = URL(string: self?.remoteAlert?.link ?? "") {
                        UIApplication.shared.open(url)
                    }
                })
                cell.selectionStyle = .none
                return cell
            }
        case ExistingWalletSection.wallet.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ExistingWalletCell") as? ExistingWalletCell {
                cell.configure(wallets[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let existingWallet = wallets[indexPath.row]
        if existingWallet.failure == nil {
            OnBoardManager.shared.params?.singleSig = existingWallet.isSingleSig
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "WalletNameViewController")
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }
}
