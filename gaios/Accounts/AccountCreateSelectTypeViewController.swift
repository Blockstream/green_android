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

        view.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSelectTypeScreen.view
        cardLegacy.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSelectTypeScreen.cardLegacy
        cardSegWit.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSelectTypeScreen.cardSegWit
        cardStandard.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSelectTypeScreen.cardStandard
        cardAmp.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSelectTypeScreen.cardAmp
        card2of3.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSelectTypeScreen.card2of3

        AnalyticsManager.shared.recordView(.addAccountChooseType, sgmt: AnalyticsManager.shared.sessSgmt(AccountDao.shared.current))
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_what_type_of_account_would_you", comment: "")
        lblLegacyTitle.text = NSLocalizedString("id_legacy_segwit_account", comment: "")
        lblLegacyHint.text = NSLocalizedString("id_bip49_accounts_allow_you_to", comment: "")
        lblSegWitTitle.text = NSLocalizedString("id_segwit_account", comment: "")
        lblSegWitHint.text = NSLocalizedString("id_bip84_accounts_allow_you_to", comment: "")
        lblStandardTitle.text = NSLocalizedString("id_standard_account", comment: "")
        lblStandardHint.text = NSLocalizedString("id_standard_accounts_allow_you_to", comment: "")
        lblAmpTitle.text = NSLocalizedString("id_amp_account", comment: "")
        lblAmpHint.text = NSLocalizedString("id_amp_accounts_are_only_available", comment: "")
        lbl2of3Title.text = NSLocalizedString("id_2of3_account", comment: "")
        lbl2of3Hint.text = NSLocalizedString("id_a_2of3_account_requires_two_out", comment: "")
    }

    func setStyle() {
        cards.forEach { card in
            card.layer.cornerRadius = 5.0
        }
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
        if let account = AccountDao.shared.current {
            let isSingleSig = (account.isSingleSig ?? false)
            let isLiquid = account.network == "liquid"
            let isLiquidTestnet = account.network == "testnet-liquid"
            cardLegacy.isHidden = !isSingleSig
            cardSegWit.isHidden = !isSingleSig
            cardStandard.isHidden = isSingleSig
            if (!isSingleSig && isLiquid) == true || (!isSingleSig && isLiquidTestnet) == true {
                cardAmp.isHidden = false
            } else {
                cardAmp.isHidden = true
            }
            card2of3.isHidden = isSingleSig || isLiquid || isLiquidTestnet
        }
    }

    @objc func didPressCardLegacy() {
        next(.segwitWrapped)
    }

    @objc func didPressCardSegWit() {
        next(.segWit)
    }

    @objc func didPressCardStandard() {
        next(.standard)
    }

    @objc func didPressCardAmp() {
        next(.amp)
    }

    @objc func didPressCard2of3() {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreateRecoveryKeyViewController") as? AccountCreateRecoveryKeyViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func next(_ accountType: AccountType) {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreateSetNameViewController") as? AccountCreateSetNameViewController {
            vc.accountType = accountType
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}
