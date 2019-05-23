import Foundation
import UIKit

class InitialViewController: UIViewController {
    @IBOutlet var content: InitialView!
    @IBOutlet weak var watchOnlyButton: UIBarButtonItem!

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.hidesBackButton = true
        content.createButton.setTitle(NSLocalizedString("id_create_new_wallet", comment: ""), for: .normal)
        content.createButton.setGradient(true)
        content.restoreButton.setTitle(NSLocalizedString("id_restore_green_wallet", comment: ""), for: .normal)
        content.walletDetectionStackView.isUserInteractionEnabled = true
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(loginClicked))
        content.walletDetectionStackView.addGestureRecognizer(tapGesture)
        setLogInText()
    }

    func setLogInText() {
        let greyString = NSLocalizedString("id_a_wallet_is_detected_on_this", comment: "")
        let greenString = NSLocalizedString("id_log_in", comment: "")
        let finalString = NSMutableAttributedString(string: greyString + " " + greenString)
        finalString.setColor(color: UIColor.customTitaniumLight(), forText: greenString)
        finalString.setColor(color: UIColor.customMatrixGreen(), forText: greenString)
        content.walletDetectedLabel.attributedText = finalString
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
        let network =  getGdkNetwork(getNetwork())
        content.networkButton.setTitle(network.name, for: .normal)
        content.walletDetectionStackView.isHidden = !isPinEnabled(network: network.network)
        watchOnlyButton.tintColor = !network.liquid ? UIColor.customTitaniumLight() : UIColor.clear
        watchOnlyButton.isEnabled = !network.liquid
    }

    @objc func click(_ sender: UIButton) {
        if sender == content.createButton {
            onAction(identifier: "createWallet")
        } else if sender == content.restoreButton {
            onAction(identifier: "enterMnemonic")
        } else if sender == content.networkButton {
            self.performSegue(withIdentifier: "network", sender: self)
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

    @objc func loginClicked() {
        self.performSegue(withIdentifier: "pin", sender: self)
    }

    @IBAction func watchButtonClicked(_ sender: Any) {
        self.performSegue(withIdentifier: "watchonly", sender: self)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let networkSelector = segue.destination as? NetworkSelectionSettings {
            networkSelector.onSave = {
                let network = getNetwork()
                onFirstInitialization(network: network)
                self.reload()
            }
        }
    }
}

@IBDesignable
class InitialView: UIView {

    @IBOutlet weak var walletDetectionStackView: UIStackView!
    @IBOutlet weak var createButton: UIButton!
    @IBOutlet weak var restoreButton: UIButton!
    @IBOutlet weak var networkButton: UIButton!
    @IBOutlet weak var walletDetectedLabel: UILabel!

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
