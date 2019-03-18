import Foundation
import PromiseKit
import UIKit
import AVFoundation
import NVActivityIndicatorView

class SendBtcViewController: KeyboardViewController, UITextFieldDelegate {

    var wallet:WalletItem? = nil
    var transaction: Transaction? = nil

    @IBOutlet weak var textfield: UITextField!
    @IBOutlet weak var qrCodeReaderBackgroundView: QRCodeReaderView!
    @IBOutlet weak var bottomButton: UIButton!
    @IBOutlet weak var orLabel: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        self.title = NSLocalizedString("id_send_to", comment: "")
        orLabel.text = NSLocalizedString("id_or", comment: "")

        textfield.delegate = self
        textfield.attributedPlaceholder =
            NSAttributedString(string: NSLocalizedString(getGAService().isWatchOnly ? "id_enter_a_private_key_to_sweep" : "id_enter_an_address", comment: ""),
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])
        textfield.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: textfield.frame.height))
        textfield.leftViewMode = .always
        textfield.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)

        bottomButton.setTitle(NSLocalizedString("id_add_amount", comment: ""), for: .normal)

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
        if let nextController = segue.destination as? SendBtcDetailsViewController {
            nextController.wallet = wallet
            nextController.transaction = transaction
        }
    }

    private func createSweepTransaction(userInput: String, feeRate: UInt64) -> Promise<Transaction> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().map(on: bgq) {_ in
            try getSession().getReceiveAddress(subaccount: self.wallet!.pointer)
        }.then(on: bgq) { address -> Promise<Transaction> in
            let details: [String: Any] = ["private_key": userInput, "fee_rate":  feeRate, "subaccount": self.wallet!.pointer, "addressees" : [["address": address, "satoshi": 0]]]
            return gaios.createTransaction(details: details)
        }
    }

    func createTransaction(userInput: String) {
        guard let settings = getGAService().getSettings() else { return }
        guard let subaccount = wallet?.pointer else { return }
        let feeRate: UInt64 = settings.customFeeRate ?? UInt64(1000)

        startAnimating(type: NVActivityIndicatorType.ballRotateChase)

        // multiple fast consecutive taps will race so 2 segues can/will be performed
        updateButton(false)

        createSweepTransaction(userInput: userInput, feeRate: feeRate).compactMap { tx -> Promise<Transaction> in
            if tx.error.isEmpty {
                return Promise<Transaction> { seal in seal.fulfill(tx) }
            } else if tx.error != "id_invalid_private_key" || getGAService().isWatchOnly {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            }
            if self.transaction != nil {
                self.transaction!.addressees = [Addressee(address: userInput, satoshi: 0)]
                self.transaction!.feeRate = feeRate
                return gaios.createTransaction(transaction: self.transaction!)
            } else {
                let details: [String: Any] = ["addressees": [["address": userInput]], "fee_rate":  feeRate, "subaccount": subaccount]
                return gaios.createTransaction(details: details)
            }
        }.then { tx in
            return tx
        }.done { tx in
            self.transaction = tx
            if !tx.error.isEmpty && tx.error != "id_invalid_amount" && tx.error != "id_insufficient_funds" {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            }
            self.performSegue(withIdentifier: "next", sender: self)
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                Toast.show(localizedDescription, timeout: Toast.SHORT_DURATION)
                break
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                Toast.show(NSLocalizedString("id_you_are_not_connected", comment: ""), timeout: Toast.SHORT_DURATION)

                break
            default:
                Toast.show(error.localizedDescription, timeout: Toast.SHORT_DURATION)
            }
            self.qrCodeReaderBackgroundView.startScan()
        }.finally {
            self.stopAnimating()
            self.updateButton(!self.isTextFieldEmpty())
        }
    }
}

extension SendBtcViewController : QRCodeReaderDelegate {

    func onQRCodeReadSuccess(result: String) {
        qrCodeReaderBackgroundView.stopScan()
        createTransaction(userInput: result)
    }
}
