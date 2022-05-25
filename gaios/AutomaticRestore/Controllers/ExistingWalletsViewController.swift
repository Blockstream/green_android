import UIKit
import PromiseKit
import simd

struct ExistingWallet {
    let isSingleSig: Bool
    let isFound: Bool
    let isJustRestored: Bool
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

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()

        btnManualRestore.isHidden = true
        tableView.isHidden = true
        lblLoading.isHidden = false

        view.accessibilityIdentifier = AccessibilityIdentifiers.ExistingWalletsScreen.view
        btnManualRestore.accessibilityIdentifier = AccessibilityIdentifiers.ExistingWalletsScreen.manualRestoreBtn

        checkWallets()

        AnalyticsManager.shared.recordView(.onBoardScan, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
    }

    func setContent() {
        lblTitle.text = "Existing Wallets"
        lblHint.text = "Any wallet found will be displayed here."
        btnManualRestore.setTitle("Manual Restore", for: .normal)
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
                self.btnManualRestore.isHidden = self.wallets.filter { !$0.isFound }.count > 0 ? false : true
            }
        }.catch { error in
            print(error)
        }
    }

    func checkWallet(isSinglesig: Bool) -> Promise<ExistingWallet> {
        OnBoardManager.shared.params?.singleSig = isSinglesig
        let params = OnBoardManager.shared.params
        let session = SessionManager(account: OnBoardManager.shared.account)
        return Promise { seal in
            session.discover(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword, hwDevice: nil)
            .ensure {
                session.destroy()
            }.done { _ in
                seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, isFound: true, isJustRestored: false))
            }.catch { error in
                switch error {
                case LoginError.walletNotFound:
                    seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, isFound: false, isJustRestored: false))
                case LoginError.walletsJustRestored:
                    seal.fulfill(ExistingWallet(isSingleSig: isSinglesig, isFound: false, isJustRestored: true))
                case LoginError.invalidMnemonic:
                    DropAlert().error(message: NSLocalizedString("id_invalid_recovery_phrase", comment: ""))
                    seal.reject(error)
                case LoginError.connectionFailed:
                    DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
                    seal.reject(error)
                default:
                    print(error)
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

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return wallets.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let cell = tableView.dequeueReusableCell(withIdentifier: "ExistingWalletCell") as? ExistingWalletCell {
            cell.configure(wallets[indexPath.row])
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let existingWallet = wallets[indexPath.row]
        if existingWallet.isFound {
            OnBoardManager.shared.params?.singleSig = existingWallet.isSingleSig
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "WalletNameViewController")
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }
}
