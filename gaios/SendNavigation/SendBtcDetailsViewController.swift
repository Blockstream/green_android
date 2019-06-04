import Foundation
import UIKit
import PromiseKit

class SendBtcDetailsViewController: UIViewController, AssetsDelegate {

    var wallet: WalletItem?
    var transaction: Transaction!

    @IBOutlet var content: SendBtcDetailsView!
    private var feeLabel: UILabel = UILabel()
    private var uiErrorLabel: UIErrorLabel!
    private var isFiat = false
    private var assetTag: String?
    private var txTask: TransactionTask?

    private var asset: AssetInfo? {
        guard let tag = assetTag else { return nil }
        return wallet?.balance[tag]?.assetInfo ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
    }

    private var feeEstimates: [UInt64?] = {
        var feeEstimates = [UInt64?](repeating: 0, count: 4)
        let estimates = getFeeEstimates() ?? []
        for (index, value) in [3, 12, 24, 0].enumerated() {
            feeEstimates[index] = estimates[value]
        }
        feeEstimates[3] = nil
        return feeEstimates
    }()

    private var minFeeRate: UInt64 = {
        guard let estimates = getFeeEstimates() else { return 1000 }
        return estimates[0]
    }()

    private var selectedFee: Int = {
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
                                                                   attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])

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
        content.minerFeeTitle.text = NSLocalizedString("id_network_fee", comment: "").uppercased()
        content.assetNameLabel.text = NSLocalizedString("id_select_asset", comment: "")

        // setup liquid view
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        content.assetView.isHidden = !isLiquid
        content.sendAllFundsButton.isHidden = isLiquid
        content.currencySwitch.isHidden = isLiquid
        content.maxAmountLabel.isHidden = isLiquid
        content.maxAmountLabel.text = String.toBtc(satoshi: wallet!.btc.satoshi)
        content.assetView.heightAnchor.constraint(equalToConstant: 0).isActive = !isLiquid
        content.assetView.layoutIfNeeded()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.amountTextField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.reviewButton.addTarget(self, action: #selector(reviewButtonClick(_:)), for: .touchUpInside)
        content.sendAllFundsButton.addTarget(self, action: #selector(sendAllFundsButtonClick(_:)), for: .touchUpInside)
        content.currencySwitch.addTarget(self, action: #selector(currencySwitchClick(_:)), for: .touchUpInside)
        let assetTap = UITapGestureRecognizer(target: self, action: #selector(self.assetClick(_:)))
        content.assetClickableView.addGestureRecognizer(assetTap)

        let addressee = transaction.addressees.first!
        content.addressLabel.text = addressee.address
        updateAmountTextField()
        updateReviewButton(false)
        updateFeeButtons()
        setCurrencySwitch()
        updateTransaction()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.amountTextField.removeTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.reviewButton.removeTarget(self, action: #selector(reviewButtonClick(_:)), for: .touchUpInside)
        content.sendAllFundsButton.removeTarget(self, action: #selector(sendAllFundsButtonClick(_:)), for: .touchUpInside)
        content.currencySwitch.removeTarget(self, action: #selector(currencySwitchClick(_:)), for: .touchUpInside)
    }

    func getOldFeeRate() -> UInt64? {
        if let prevTx = transaction.details["previous_transaction"] as? [String: Any] {
            return prevTx["fee_rate"] as? UInt64
        }
        return nil
    }

    func updateAmountTextField() {
        content.amountTextField.textColor = content.amountTextField.isEnabled ? UIColor.white : UIColor.lightGray
        if content.sendAllFundsButton.isSelected {
            content.amountTextField.text = NSLocalizedString("id_all", comment: "")
            return
        }
        if content.amountTextField.text == NSLocalizedString("id_all", comment: "") {
            content.amountTextField.text = ""
            return
        }
        guard let addressee = transaction.addressees.first else { return }
        guard addressee.satoshi != 0 else { return }
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        let denominationBtc = getGAService().getSettings()!.denomination.rawValue
        let denominationAsset = assetTag ?? "btc"
        let key = isLiquid ? denominationAsset : isFiat ? "fiat" : denominationBtc
        let details = isLiquid ? ["satoshi": addressee.satoshi, "asset_info": asset!.encode()!] : ["satoshi": addressee.satoshi]
        let res = try? getSession().convertAmount(input: details)
        content.amountTextField.text = res?[key] as? String ?? ""
    }

    func setCurrencySwitch() {
        let settings = getGAService().getSettings()!
        let title = isFiat ? settings.getCurrency() : settings.denomination.toString()
        let color = isFiat ? UIColor.clear : UIColor.customMatrixGreen()
        content.currencySwitch.setTitle(title, for: UIControl.State.normal)
        content.currencySwitch.backgroundColor = color
        content.currencySwitch.setTitleColor(UIColor.white, for: UIControl.State.normal)
        updateFeeButtons()
    }

    @objc func sendAllFundsButtonClick(_ sender: UIButton) {
        content.sendAllFundsButton.isSelected = !content.sendAllFundsButton.isSelected
        content.amountTextField.isEnabled = !content.sendAllFundsButton.isSelected
        content.amountTextField.isUserInteractionEnabled = !content.sendAllFundsButton.isSelected
        updateTransaction()
        updateAmountTextField()
    }

    @objc func reviewButtonClick(_ sender: UIButton) {
        self.performSegue(withIdentifier: "next", sender: self)
    }

    @objc func assetClick(_ sender: UIButton) {
        self.performSegue(withIdentifier: "assets", sender: self)
    }

    @objc func currencySwitchClick(_ sender: UIButton) {
        isFiat = !isFiat
        setCurrencySwitch()
        updateAmountTextField()
        updateTransaction()
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SendBTCConfirmationViewController {
            nextController.wallet = wallet
            nextController.transaction = transaction
        } else if let nextController = segue.destination as? AssetsTableViewController {
            nextController.wallet = wallet
            nextController.delegate = self
            nextController.title = NSLocalizedString("id_select_asset", comment: "")
        }
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        updateTransaction()
    }

    func getSatoshi() -> UInt64? {
        var amountText = content.amountTextField.text!
        amountText = amountText.replacingOccurrences(of: ",", with: ".")
        amountText = amountText.isEmpty ? "0" : amountText
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        let denominationBtc = getGAService().getSettings()!.denomination.rawValue
        let denominationAsset = assetTag ?? "btc"
        let key = isLiquid ? denominationAsset : isFiat ? "fiat" : denominationBtc
        let details = isLiquid ? [key: amountText, "asset_info": asset!.encode()!] : [key: amountText]
        let res = try? getSession().convertAmount(input: details)
        return res?["satoshi"] as? UInt64
    }

    func onSelect(_ tag: String) {
        assetTag = tag
        content.assetNameLabel.text = asset?.name ?? tag
        content.currencySwitch.isHidden = tag != "btc"
        if let satoshi = wallet?.balance[tag]?.satoshi {
            content.maxAmountLabel.text = "\(String.toBtc(satoshi: satoshi, showDenomination: false)) \(tag)"
        }
    }

    func updateTransaction() {
        guard let feeEstimate = feeEstimates[selectedFee] else { return }
        transaction.sendAll = content.sendAllFundsButton.isSelected
        transaction.feeRate = feeEstimate

        if getGdkNetwork(getNetwork()).liquid && assetTag == nil {
            uiErrorLabel.text = NSLocalizedString("id_select_asset", comment: "")
            uiErrorLabel.isHidden = false
            return
        }

        if !transaction.addresseesReadOnly {
            let satoshi = self.getSatoshi() ?? 0
            let address = content.addressLabel.text!
            let addressee = Addressee(address: address, satoshi: satoshi, assetTag: assetTag)
            transaction.addressees = [addressee]
        }
        txTask?.cancel()
        txTask = TransactionTask(tx: transaction)
        txTask?.execute().get { tx in
            self.transaction = tx
        }.done { tx in
            if !tx.error.isEmpty {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            }
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

    @objc func dismissKeyboard (_ sender: UITapGestureRecognizer?) {
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
            let tp = TransactionPriority(rawValue: [3, 12, 24, 0][index]) ?? TransactionPriority.Medium
            feeButton.timeLabel.text = tp == .Custom ? "" : "~ \(tp.time)"
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
                                                                          attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
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
            break
        }
        updateFeeButtons()
        updateTransaction()
        dismissKeyboard(nil)
    }
}

class TransactionTask {
    var tx: Transaction
    private var cancelme = false
    private var task: DispatchWorkItem?

    init(tx: Transaction) {
        self.tx = tx
        task = DispatchWorkItem {
            let data = try? getSession().createTransaction(details: self.tx.details)
            self.tx = Transaction(data!)
        }
    }

    func execute() -> Promise<Transaction> {
        let bgq = DispatchQueue.global(qos: .background)
        return Promise<Transaction> { seal in
            self.task!.notify(queue: bgq) {
                guard !self.cancelme else { return seal.reject(PMKError.cancelled) }
                seal.fulfill(self.tx)
            }
            bgq.async(execute: self.task!)
        }
    }

    func cancel() {
        cancelme = true
        task?.cancel()
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
    @IBOutlet weak var assetView: UIView!
    @IBOutlet weak var assetNameLabel: UILabel!
    @IBOutlet weak var assetClickableView: UIView!

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
