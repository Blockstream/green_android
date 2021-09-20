import Foundation
import PromiseKit
import UIKit
import AVFoundation

class SendBtcViewController: KeyboardViewController {

    var wallet: WalletItem?
    var transaction: Transaction?
    var isSweep: Bool = false
    private var isLiquid: Bool!
    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    @IBOutlet weak var textView: UITextView!
    var placeholderLabel = UILabel()

    @IBOutlet weak var qrCodeReaderBackgroundView: QRCodeReaderView!
    @IBOutlet weak var bottomButton: UIButton!
    @IBOutlet weak var orLabel: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        if let wallet = wallet {
            self.title = isSweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), wallet.localizedName()) : NSLocalizedString("id_send_to", comment: "")
        }
        isLiquid = AccountsManager.shared.current?.gdkNetwork?.liquid ?? false
        orLabel.text = NSLocalizedString("id_or", comment: "")

        textView.delegate = self
        textView.borderWidth = 1.0
        textView.borderColor = UIColor.white.withAlphaComponent(0.5)
        textView.layer.cornerRadius = 3.0

        bottomButton.setTitle(isLiquid ? NSLocalizedString("id_select_asset", comment: "") : NSLocalizedString("id_add_amount", comment: ""), for: .normal)

        qrCodeReaderBackgroundView.delegate = self

        addPlaceHolder()

        view.accessibilityIdentifier = AccessibilityIdentifiers.SendBtcScreen.view
        textView.accessibilityIdentifier = AccessibilityIdentifiers.SendBtcScreen.textView
        bottomButton.accessibilityIdentifier = AccessibilityIdentifiers.SendBtcScreen.nextBtn
    }

    func addPlaceHolder() {
        placeholderLabel.text = NSLocalizedString(isSweep ? "id_enter_a_private_key_to_sweep" : "id_enter_an_address", comment: "")
        placeholderLabel.font = UIFont.systemFont(ofSize: (textView.font?.pointSize)!)
        placeholderLabel.sizeToFit()
        textView.addSubview(placeholderLabel)
        placeholderLabel.frame.origin = CGPoint(x: 5, y: (textView.font?.pointSize)! / 2)
        placeholderLabel.textColor = UIColor.customTitaniumLight()
        placeholderLabel.isHidden = !textView.text.isEmpty
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
        updateButton(!textView.text.isEmpty)
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

    func updateButton(_ enable: Bool) {
        bottomButton.setGradient(enable)
    }

    @objc func click(_ sender: Any) {
        guard let text = textView.text else { return }
        createTransaction(userInput: text)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? SendBtcDetailsViewController {
            next.wallet = wallet
            next.transaction = sender as? Transaction
            next.assetId = next.transaction?.addressees.first?.assetId ?? "btc"
        } else if let next = segue.destination as? AssetsListTableViewController {
            next.isSend = true
            next.wallet = wallet
            next.transaction = sender as? Transaction
        }
    }

    func createTransaction(userInput: String) {
        let subaccount = self.wallet!.pointer
        let feeRate = getFeeEstimates()?.first ?? 1000
        let isBip21 = userInput.starts(with: "bitcoin:") || userInput.starts(with: "liquidnetwork:")
        let network = AccountsManager.shared.current?.gdkNetwork
        let policyAsset = network?.policyAsset ?? "btc"
        var tx: Transaction?

        self.startAnimating()
        let queue = DispatchQueue.global(qos: .default)
        Guarantee().compactMap { [unowned self] _ -> [String: Any] in
            if self.isSweep {
                return ["private_key": userInput, "fee_rate": feeRate, "subaccount": subaccount, "utxos": []]
            } else {
                // user input can be a bitcoin or liquid uri as well as an address
                var addressee: [String: Any] = ["address": userInput]
                if network?.liquid ?? false && !isBip21 {
                    // insert dummy policy asset to validate address
                    addressee["asset_id"] = policyAsset
                }
                return ["addressees": [addressee], "fee_rate": feeRate, "subaccount": subaccount, "utxos": [:]]
            }
        }.then(on: queue) { data in
            try SessionManager.shared.createTransaction(details: data).resolve()
        }.then(on: queue) { data -> Promise<[String: Any]> in
            let result = data["result"] as? [String: Any]
            tx = Transaction(result ?? [:])
            // handle tx errors
            if let error = tx?.error, !error.isEmpty && !["id_invalid_amount", "id_no_amount_specified", "id_insufficient_funds"].contains(error) {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(error, comment: ""))
            } else if let addressees = tx?.addressees, addressees.isEmpty {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString("id_invalid_address", comment: ""))
            } else if network?.liquid ?? false && !isBip21 {
                // remove dummy assetid
                let addressee = tx?.addressees.first
                tx?.addressees = [Addressee(address: addressee!.address, satoshi: addressee!.satoshi)]
            }
            if self.isSweep {
                return Promise { seal in seal.fulfill([:])}
            } else {
                // fetch utxos to create transaction
                return try SessionManager.shared.getUnspentOutputs(details: ["subaccount": self.wallet?.pointer ?? 0, "num_confs": 0]).resolve()
            }
        }.done { data in
            let result = data["result"] as? [String: Any]
            let unspent = result?["unspent_outputs"] as? [String: Any]
            tx?.details["utxos"] = unspent ?? [:]
            let haveAssets = tx?.addressees.first?.assetId != nil
            if self.isLiquid && !haveAssets {
                self.performSegue(withIdentifier: "asset_select", sender: tx)
            } else {
                self.performSegue(withIdentifier: "next", sender: tx)
            }
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                DropAlert().error(message: localizedDescription)
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                DropAlert().error(message: NSLocalizedString("id_you_are_not_connected", comment: ""))
            default:
                DropAlert().error(message: error.localizedDescription)
            }
            self.qrCodeReaderBackgroundView.startScan()
        }.finally {
            self.stopAnimating()
            self.updateButton(!self.textView.text.isEmpty)
        }
    }
}

extension SendBtcViewController: QRCodeReaderDelegate {
    func userDidGrant(_: Bool) {
        //
    }

    func onQRCodeReadSuccess(result: String) {
        qrCodeReaderBackgroundView.stopScan()
        createTransaction(userInput: result)
    }
}

extension SendBtcViewController: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        placeholderLabel.isHidden = !textView.text.isEmpty
        updateButton(!textView.text.isEmpty)
    }

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        if text == "\n" {
            view.endEditing(true)
            return false
        } else {
            return true
        }
    }
}
