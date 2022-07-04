import UIKit

class ChooseNetworkViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardBitcoin: UIView!
    @IBOutlet weak var lblBitcoinTitle: UILabel!
    @IBOutlet weak var lblBitcoinHint: UILabel!

    @IBOutlet weak var cardLiquid: UIView!
    @IBOutlet weak var lblLiquidTitle: UILabel!
    @IBOutlet weak var lblLiquidHint: UILabel!

    @IBOutlet weak var cardTestnet: UIView!
    @IBOutlet weak var lblTestnetTitle: UILabel!
    @IBOutlet weak var lblTestnetHint: UILabel!

    @IBOutlet weak var cardLiquidTestnet: UIView!

    var restoreSingleSig = false
    var watchOnlySecurityOption: SecurityOption = .multi

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

        view.accessibilityIdentifier = AccessibilityIdentifiers.ChooseNetworkScreen.view
        cardTestnet.accessibilityIdentifier = AccessibilityIdentifiers.ChooseNetworkScreen.testnetCard
        cardLiquidTestnet.accessibilityIdentifier = AccessibilityIdentifiers.ChooseNetworkScreen.liquidTestnetCard

        switch LandingViewController.flowType {
        case .add:
            AnalyticsManager.shared.recordView(.onBoardChooseNetwork, sgmt: AnalyticsManager.shared.chooseNtwSgmt(flow: AnalyticsManager.OnBoardFlow.strCreate))
        case .restore:
            AnalyticsManager.shared.recordView(.onBoardChooseNetwork, sgmt: AnalyticsManager.shared.chooseNtwSgmt(flow: AnalyticsManager.OnBoardFlow.strRestore))
        case .watchonly:
            AnalyticsManager.shared.recordView(.onBoardChooseNetwork, sgmt: AnalyticsManager.shared.chooseNtwSgmt(flow: AnalyticsManager.OnBoardFlow.strCreate))
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        let hideTestnets: Bool = UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) != true
        cardTestnet.isHidden = hideTestnets
        cardLiquidTestnet.isHidden = hideTestnets
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_choose_your_network", comment: "")
        lblHint.text = NSLocalizedString("id_blockstream_green_supports_both", comment: "")
        lblBitcoinTitle.text = "Bitcoin"
        lblBitcoinHint.text = NSLocalizedString("id_bitcoin_is_the_worlds_leading", comment: "")
        lblLiquidTitle.text = "Liquid"
        lblLiquidHint.text = NSLocalizedString("id_the_liquid_network_is_a_bitcoin", comment: "")
        lblTestnetTitle.text = "Testnet"
        lblTestnetHint.text = ""
    }

    func setStyle() {
        cardBitcoin.layer.cornerRadius = 5.0
        cardLiquid.layer.cornerRadius = 5.0
        cardTestnet.layer.cornerRadius = 5.0
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardBitcoin))
        cardBitcoin.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardLiquid))
        cardLiquid.addGestureRecognizer(tapGesture2)
        let tapGesture3 = UITapGestureRecognizer(target: self, action: #selector(didPressCardTestnet))
        cardTestnet.addGestureRecognizer(tapGesture3)
        let tapGesture4 = UITapGestureRecognizer(target: self, action: #selector(didPressCardLiquidTestnet))
        cardLiquidTestnet.addGestureRecognizer(tapGesture4)
    }

    @objc func didPressCardBitcoin() {
        switch LandingViewController.flowType {
        case .watchonly:
            nextWatchOnly(.bitcoin)
        default:
            OnBoardManager.shared.params = OnBoardParams(network: AvailableNetworks.bitcoin.rawValue)
            next()
        }
    }

    @objc func didPressCardLiquid() {
        switch LandingViewController.flowType {
        case .watchonly:
            nextWatchOnly(.liquid)
        default:
            OnBoardManager.shared.params = OnBoardParams(network: AvailableNetworks.liquid.rawValue)
            next()
        }
    }

    @objc func didPressCardTestnet() {
        switch LandingViewController.flowType {
        case .watchonly:
            nextWatchOnly(.testnet)
        default:
            OnBoardManager.shared.params = OnBoardParams(network: AvailableNetworks.testnet.rawValue)
            next()
        }
    }

    @objc func didPressCardLiquidTestnet() {
        switch LandingViewController.flowType {
        case .watchonly:
            nextWatchOnly(.testnetLiquid)
        default:
            OnBoardManager.shared.params = OnBoardParams(network: AvailableNetworks.testnetLiquid.rawValue)
            next()
        }
    }

    func nextWatchOnly(_ network: AvailableNetworks) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WatchOnlyViewController") as? WatchOnlyViewController {
            vc.network = network
            vc.watchOnlySecurityOption = watchOnlySecurityOption
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func next() {
        switch LandingViewController.flowType {
        case .add:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "ChooseSecurityViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .restore:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryPhraseViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .watchonly:
            break
        }
    }
}
