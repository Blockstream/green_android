import Foundation
import UIKit

class InitialViewController: UIViewController {
    @IBOutlet var content: InitialView!

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.hidesBackButton = true
        content.createButton.setTitle(NSLocalizedString("id_create_new_wallet", comment: ""), for: .normal)
        content.createButton.setGradient(true)
        content.restoreButton.setTitle(NSLocalizedString("id_restore_green_wallet", comment: ""), for: .normal)
        content.walletDetectedLabel.text = NSLocalizedString("id_a_wallet_is_detected_on_this", comment: "")
        content.walletLoginButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        content.walletLoginButton.addTarget(self, action: #selector(loginClicked), for: .touchUpInside)
        content.walletDetectionStackView.isUserInteractionEnabled = true
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(loginClicked))
        content.walletDetectionStackView.addGestureRecognizer(tapGesture)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.createButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.restoreButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        reload()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.createButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.restoreButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
    }

    func reload() {
        let defaults = getUserNetworkSettings()
        let networkName = defaults?["network"] as? String ?? "Bitcoin"
        content.networkButton.setTitle(networkName == "Mainnet" ? "Bitcoin" : networkName, for: .normal)
        content.walletDetectionStackView.isHidden = !isPinEnabled(network: getNetwork())
    }

    @objc func click(_ sender: UIButton) {
        if sender == content.createButton {
            onAction(identifier: "createWallet")
        } else if sender == content.restoreButton {
            onAction(identifier: "enterMnemonic")
        } else if sender == content.networkButton {
            networkButtonClicked()
        }
    }

    private func onAction(identifier: String) {
        if isPinEnabled(network: getNetwork()) {
            let message = NSLocalizedString("id_green_only_supports_one_pin_for", comment: "")
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in
                self.performSegue(withIdentifier: identifier, sender: self)
            })
            DispatchQueue.main.async {
                self.present(alert, animated: true, completion: nil)
            }
        } else {
            self.performSegue(withIdentifier: identifier, sender: self)
        }
    }

    func networkButtonClicked() {
        let networkSelector = self.storyboard?.instantiateViewController(withIdentifier: "networkSelection") as! NetworkSelectionSettings
        networkSelector.onSave = {
            let network = getNetwork()
            onFirstInitialization(network: network)
            self.reload()
        }
        networkSelector.providesPresentationContextTransitionStyle = true
        networkSelector.definesPresentationContext = true
        networkSelector.modalPresentationStyle = UIModalPresentationStyle.overCurrentContext
        networkSelector.modalTransitionStyle = UIModalTransitionStyle.crossDissolve
        self.present(networkSelector, animated: true, completion: nil)
    }

    @objc func loginClicked() {
        self.performSegue(withIdentifier: "pin", sender: self)
    }

    @IBAction func watchButtonClicked(_ sender: Any) {
        self.performSegue(withIdentifier: "watchonly", sender: self)
    }
}

@IBDesignable
class InitialView: UIView {

    @IBOutlet weak var walletDetectionStackView: UIStackView!
    @IBOutlet weak var createButton: UIButton!
    @IBOutlet weak var restoreButton: UIButton!
    @IBOutlet weak var networkButton: UIButton!
    @IBOutlet weak var walletDetectedLabel: UILabel!
    @IBOutlet weak var walletLoginButton: UIButton!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        createButton.updateGradientLayerFrame()
    }
}
