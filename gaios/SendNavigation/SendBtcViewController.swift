import Foundation
import PromiseKit
import UIKit
import AVFoundation
import NVActivityIndicatorView

class SendBtcViewController: KeyboardViewController, UITextFieldDelegate {

    var wallet: WalletItem?
    var transaction: Transaction?
    var isSweep: Bool = false
    private var isLiquid: Bool!

    @IBOutlet weak var textfield: UITextField!
    @IBOutlet weak var qrCodeReaderBackgroundView: QRCodeReaderView!
    @IBOutlet weak var bottomButton: UIButton!
    @IBOutlet weak var orLabel: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        if let wallet = wallet {
            self.title = isSweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), wallet.localizedName()) : NSLocalizedString("id_send_to", comment: "")
        }
        isLiquid = getGdkNetwork(getNetwork()).liquid
        orLabel.text = NSLocalizedString("id_or", comment: "")

        textfield.delegate = self
        textfield.attributedPlaceholder =
            NSAttributedString(string: NSLocalizedString(isSweep ? "id_enter_a_private_key_to_sweep" : "id_enter_an_address", comment: ""),
                attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        textfield.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: textfield.frame.height))
        textfield.leftViewMode = .always
        textfield.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)

        bottomButton.setTitle(isLiquid ? NSLocalizedString("id_select_asset", comment: "") : NSLocalizedString("id_add_amount", comment: ""), for: .normal)

        qrCodeReaderBackgroundView.delegate = self
    }

    private func startCapture() {
        if qrCodeReaderBackgroundView.isSessionNotDetermined() {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
                self.startCapture()
            }
            return
        }
        if !qrCodeReaderBackgroundView.isSessionAuthorized() {
            return
        }
        qrCodeReaderBackgroundView.startScan()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateButton(!isTextFieldEmpty())
        bottomButton.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        qrCodeReaderBackgroundView.stopScan()
        bottomButton.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        startCapture()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        bottomButton.updateGradientLayerFrame()
    }

    func isTextFieldEmpty() -> Bool {
        return textfield.text?.isEmpty ?? true
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        updateButton(!isTextFieldEmpty())
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        updateButton(!isTextFieldEmpty())
        return true
    }

    func updateButton(_ enable: Bool) {
        bottomButton.setGradient(enable)
    }

    @objc func click(_ sender: Any) {
        guard let text = textfield.text else { return }
        createTransaction(userInput: text)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? SendBtcDetailsViewController {
            next.wallet = wallet
            next.transaction = sender as? Transaction
        } else if let next = segue.destination as? AssetsListTableViewController {
            next.isSend = true
            next.wallet = wallet
            next.transaction = sender as? Transaction
        }
    }

    func createTransaction(userInput: String) {
        let settings = getGAService().getSettings()!
        let subaccount = self.wallet!.pointer
        let feeRate: UInt64 = settings.customFeeRate ?? UInt64(1000)

        startAnimating(type: NVActivityIndicatorType.ballRotateChase)
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap { [unowned self] _ -> [String: Any] in
            if self.isSweep {
                return ["private_key": userInput, "fee_rate": feeRate, "subaccount": subaccount]
            } else {
                return ["addressees": [["address": userInput]], "fee_rate": feeRate, "subaccount": subaccount]
            }
        }.compactMap(on: bgq) { data in
            try getSession().createTransaction(details: data)
        }.compactMap(on: bgq) { data in
            return Transaction(data)
        }.done { tx in
            if !tx.error.isEmpty && tx.error != "id_invalid_amount" {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            }
            if self.isLiquid {
                self.performSegue(withIdentifier: "asset_select", sender: tx)
            } else {
                self.performSegue(withIdentifier: "next", sender: tx)
            }
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                Toast.show(localizedDescription, timeout: Toast.SHORT)
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                Toast.show(NSLocalizedString("id_you_are_not_connected", comment: ""), timeout: Toast.SHORT)
            default:
                Toast.show(error.localizedDescription, timeout: Toast.SHORT)
            }
            self.qrCodeReaderBackgroundView.startScan()
        }.finally {
            self.stopAnimating()
            self.updateButton(!self.isTextFieldEmpty())
        }
    }
}

extension SendBtcViewController: QRCodeReaderDelegate {

    func onQRCodeReadSuccess(result: String) {
        qrCodeReaderBackgroundView.stopScan()
        createTransaction(userInput: result)
    }
}
