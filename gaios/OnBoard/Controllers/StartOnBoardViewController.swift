import UIKit

//enum OnBoardingFlowType {
//    case add
//    case restore
//    case watchonly
//}
//
//enum OnBoardingChainType {
//    case mainnet
//    case testnet
//}
//
//enum ActionOnButton {
//    case new
//    case restore
//    case watchOnly
//}
//
//enum LandingScope {
//    case onBoard
//    case hwTerms
//}
//
//protocol LandingViewControllerDelegate: AnyObject {
//    func didPressContinue()
//}

class StartOnBoardViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var btnNewWallet: UIButton!
    @IBOutlet weak var btnRestoreWallet: UIButton!
    @IBOutlet weak var btnWatchOnly: UIButton!

    static var flowType: OnBoardingFlowType = .add
    static var chainType: OnBoardingChainType = .mainnet

    weak var delegate: LandingViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

//        customBack()
        setContent()
        setStyle()
        updateUI()

//        if landingScope == .onBoard {
//            AnalyticsManager.shared.recordView(.onBoardIntro)
//        }
    }

    func customBack() {
        var arrow = UIImage.init(named: "backarrow")
        if #available(iOS 13.0, *) {
            arrow = UIImage(systemName: "chevron.backward")
        }
        let newBackButton = UIBarButtonItem(image: arrow, style: UIBarButtonItem.Style.plain, target: self, action: #selector(LandingViewController.back(sender:)))
        navigationItem.leftBarButtonItem = newBackButton
        navigationItem.hidesBackButton = true
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
    }

    func setContent() {
        lblTitle.text = "Take Control: Your Keys, Your Bitcoin"
        lblHint.text = "Your keys secure your coins on the blockchain"
        btnNewWallet.setTitle(NSLocalizedString("id_new_wallet", comment: ""), for: .normal)
        btnRestoreWallet.setTitle(NSLocalizedString("id_restore_wallet", comment: ""), for: .normal)
        btnWatchOnly.setTitle(NSLocalizedString("id_watchonly", comment: ""), for: .normal)
    }

    func setStyle() {
    }

    func updateUI() {
        btnNewWallet.setStyle(.primary)
        btnRestoreWallet.setStyle(.primary)
        btnWatchOnly.setStyle(.outlinedWhite)
    }

    func onNext(_ action: ActionOnButton) {

        switch action {
        case .new:
            LandingViewController.flowType = .add
        case .restore:
            LandingViewController.flowType = .restore
        case .watchOnly:
            LandingViewController.flowType = .watchonly
        }
        let testnetAvailable = UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true
        if testnetAvailable {
            selectNetwork()
        } else {
            next()
        }
    }

    func selectNetwork() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Select Network", type: .networkPrefs, items: NetworkPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func next() {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)

        // refactor and dismiss LandingViewController
        switch LandingViewController.flowType {
        case .add:
            let vc = storyboard.instantiateViewController(withIdentifier: "OnBoardInfoViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .restore:
            let vc = storyboard.instantiateViewController(withIdentifier: "MnemonicViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .watchonly:
            let vc = storyboard.instantiateViewController(withIdentifier: "WatchOnlyViewController")
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnNewWallet(_ sender: Any) {
        AnalyticsManager.shared.newWallet()
        onNext(.new)
    }

    @IBAction func btnRestoreWallet(_ sender: Any) {
        onNext(.restore)
    }

    @IBAction func btnWatchOnly(_ sender: Any) {
        onNext(.watchOnly)
    }
}

extension StartOnBoardViewController: DialogListViewControllerDelegate {
    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch NetworkPrefs(rawValue: index) {
        case .mainnet:
            LandingViewController.chainType = .mainnet
            next()
        case .testnet:
            LandingViewController.chainType = .testnet
            next()
        case .none:
            break
        }
    }
}
