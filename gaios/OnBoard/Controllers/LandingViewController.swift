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
    }

    func setContent() {
        lblTitle.text = "Blockstream Green: Simple and Secure"
        lblHint.text = "We'll get you set up in no time. Make sure you have a pen and paper ready!"

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
        let vc = storyboard.instantiateViewController(withIdentifier: "RestoreWalletViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

    @IBAction func btnWatchOnly(_ sender: Any) {

    }
}
