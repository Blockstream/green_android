import Foundation
import UIKit

class InitialViewController: UIViewController {
    @IBOutlet var content: InitialView!
    let menuButton = UIButton(type: .system)
    private var tempRestore = false
    private var walletFound: Bool { isPinEnabled(network: getNetwork()) }

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.hidesBackButton = true
        menuButton.setImage(UIImage(named: "ellipses"), for: .normal)
        menuButton.addTarget(self, action: #selector(menuButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: menuButton)
        content.restoreButton.setTitle(NSLocalizedString("id_restore_green_wallet", comment: ""), for: .normal)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        let backItem = UIBarButtonItem()
        backItem.title = " "
        navigationItem.backBarButtonItem = backItem
        content.createLoginButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.restoreButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        reload()

        BLEManager.shared.dispose()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        guard content != nil else { return }
        content.createLoginButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.restoreButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
    }

    func reload() {
        let network =  getGdkNetwork(getNetwork())
        content.createLoginButton.setTitle(walletFound ?
            NSLocalizedString("id_log_in", comment: "") :
            NSLocalizedString("id_create_new_wallet", comment: ""), for: .normal)
        content.createLoginButton.setGradient(true)
        content.restoreButton.isHidden = walletFound
        content.networkButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.networkButton.setTitle(network.name, for: .normal)
        menuButton.tintColor = UIColor.customTitaniumLight()
    }

    @objc func click(_ sender: UIButton) {

        //TEMP
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "LandingViewController")
        navigationController?.pushViewController(vc, animated: true)
        return

        if sender == content.createLoginButton {
            if walletFound {
                self.performSegue(withIdentifier: "pin", sender: self)
            } else {
                self.performSegue(withIdentifier: "createWallet", sender: self)
            }
        } else if sender == content.restoreButton {
            self.performSegue(withIdentifier: "enterMnemonic", sender: self)
        } else if sender == content.networkButton {
            self.performSegue(withIdentifier: "network", sender: self)
        }
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
            networkSelector.onSelection = {
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
        case .hardwareWallets:
            performSegue(withIdentifier: "ble", sender: nil)
        case .tempRestore:
            tempRestore = true
            performSegue(withIdentifier: "enterMnemonic", sender: nil)
        case .help:
            performSegue(withIdentifier: "help", sender: nil)
        }
    }
}

@IBDesignable
class InitialView: UIView {

    @IBOutlet weak var createLoginButton: UIButton!
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
        createLoginButton.updateGradientLayerFrame()
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
