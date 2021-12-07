import UIKit

enum OnBoardingFlowType {
    case add
    case restore
}

class LandingViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblTerms: UILabel!
    @IBOutlet weak var btnTerms: UIButton!

    @IBOutlet weak var btnCheckTerms: CheckButton!
    @IBOutlet weak var btnNewWallet: UIButton!
    @IBOutlet weak var btnRestoreWallet: UIButton!
    @IBOutlet weak var btnWatchOnly: UIButton!

    @IBOutlet weak var iconPlus: UIImageView!
    @IBOutlet weak var iconRestore: UIImageView!
    @IBOutlet weak var iconWatch: UIImageView!

    static var flowType: OnBoardingFlowType = .add

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        updateUI()

        view.accessibilityIdentifier = AccessibilityIdentifiers.LandingScreen.view
        btnCheckTerms.accessibilityIdentifier = AccessibilityIdentifiers.LandingScreen.acceptTermsBtn
        btnNewWallet.accessibilityIdentifier = AccessibilityIdentifiers.LandingScreen.newWalletBtn
        btnRestoreWallet.accessibilityIdentifier = AccessibilityIdentifiers.LandingScreen.restoreWalletBtn
        btnWatchOnly.accessibilityIdentifier = AccessibilityIdentifiers.LandingScreen.watchOnlyWalletBtn
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_blockstream_greennsimple_and", comment: "")
        lblHint.text = NSLocalizedString("id_well_get_you_set_up_in_no_time", comment: "")
        lblTerms.text = NSLocalizedString("id_i_agree_to_the", comment: "")
        btnNewWallet.setTitle(NSLocalizedString("id_new_wallet", comment: ""), for: .normal)
        btnRestoreWallet.setTitle(NSLocalizedString("id_restore_wallet", comment: ""), for: .normal)
        btnWatchOnly.setTitle(NSLocalizedString("id_watchonly", comment: ""), for: .normal)
    }

    func setStyle() {
        btnNewWallet.cornerRadius = 4.0
        btnRestoreWallet.cornerRadius = 4.0
        btnWatchOnly.cornerRadius = 4.0
        btnWatchOnly.borderWidth = 1.0
        btnWatchOnly.borderColor = UIColor.customGrayLight()
    }

    func updateUI() {
        let isOn = btnCheckTerms.isSelected
        btnNewWallet.isEnabled = isOn
        btnRestoreWallet.isEnabled = isOn
        btnWatchOnly.isEnabled = isOn

        if isOn {
            btnNewWallet.backgroundColor = UIColor.customMatrixGreen()
            btnRestoreWallet.backgroundColor = UIColor.customMatrixGreen()
            btnNewWallet.setTitleColor(.white, for: .normal)
            btnRestoreWallet.setTitleColor(.white, for: .normal)
            btnWatchOnly.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
            iconPlus.image = iconPlus.image?.maskWithColor(color: .white)
            iconRestore.image = iconRestore.image?.maskWithColor(color: .white)
            iconWatch.image = iconWatch.image?.maskWithColor(color: UIColor.customMatrixGreen())
        } else {
            btnNewWallet.backgroundColor = UIColor.customBtnOff()
            btnRestoreWallet.backgroundColor = UIColor.customBtnOff()
            btnNewWallet.setTitleColor(UIColor.customGrayLight(), for: .normal)
            btnRestoreWallet.setTitleColor(UIColor.customGrayLight(), for: .normal)
            btnWatchOnly.setTitleColor(UIColor.customGrayLight(), for: .normal)
            iconPlus.image = iconPlus.image?.maskWithColor(color: UIColor.customGrayLight())
            iconRestore.image = iconRestore.image?.maskWithColor(color: UIColor.customGrayLight())
            iconWatch.image = iconWatch.image?.maskWithColor(color: UIColor.customGrayLight())
        }
    }

    @IBAction func btnCheckTerms(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnTerms(_ sender: Any) {
        if let url = URL(string: "https://blockstream.com/green/terms/") {
            UIApplication.shared.open(url)
        }
    }

    @IBAction func btnNewWallet(_ sender: Any) {
        LandingViewController.flowType = .add
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "ChooseNetworkViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

    @IBAction func btnRestoreWallet(_ sender: Any) {
        LandingViewController.flowType = .restore
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "ChooseNetworkViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

    @IBAction func btnWatchOnly(_ sender: Any) {

        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WatchOnlyViewController")
        navigationController?.pushViewController(vc, animated: true)
    }
}
