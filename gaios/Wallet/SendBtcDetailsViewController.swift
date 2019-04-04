import Foundation
import UIKit
import PromiseKit

class SendBtcDetailsViewController: UIViewController {

    @IBOutlet var content: SendBtcDetailsView!
    let blockTime = ["~ 30 " + NSLocalizedString("id_minutes", comment: ""), NSLocalizedString("id_2_hours", comment: ""), NSLocalizedString("id_4_hours", comment: ""), ""]
    var feeLabel: UILabel = UILabel()
    var uiErrorLabel: UIErrorLabel!
    var wallet: WalletItem?
    var isFiat = false
    var transaction: Transaction!
    var amountData: [String: Any]?

    var feeEstimates: [UInt64?] = {
        var feeEstimates = [UInt64?](repeating: 0, count: 4)
        let estimates = getFeeEstimates() ?? []
        for (index, value) in [3, 12, 24, 0].enumerated() {
            feeEstimates[index] = estimates[value]
        }
        feeEstimates[3] = nil
        return feeEstimates
    }()

    var minFeeRate: UInt64 = {
        guard let estimates = getFeeEstimates() else { return 1000 }
        return estimates[0]
    }()

    var selectedFee: Int = {
        guard let settings = getGAService().getSettings() else { return 0 }
        switch settings.transactionPriority {
        case .High:
            return 0
        case .Medium:
            return 1
        case .Low:
            return 2
        case .Custom:
            return 3
        }
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = NSLocalizedString("id_send", comment: "")
        self.tabBarController?.tabBar.isHidden = true

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(self.dismissKeyboard (_:)))
        self.view.addGestureRecognizer(tapGesture)

        uiErrorLabel = UIErrorLabel(self.view)
        content.errorLabel.isHidden = true
        content.amountTextField.attributedPlaceholder = NSAttributedString(string: "0.00",
                                                                   attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])

        if transaction.addresseesReadOnly {
            content.amountTextField.isEnabled = false
            content.amountTextField.isUserInteractionEnabled = false
            content.sendAllFundsButton.isHidden = true
            content.maxAmountLabel.isHidden = true
        }

        if let oldFeeRate = getOldFeeRate() {
            feeEstimates[content.feeRateButtons.count - 1] = oldFeeRate + minFeeRate
            var found = false
            for index in 0..<content.feeRateButtons.count - 1 {
                guard let feeEstimate = feeEstimates[index] else { break }
                if oldFeeRate < feeEstimate {
                    found = true
                    selectedFee = index
                    break
                }
                content.feeRateButtons[index]?.isEnabled = false
            }
            if !found {
                selectedFee = content.feeRateButtons.count - 1
            }
        }

        content.fastFeeButton.setTitle(NSLocalizedString("id_fast", comment: ""))
        content.mediumFeeButton.setTitle(NSLocalizedString("id_medium", comment: ""))
        content.slowFeeButton.setTitle(NSLocalizedString("id_slow", comment: ""))
        content.customFeeButton.setTitle(NSLocalizedString("id_custom", comment: ""))
        content.sendAllFundsButton.setTitle(NSLocalizedString(("id_send_all_funds"), comment: ""), for: .normal)
        content.reviewButton.setTitle(NSLocalizedString("id_review", comment: ""), for: .normal)
        content.recipientTitle.text = NSLocalizedString("id_recipient", comment: "").uppercased()
        content.sendingTitle.text = NSLocalizedString("id_sending", comment: "").uppercased()
        content.minerFeeTitle.text = NSLocalizedString("id_miner_fee", comment: "").uppercased()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.amountTextField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.reviewButton.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.sendAllFundsButton.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.currencySwitch.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)

        if transaction.satoshi != 0 {
            updateAmountData(transaction.satoshi)
        }

        let address = transaction.addressees[0].address
        content.addressLabel.text = address

        updateReviewButton(false)
        updateFeeButtons()
        updateMaxAmountLabel()
        setCurrencySwitch()
        updateTransaction()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.amountTextField.removeTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.reviewButton.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.sendAllFundsButton.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.currencySwitch.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
    }

    func getOldFeeRate() -> UInt64? {
        if let prevTx = transaction.details["previous_transaction"] as? [String: Any] {
            return prevTx["fee_rate"] as? UInt64
        }
        return nil
    }

    func updateAmountData(_ satoshi: UInt64) {
        let newAmountData = convertAmount(details: ["satoshi": satoshi])
        if newAmountData?["satoshi"] as? UInt64 != amountData?["satoshi"] as? UInt64 {
            amountData = newAmountData
            updateAmountTextField(true)
        }
    }

    func updateAmountTextField(_ forceUpdate: Bool) {
        if forceUpdate {
            guard let settings = getGAService().getSettings() else { return }
            let textAmount = content.sendAllFundsButton.isSelected ? NSLocalizedString("id_all", comment: "") : amountData?[!isFiat ? settings.denomination.rawValue : "fiat"] as? String ?? String()
            content.amountTextField.text = textAmount
        }
        content.amountTextField.textColor = content.amountTextField.isEnabled ? UIColor.white : UIColor.lightGray
    }

    func setCurrencySwitch() {
        guard let settings = getGAService().getSettings() else { return }
        if !isFiat {
            content.currencySwitch.setTitle(settings.denomination.toString(), for: UIControlState.normal)
            content.currencySwitch.backgroundColor = UIColor.customMatrixGreen()
        } else {
            content.currencySwitch.setTitle(settings.getCurrency(), for: UIControlState.normal)
            content.currencySwitch.backgroundColor = UIColor.clear
        }
        content.currencySwitch.setTitleColor(UIColor.white, for: UIControlState.normal)
        updateFeeButtons()
    }

    func updateMaxAmountLabel() {
        guard let wallet = self.wallet else { return }
        wallet.getBalance().get { balance in
            self.content.maxAmountLabel.text = String.toBtc(satoshi: wallet.balance.satoshi)
        }.done { _ in }.catch { _ in }
    }

    @objc func click(_ sender: UIButton?) {
        if sender == content.sendAllFundsButton {
            content.sendAllFundsButton.isSelected = !content.sendAllFundsButton.isSelected
            updateTransaction()
            updateAmountTextField(true)
        } else if sender == content.reviewButton {
            self.performSegue(withIdentifier: "confirm", sender: self)
        } else if sender == content.currencySwitch {
            isFiat = !isFiat
            setCurrencySwitch()
            updateAmountTextField(true)
            updateMaxAmountLabel()
            updateTransaction()
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SendBTCConfirmationViewController {
            nextController.wallet = wallet
            nextController.transaction = transaction
        }
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        guard var amountText = content.amountTextField.text else { return }
        amountText = amountText.replacingOccurrences(of: ",", with: ".")
        content.amountTextField.text = amountText
        guard let settings = getGAService().getSettings() else { return }
        let amount = !amountText.isEmpty ? amountText : "0"
        let conversionKey = !isFiat ? settings.denomination.rawValue : "fiat"
        amountData = convertAmount(details: [conversionKey: amount])
        updateTransaction()
    }

    func updateTransaction() {
        guard let feeEstimate = feeEstimates[selectedFee] else { return }
        transaction.sendAll = content.sendAllFundsButton.isSelected
        transaction.feeRate = feeEstimate

        if !transaction.addresseesReadOnly {
            let satoshi = amountData?["satoshi"] as? UInt64 ?? 0
            let addressee = Addressee(address: content.addressLabel.text!, satoshi: satoshi)
            transaction.addressees = [addressee]
        }

        gaios.createTransaction(transaction: transaction).get { tx in
            self.transaction = tx
        }.done { tx in
            if !tx.error.isEmpty {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            }
            self.updateAmountData(tx.addressees[0].satoshi)
            self.uiErrorLabel.isHidden = true
            self.updateReviewButton(true)
            self.updateFeeButtons()
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                self.uiErrorLabel.text = localizedDescription
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                self.uiErrorLabel.text = NSLocalizedString("id_you_are_not_connected", comment: "")
            default:
                self.uiErrorLabel.text = error.localizedDescription
            }
            self.uiErrorLabel.isHidden = false
            self.updateReviewButton(false)
            self.updateFeeButtons()
        }
    }

    func updateReviewButton(_ enable: Bool) {
        content.reviewButton.setGradient(enable)
    }

    @objc func dismissKeyboard (_ sender: UITapGestureRecognizer) {
        content.amountTextField.resignFirstResponder()
    }

    func updateFeeButtons() {
        for index in 0..<feeEstimates.count {
            guard let feeButton = content.feeRateButtons[index] else { break }
            if feeButton.gestureRecognizers == nil && feeButton.isEnabled {
                let tap = UITapGestureRecognizer(target: self, action: #selector(clickFeeButton))
                feeButton.addGestureRecognizer(tap)
                feeButton.isUserInteractionEnabled = true
            }
            feeButton.isSelect = false
            feeButton.timeLabel.text = String(format: "%@", blockTime[index])
            guard let fee = feeEstimates[index] else {
                feeButton.feerateLabel.text = NSLocalizedString("id_set_custom_fee_rate", comment: "")
                break
            }
            let feeSatVByte = Double(fee) / 1000.0
            let feeSatoshi = UInt64(feeSatVByte * Double(transaction.size))
            let amount = isFiat ? String.toFiat(satoshi: feeSatoshi) : String.toBtc(satoshi: feeSatoshi)
            feeButton.feerateLabel.text = String(format: "%@ (%.1f satoshi / vbyte)", amount, feeSatVByte)
        }
        content.feeRateButtons[selectedFee]?.isSelect = true
    }

    func showFeeCustomPopup() {
        let alert = UIAlertController(title: NSLocalizedString("id_set_custom_fee_rate", comment: ""), message: "satoshi / byte", preferredStyle: .alert)
        alert.addTextField { (textField) in
            let feeRate: UInt64
            if let storedFeeRate = self.feeEstimates[self.content.feeRateButtons.count - 1] {
                feeRate = storedFeeRate
            } else if let oldFeeRate = self.getOldFeeRate() {
                feeRate = (oldFeeRate + self.minFeeRate)
            } else if let settings = getGAService().getSettings() {
                feeRate = UInt64(settings.customFeeRate ?? self.minFeeRate)
            } else {
                feeRate = self.minFeeRate
            }
            textField.keyboardType = .decimalPad
            textField.attributedPlaceholder = NSAttributedString(string: String(Double(feeRate) / 1000),
                                                                          attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { [weak alert] (_) in
            alert?.dismiss(animated: true, completion: nil)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_save", comment: ""), style: .default) { [weak alert] (_) in
            guard var amount = alert!.textFields![0].text else { return }
            amount = amount.replacingOccurrences(of: ",", with: ".")
            guard let number = Double(amount) else { return }
            let minFeeRate = Double(self.minFeeRate) / 1000
            if number < minFeeRate {
                Toast.show(String(format: NSLocalizedString("id_fee_rate_must_be_at_least_s", comment: ""), String(minFeeRate)))
                return
            }
            self.selectedFee = self.content.feeRateButtons.count - 1
            self.feeEstimates[self.content.feeRateButtons.count - 1] = UInt64(1000 * number)
            self.updateFeeButtons()
            self.updateTransaction()
        })
        self.present(alert, animated: true, completion: nil)
    }

    @objc func clickFeeButton(_ sender: UITapGestureRecognizer) {
        guard let view = sender.view else { return }
        switch view {
        case content.fastFeeButton:
            self.selectedFee = 0
        case content.mediumFeeButton:
            self.selectedFee = 1
        case content.slowFeeButton:
            self.selectedFee = 2
        case content.customFeeButton:
            showFeeCustomPopup()
        default:
            return
        }
        updateFeeButtons()
        updateTransaction()
    }
}

@IBDesignable
class SendBtcDetailsView: UIView {
    @IBOutlet weak var addressLabel: UILabel!
    @IBOutlet weak var maxAmountLabel: UILabel!
    @IBOutlet weak var amountTextField: UITextField!
    @IBOutlet weak var reviewButton: UIButton!
    @IBOutlet weak var currencySwitch: UIButton!
    @IBOutlet weak var errorLabel: UILabel!
    @IBOutlet weak var recipientTitle: UILabel!
    @IBOutlet weak var sendAllFundsButton: UIButton!
    @IBOutlet weak var minerFeeTitle: UILabel!
    @IBOutlet weak var fastFeeButton: FeeButton!
    @IBOutlet weak var mediumFeeButton: FeeButton!
    @IBOutlet weak var slowFeeButton: FeeButton!
    @IBOutlet weak var customFeeButton: FeeButton!
    @IBOutlet weak var sendingTitle: UILabel!
    lazy var feeRateButtons = [fastFeeButton, mediumFeeButton, slowFeeButton, customFeeButton]

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
        reviewButton.updateGradientLayerFrame()
    }
}
