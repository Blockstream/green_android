import Foundation
import UIKit

class InitialViewController: UIViewController {
    @IBOutlet var content: InitialView!
    let menuButton = UIButton(type: .system)
    private var tempRestore = false

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.hidesBackButton = true
        menuButton.setImage(UIImage(named: "ellipses"), for: .normal)
        menuButton.addTarget(self, action: #selector(menuButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: menuButton)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.restoreButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        reload()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        guard content != nil else { return }
        content.createButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.restoreButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
    }

    func reload() {
        let network =  getGdkNetwork(getNetwork())
        let walletFound = isPinEnabled(network: getNetwork())
        content.createButton.setTitle(walletFound ?
            NSLocalizedString("id_log_in", comment: "") :
            NSLocalizedString("id_create_new_wallet", comment: ""), for: .normal)
        content.createButton.setGradient(true)
        if walletFound {
            content.createButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
            content.createButton.addTarget(self, action: #selector(loginClicked), for: .touchUpInside)
        } else {
            content.createButton.setTitle(NSLocalizedString("id_create_new_wallet", comment: ""), for: .normal)
            content.restoreButton.setTitle(NSLocalizedString("id_restore_green_wallet", comment: ""), for: .normal)
            content.createButton.addTarget(self, action: #selector(click), for: .touchUpInside)
            content.restoreButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        }
        content.restoreButton.isHidden = walletFound
        content.networkButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.setTitle(network.name, for: .normal)
        menuButton.tintColor = UIColor.customTitaniumLight()
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
        if !tempRestore && isPinEnabled(network: getNetwork()) {
            let message = String(format: NSLocalizedString("id_you_cannot_create_or_restore_a", comment: ""), getNetwork())
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in })
            DispatchQueue.main.async {
                self.present(alert, animated: true, completion: nil)
            }
        } else {
            self.performSegue(withIdentifier: identifier, sender: self)
        }
    }

    @objc func loginClicked(_ sender: UIButton) {
        self.performSegue(withIdentifier: "pin", sender: self)
    }

    @objc func menuButtonTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
        if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuViewController") as? PopoverMenuViewController {
            popover.delegate = self
            popover.modalPresentationStyle = .popover
            let popoverPresentationController = popover.popoverPresentationController
            popoverPresentationController?.backgroundColor = UIColor.customModalDark()
            popoverPresentationController?.delegate = self
            popoverPresentationController?.sourceView = self.menuButton
            popoverPresentationController?.sourceRect = self.menuButton.bounds
            self.present(popover, animated: true)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? EnterMnemonicsViewController {
            next.isTemporary = tempRestore
        }
        if let networkSelector = segue.destination as? NetworkSelectionSettings {
            networkSelector.saveTitle = NSLocalizedString("id_save", comment: "")
            networkSelector.onSave = {
                let network = getNetwork()
                onFirstInitialization(network: network)
                self.reload()
            }
        }
    }
}

extension InitialViewController: PopoverMenuDelegate {
    func didSelectionMenuOption(_ menuOption: MenuOption) {
        switch menuOption {
        case .watchOnly:
            performSegue(withIdentifier: "watchonly", sender: nil)
        case .tempRestore:
            tempRestore = true
            onAction(identifier: "enterMnemonic")
        }
    }
}

@IBDesignable
class InitialView: UIView {

    @IBOutlet weak var createButton: UIButton!
    @IBOutlet weak var restoreButton: UIButton!
    @IBOutlet weak var networkButton: UIButton!

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

extension InitialViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}
