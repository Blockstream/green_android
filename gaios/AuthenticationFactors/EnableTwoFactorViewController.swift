import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class EnableTwoFactorViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {

    @IBOutlet weak var tableview: UITableView!
    @IBOutlet weak var walletButton: UIButton!

    struct FactorItem {
        var name: String
        var image: UIImage
        var enabled: Bool
        var type: TwoFactorType
    }
    fileprivate var factors = [FactorItem]()
    var isHiddenWalletButton: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_twofactor_authentication", comment: "")
        walletButton.setTitle(NSLocalizedString("id_go_to_wallet", comment: ""), for: .normal)
        walletButton.isHidden = isHiddenWalletButton
        navigationItem.setHidesBackButton(!isHiddenWalletButton, animated: false)

        self.tableview.rowHeight = 70
    }

    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell =
            tableview.dequeueReusableCell(withIdentifier: "cell",
                                          for: indexPath as IndexPath)
        cell.textLabel?.text = self.factors[indexPath.row].name
        cell.imageView?.image = self.factors[indexPath.row].image
        let uiswitch = UISwitch()
        uiswitch.isOn = self.factors[indexPath.row].enabled
        uiswitch.isUserInteractionEnabled = false
        cell.accessoryView = uiswitch

        return cell
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.factors.count
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let selectedFactor: FactorItem = self.factors[indexPath.row]
        if selectedFactor.enabled {
            disable(selectedFactor.type)
            return
        }
        switch selectedFactor.type {
        case .email:
            self.performSegue(withIdentifier: "email", sender: nil)
        case .sms:
            self.performSegue(withIdentifier: "phone", sender: "sms")
        case .phone:
            self.performSegue(withIdentifier: "phone", sender: "call")
        case .gauth:
            self.performSegue(withIdentifier: "gauth", sender: nil)
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reloadData()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        walletButton.updateGradientLayerFrame()
    }

    func reloadData() {
        let dataTwoFactorConfig = try? getSession().getTwoFactorConfig()
        guard dataTwoFactorConfig != nil else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: [])) else { return }
        factors.removeAll()
        factors.append(FactorItem(name: NSLocalizedString("id_email", comment: ""), image: UIImage.init(named: "2fa_email")!, enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, type: .email))
        factors.append(FactorItem(name: NSLocalizedString("id_sms", comment: ""), image: UIImage.init(named: "2fa_sms")!, enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, type: .sms))
        factors.append(FactorItem(name: NSLocalizedString("id_call", comment: ""), image: UIImage.init(named: "2fa_call")!, enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, type: .phone))
        factors.append(FactorItem(name: NSLocalizedString("id_google_auth", comment: ""), image: UIImage.init(named: "2fa_google")!, enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: .gauth))
        tableview.reloadData()
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SetPhoneViewController {
            if sender as? String == "sms" {
                nextController.sms = true
            } else {
                nextController.phoneCall = true
            }
        }
    }

    @IBAction func walletButtonClick(_ sender: UIButton) {
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }

    func disable(_ type: TwoFactorType) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: false, confirmed: false, data: "")
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try getGAService().getSession().changeSettingsTwoFactor(method: type.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
            getGAService().reloadTwoFactor()
        }.catch { error in
            if let twofaError = error as? TwoFactorCallError {
                switch twofaError {
                case .failure(let localizedDescription), .cancel(let localizedDescription):
                    Toast.show(localizedDescription)
                }
            } else {
                Toast.show(error.localizedDescription)
            }
        }
    }
}
