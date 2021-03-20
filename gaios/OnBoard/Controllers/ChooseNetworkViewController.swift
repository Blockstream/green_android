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

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        title = ""
        lblTitle.text = "Choose your Network"
        lblHint.text = "Blockstream Green supports both  Bitcoin and the Liquid Network. Don't worry, you can create another wallet for a different network at any time."
        lblBitcoinTitle.text = "Bitcoin"
        lblBitcoinHint.text = "Bitcoin is the world's leading P2P cryptocurrency network. Select to send and receive bitcoin."
        lblLiquidTitle.text = "Liquid"
        lblLiquidHint.text = "The Liquid Network is a Bitcoin sidechain. Select to send and receive Liquid Bitcoin (L-BTC), Tether (USDt), and other Liquid assets."
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
    }

    @objc func didPressCardBitcoin() {
        AccountsManager.shared.current = Account(name: "", network: "mainnet")
        next()
    }

    @objc func didPressCardLiquid() {
        AccountsManager.shared.current = Account(name: "", network: "liquid")
        next()
    }

    @objc func didPressCardTestnet() {
        AccountsManager.shared.current = Account(name: "", network: "testnet")
        next()
    }

    func next() {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "ChooseSecurityViewController")
        navigationController?.pushViewController(vc, animated: true)
    }
}
