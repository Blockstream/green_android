import UIKit

class AccountCreateSelectTypeViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var cardLegacy: UIView!
    @IBOutlet weak var lblLegacyTitle: UILabel!
    @IBOutlet weak var lblLegacyHint: UILabel!

    @IBOutlet weak var cardSegWit: UIView!
    @IBOutlet weak var lblSegWitTitle: UILabel!
    @IBOutlet weak var lblSegWitHint: UILabel!

    @IBOutlet weak var cardStandard: UIView!
    @IBOutlet weak var lblStandardTitle: UILabel!
    @IBOutlet weak var lblStandardHint: UILabel!

    @IBOutlet weak var cardAmp: UIView!
    @IBOutlet weak var lblAmpTitle: UILabel!
    @IBOutlet weak var lblAmpHint: UILabel!

    @IBOutlet weak var card2of3: UIView!
    @IBOutlet weak var lbl2of3Title: UILabel!
    @IBOutlet weak var lbl2of3Hint: UILabel!

    var cards: [UIView] = []

    override func viewDidLoad() {
        super.viewDidLoad()

        cards = [cardLegacy, cardSegWit, cardStandard, cardAmp, card2of3]
        setContent()
        setStyle()
        setActions()
        setVisibility()
    }

    func setContent() {
        lblTitle.text = "Che tipo di account vuoi aggiungere?"

        lblLegacyTitle.text = "Legacy Account"
        lblLegacyHint.text = "BIP49 accounts allow you to segregate founds, and to receive on wrapped segwit addresses, thus ensuring the highest backwards compatibility when receiving funds from anyone in the network."
        lblSegWitTitle.text = "SegWit Account"
        lblSegWitHint.text = "BIP84 accounts allow you to segregate your founds, and to receive on bech32 native segwit addresses. This account type ensures cheaper transactions when sending funds, but not all services support bech32 addresses yet."
        lblStandardTitle.text = "Account Standard"
        lblStandardHint.text = "Gli account standard ti permettono di separare i fondi. Ogni account avrà indirizzi di ricezione separati e muovere fondi fra account richiederà una transazione."
        lblAmpTitle.text = "AMP Account"
        lblAmpHint.text = "AMP account are only available on Liquid wallets. You may be required to provide your account ID to issuers to receive an AMP Asset."
        lbl2of3Title.text = "Account 2of3"
        lbl2of3Hint.text = "A 2of3 account requires two out of three signatures to spend coins. The third signature is from a backup key known only to you. This gives you the security benefits of a standard account, while still allowing you to move your coins independently at any point in time."
    }

    func setStyle() {
        cards.forEach { card in
            card.layer.cornerRadius = 5.0
        }
        card2of3.alpha = 0.5
    }

    func setActions() {
        cards.forEach { card in
            card.layer.cornerRadius = 5.0
        }
        cardLegacy.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardLegacy)))
        cardSegWit.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardSegWit)))
        cardStandard.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardStandard)))
        cardAmp.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardAmp)))
        card2of3.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCard2of3)))
    }

    func setVisibility() {
        if let account = AccountsManager.shared.current {
            let isSingleSig = (account.isSingleSig ?? false)
            let isLiquid = account.network == "liquid"

            cardLegacy.isHidden = !isSingleSig
            cardSegWit.isHidden = !isSingleSig
            cardStandard.isHidden = isSingleSig
            cardAmp.isHidden = !(!isSingleSig && isLiquid)
            card2of3.isHidden = isSingleSig
        }
    }

    @objc func didPressCardLegacy() {
        // hold type
        next()
    }

    @objc func didPressCardSegWit() {
        // hold type
        next()
    }

    @objc func didPressCardStandard() {
        // hold type
        next()
    }

    @objc func didPressCardAmp() {
        // hold type
        next()
    }

    @objc func didPressCard2of3() { /* for future usage */ }

    func next() {
        performSegue(withIdentifier: "set-name", sender: nil)
    }
}
