import UIKit
import PromiseKit
import simd

struct ExistingWallet {
    let isSingleSig: Bool
    let isFound: Bool
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

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()

        btnManualRestore.isHidden = true
        tableView.isHidden = true
        showLoader()
        lblLoading.isHidden = false
        validateMnemonic()
    }

    func setContent() {
        lblTitle.text = "Existing Wallets"
        lblHint.text = "Any wallet found will be displayed here."
        btnManualRestore.setTitle("Manual Restore", for: .normal)
        lblLoading.text = "Looking for existing  wallets…"
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

    }

    func setStyle() {
    }

    enum ValidateError: Error {
        case invalidMnemonic
    }

    func validateMnemonic() {

        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            return Guarantee()
        }.compactMap(on: bgq) {
            guard try gaios.validateMnemonic(mnemonic: OnBoardManager.shared.params?.mnemonic ?? "") else {
                throw ValidateError.invalidMnemonic
            }
        }.done { _ in
            self.checkWalletsExistance()
        }.catch { error in
            DropAlert().error(message: "Invalid Recovery Phrase")
            self.invalidMnemonic()
        }
    }

    func invalidMnemonic() {
        self.wallets = [
            ExistingWallet(isSingleSig: true, isFound: false),
            ExistingWallet(isSingleSig: false, isFound: false)
        ]
        self.tableView.reloadData {
            self.hideLoader()
            self.lblLoading.isHidden = true
            self.tableView.isHidden = false
            self.btnManualRestore.isHidden = self.wallets.filter { !$0.isFound }.count > 0 ? false : true
        }
    }

    func checkWalletsExistance() {

        var singleSigExists: Bool = false
        var multiSigExists: Bool = false

        firstly {
            checkExistance(isSinglesig: true)
        }
        .then({ res -> Promise<Bool> in
            singleSigExists = res
            return self.checkExistance(isSinglesig: false)
        })
        .done { [weak self] res in
            multiSigExists = res

            self?.wallets = [
                ExistingWallet(isSingleSig: true, isFound: singleSigExists),
                ExistingWallet(isSingleSig: false, isFound: multiSigExists)
            ]
        }.ensure {
            self.tableView.reloadData {
                self.hideLoader()
                self.lblLoading.isHidden = true
                self.tableView.isHidden = false
                self.btnManualRestore.isHidden = self.wallets.filter { !$0.isFound }.count > 0 ? false : true
            }
        }.catch { error in
            print(error)
        }
    }

    func getSubaccounts() {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            return Guarantee()
        }.then(on: bgq) {
            SessionManager.shared.subaccounts()
        }.then(on: bgq) { wallets -> Promise<[WalletItem]> in
            let balances = wallets.map { wallet in { wallet.getBalance() } }
            return Promise.chain(balances).compactMap { _ in wallets }
        }.done { wallets in
            print(wallets)
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    func checkExistance(isSinglesig: Bool) -> Promise<Bool> {
        OnBoardManager.shared.params?.singleSig = isSinglesig
        let params = OnBoardManager.shared.params
        let bgq = DispatchQueue.global(qos: .background)
        let account = OnBoardManager.shared.account()
        let session = SessionManager.newSession(account: account)
        if isSinglesig {
            return Promise { seal in
                firstly {
                    return Guarantee()
                }.compactMap(on: bgq) {
                    return try session.connect(network: OnBoardManager.shared.networkName)
                }.then(on: bgq) {
                    session.login(details: ["mnemonic": params?.mnemonic ?? "", "password": params?.mnemomicPassword ?? ""])
                }.then(on: bgq) {
                    SessionManager.shared.subaccounts()
                }.then(on: bgq) { wallets -> Promise<Bool> in
                    return self.findTransactions(accounts: wallets)
                }.ensure {
                    _ = SessionManager.newSession(account: account)
                }.done { result in
                    print(result)
                    seal.fulfill(result)
                }.catch { error in
                    print(error)
                    seal.fulfill(false)
                }
            }
        } else {
            return Promise { seal in
                firstly {
                    return Guarantee()
                }.compactMap(on: bgq) {
                    return try session.connect(network: OnBoardManager.shared.networkName)
                }.then(on: bgq) {
                    session.login(details: ["mnemonic": params?.mnemonic ?? "", "password": params?.mnemomicPassword ?? ""])
                }.ensure {
                    _ = SessionManager.newSession(account: account)
                }.done { _ in
                    seal.fulfill(true)
                }.catch { error in
                    print(error)
                    seal.fulfill(false)
                }
            }
        }
    }

    func findTransactions(accounts: [WalletItem]) -> Promise<Bool> {
        var promises: [Promise<Bool>] = []
        for account in accounts {
            promises.append(SessionManager.shared.hasTransactions(pointer: account.pointer))
        }

        return Promise { seal in
            firstly {
                when(fulfilled: promises)
            }.done { list in
                seal.fulfill(list.filter { $0 == true }.count > 0)
            }.catch { error in
                print(error)
                seal.fulfill(false)
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

extension ExistingWalletsViewController {
    func showLoader() {
        if loadingIndicator.isAnimating { return }
        self.view.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: self.loaderPlaceholder.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: self.loadingIndicator.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    func hideLoader() {
        if !loadingIndicator.isAnimating { return }
        loadingIndicator.isAnimating = false
    }
}
