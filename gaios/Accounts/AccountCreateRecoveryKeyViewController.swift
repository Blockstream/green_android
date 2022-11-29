import UIKit
import PromiseKit

protocol AccountCreateRecoveryKeyDelegate: AnyObject {
    func didPublicKey(_ key: String)
    func didNewRecoveryPhrase(_ mnemonic: String)
    func didExistingRecoveryPhrase(_ mnemonic: String)
}

class AccountCreateRecoveryKeyViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var cardHW: UIView!
    @IBOutlet weak var lblHWTitle: UILabel!
    @IBOutlet weak var lblHWHint: UILabel!

    @IBOutlet weak var cardNewPhrase: UIView!
    @IBOutlet weak var lblNewPhraseTitle: UILabel!
    @IBOutlet weak var lblNewPhraseHint: UILabel!

    @IBOutlet weak var expandSeparator: UIView!
    @IBOutlet weak var expandArrow: UIImageView!
    @IBOutlet weak var lblExpand: UILabel!

    @IBOutlet weak var cardExistingPhrase: UIView!
    @IBOutlet weak var lblExistingPhraseTitle: UILabel!
    @IBOutlet weak var lblExistingPhraseHint: UILabel!

    @IBOutlet weak var cardPublicKey: UIView!
    @IBOutlet weak var lblPublicKeyTitle: UILabel!
    @IBOutlet weak var lblPublicKeyHint: UILabel!

    private var cards: [UIView] = []
    var session: SessionManager!
    weak var delegate: SecuritySelectViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        cards = [cardHW, cardNewPhrase, cardExistingPhrase, cardPublicKey]
        setContent()
        setStyle()
        setActions()

        cardExistingPhrase.isHidden = true
        cardPublicKey.isHidden = true

        AnalyticsManager.shared.recordView(.addAccountChooseRecovery, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_advanced_options_for_your_third", comment: "")
        lblHWTitle.text = NSLocalizedString("id_hardware_wallet", comment: "")
        lblHWHint.text = NSLocalizedString("id_use_a_hardware_wallet_as_your", comment: "")
        lblNewPhraseTitle.text = NSLocalizedString("id_new_recovery_phrase", comment: "")
        lblNewPhraseHint.text = NSLocalizedString("id_generate_a_new_recovery_phrase", comment: "")
        lblExistingPhraseTitle.text = NSLocalizedString("id_existing_recovery_phrase", comment: "")
        lblExistingPhraseHint.text = NSLocalizedString("id_use_an_existing_recovery_phrase", comment: "")
        lblPublicKeyTitle.text = NSLocalizedString("id_use_a_public_key", comment: "")
        lblPublicKeyHint.text = NSLocalizedString("id_use_an_xpub_for_which_you_own", comment: "")
        lblExpand.text = NSLocalizedString("id_more_options", comment: "")
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
        cardHW.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardHW)))
        cardNewPhrase.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardNewPhrase)))
        cardExistingPhrase.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardExistingPhrase)))
        cardPublicKey.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressCardPublicKey)))
        expandSeparator.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressExpandSeparator)))
    }

    @objc func didPressCardHW() {
        next(.hw)
    }

    @objc func didPressCardNewPhrase() {
        next(.newPhrase)
    }

    @objc func didPressCardExistingPhrase() {
        next(.existingPhrase)
    }

    @objc func didPressCardPublicKey() {
        next(.publicKey)
    }

    @objc func didPressExpandSeparator() {
        let clock = self.cardExistingPhrase.isHidden ? 1 : -1
        UIView.animate(withDuration: 0.3) {
            self.cardExistingPhrase.isHidden = !self.cardExistingPhrase.isHidden
            self.cardPublicKey.isHidden = !self.cardPublicKey.isHidden
            self.expandArrow.transform = self.expandArrow.transform.rotated(by: CGFloat(clock) * .pi / 2)
        }
    }

    func next(_ recoveryKeyType: RecoveryKeyType) {
        switch recoveryKeyType {
        case .hw:
            DropAlert().warning(message: NSLocalizedString("id_this_feature_is_coming_soon", comment: ""), delay: 3)
        case .newPhrase:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "OnBoardInfoViewController") as? OnBoardInfoViewController {
                OnBoardInfoViewController.flowType = .subaccount
                OnBoardInfoViewController.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        case .existingPhrase:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "MnemonicViewController") as? MnemonicViewController {
                vc.mnemonicActionType = .addSubaccount
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        case .publicKey:
            let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreatePublicKeyViewController") as? AccountCreatePublicKeyViewController {
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }

    func createSubaccount(_ params: CreateSubaccountParams) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly { self.startLoader(message: "Creating new account"); return Guarantee() }
            .then(on: bgq) { _ in return self.session.createSubaccount(params) }
            .ensure { self.stopLoader() }
            .done { (wallet: WalletItem) in
                //AnalyticsManager.shared.createAccount(account: AccountsManager.shared.current, walletType: type)
                DropAlert().success(message: "Account created")
                self.navigationController?.popToViewController(ofClass: WalletViewController.self, animated: true)
                self.delegate?.didCreatedWallet(wallet)
            }.catch { err in self.showError(err) }
    }
}
extension AccountCreateRecoveryKeyViewController: AccountCreateRecoveryKeyDelegate {
    func didPublicKey(_ key: String) {
        let params = CreateSubaccountParams(name: AccountType.twoOfThree.nameStringId,
                               type: .twoOfThree,
                               recoveryMnemonic: nil,
                               recoveryXpub: key)
        createSubaccount(params)
    }

    func didNewRecoveryPhrase(_ mnemonic: String) {
        let params = CreateSubaccountParams(name: AccountType.twoOfThree.nameStringId,
                                type: .twoOfThree,
                                recoveryMnemonic: mnemonic,
                                recoveryXpub: nil)
        createSubaccount(params)
    }

    func didExistingRecoveryPhrase(_ mnemonic: String) {
        let params = CreateSubaccountParams(name: AccountType.twoOfThree.nameStringId,
                                type: .twoOfThree,
                                recoveryMnemonic: mnemonic,
                                recoveryXpub: nil)
        createSubaccount(params)
    }
}
