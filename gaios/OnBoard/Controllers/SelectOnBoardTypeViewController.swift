import UIKit

class SelectOnBoardTypeViewController: UIViewController {

    enum ActionToButton {
        case newWallet
        case useHardware
    }

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblTerms: UILabel!
    @IBOutlet weak var btnTerms: UIButton!

    @IBOutlet weak var btnCheckTerms: CheckButton!
    @IBOutlet weak var btnNewWallet: UIButton!
    @IBOutlet weak var btnUseHardware: UIButton!
    @IBOutlet weak var btnAppSettings: UIButton!

    var actionToButton: ActionToButton?
    var iAgree: Bool = false
    let mash = UIImageView(image: UIImage(named: "il_mash")!)

    override func viewDidLoad() {
        super.viewDidLoad()

        mash.alpha = 0.6
        view.insertSubview(mash, at: 0)
        mash.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            mash.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 0),
            mash.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: 0),
            mash.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: 0),
            mash.heightAnchor.constraint(equalToConstant: view.frame.height / 1.8)
        ])

        iAgree = AccountsRepository.shared.swAccounts.count + AccountsRepository.shared.hwAccounts.count > 0

        setContent()
        setStyle()
        updateUI()
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
    }

    func setContent() {
        lblTitle.text = "id_simple__secure_selfcustody".localized
        lblHint.text = "Everything you need to take control of your bitcoin."
        lblTerms.text = "id_i_agree_to_the".localized
        btnNewWallet.setTitle("id_add_wallet".localized, for: .normal)
        btnUseHardware.setTitle("id_use_hardware_device".localized, for: .normal)
        btnAppSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 30.0, weight: .bold)
        lblTitle.textColor = .white
        lblHint.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        lblHint.textColor = .white.withAlphaComponent(0.6)
        btnNewWallet.setStyle(.primary)
        btnUseHardware.setStyle(.outlinedWhite)
        btnAppSettings.setStyle(.inline)
        btnAppSettings.setTitleColor(.white, for: .normal)
    }

    func updateUI() {
        btnCheckTerms.isSelected = iAgree
        btnNewWallet.isEnabled = iAgree
        btnUseHardware.isEnabled = iAgree

        if iAgree {
            btnNewWallet.setStyle(.primary)
            btnUseHardware.setStyle(.outlinedWhite)
        } else {
            btnNewWallet.setStyle(.primaryDisabled)
            btnUseHardware.setStyle(.outlinedWhiteDisabled)
        }
    }

    func onNext(_ action: ActionToButton) {
        if AnalyticsManager.shared.consent == .notDetermined {
            actionToButton = action

            let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCountlyViewController") as? DialogCountlyViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                self.present(vc, animated: true, completion: nil)
            }
            return
        }
        switch action {
        case .newWallet:
            let onBFlow = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = onBFlow.instantiateViewController(withIdentifier: "StartOnBoardViewController") as? StartOnBoardViewController {
                navigationController?.pushViewController(vc, animated: true)
            }
        case .useHardware:
            let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
            if let vc = hwFlow.instantiateViewController(withIdentifier: "WelcomeJadeViewController") as? WelcomeJadeViewController {
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }

    @IBAction func btnCheckTerms(_ sender: Any) {
        print(btnCheckTerms.isSelected)
        iAgree = btnCheckTerms.isSelected
        updateUI()
    }

    @IBAction func btnTerms(_ sender: Any) {
        if let url = URL(string: "https://blockstream.com/green/terms/") {
            UIApplication.shared.open(url)
        }
    }

    @IBAction func btnNewWallet(_ sender: Any) {
        AnalyticsManager.shared.addWallet()
        onNext(.newWallet)
    }

    @IBAction func btnUseHardware(_ sender: Any) {
        AnalyticsManager.shared.hwwWallet()
        onNext(.useHardware)
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            present(vc, animated: true) {}
        }
    }
}

extension SelectOnBoardTypeViewController: DialogCountlyViewControllerDelegate {
    func didChangeConsent() {
        switch AnalyticsManager.shared.consent {
        case .notDetermined:
            break
        case .denied, .authorized:
            if let actionToButton = actionToButton {
                onNext(actionToButton)
            }
        }
    }
}
