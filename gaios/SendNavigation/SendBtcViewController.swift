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

        self.startAnimating()
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap { [unowned self] _ -> [String: Any] in
            if self.isSweep {
                return ["private_key": userInput, "fee_rate": feeRate, "subaccount": subaccount]
            } else {
                // user input can be a bitcoin or liquid uri as well as an address
                return ["addressees": [["address": userInput]], "fee_rate": feeRate, "subaccount": subaccount]
            }
        }.compactMap(on: bgq) { data in
            try getSession().createTransaction(details: data)
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap(on: bgq) { data -> Transaction in
            let result = data["result"] as? [String: Any]
            return Transaction(result ?? [:])
        }.done { tx in
            if !tx.error.isEmpty && tx.error != "id_no_amount_specified" && tx.error != "id_invalid_amount" && tx.error != "Invalid AssetID" {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            }
            let haveAssets = tx.details["addressees_have_assets"] as? Bool
            if self.isLiquid && !(haveAssets ?? false) {
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
