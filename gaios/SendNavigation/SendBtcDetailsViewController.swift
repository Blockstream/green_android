import Foundation
import UIKit
import PromiseKit

class SendBtcDetailsViewController: UIViewController {

    @IBOutlet var content: SendBtcDetailsView!

    var wallet: WalletItem?
    var transaction: Transaction!
    var assetTag: String?

    private var feeLabel: UILabel = UILabel()
    private var uiErrorLabel: UIErrorLabel!
    private var isFiat = false
    private var txTask: TransactionTask?

    private var asset: AssetInfo? {
        guard let tag = assetTag else { return nil }
        return wallet?.balance[tag]?.assetInfo ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
    }

    private var oldFeeRate: UInt64? {
        if let prevTx = transaction.details["previous_transaction"] as? [String: Any] {
            return prevTx["fee_rate"] as? UInt64
        }
        return nil
    }

    private var isLiquid: Bool {
        return getGdkNetwork(getNetwork()).liquid
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
        self.tabBarController?.tabBar.isHidden = true

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(self.dismissKeyboard (_:)))
        self.view.addGestureRecognizer(tapGesture)

        uiErrorLabel = UIErrorLabel(self.view)
        content.errorLabel.isHidden = true
        content.amountTextField.attributedPlaceholder = NSAttributedString(string: "0.00",
                                                                   attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])

        if let oldFeeRate = oldFeeRate {
            feeEstimates[content.feeRateButtons.count - 1] = oldFeeRate + minFeeRate
            for index in (0..<content.feeRateButtons.count - 1).reversed() {
                guard let feeEstimate = feeEstimates[index] else { break }
                if oldFeeRate < feeEstimate {
                    selectedFee = index
                    break
                }
                content.feeRateButtons[index]?.isEnabled = false
                selectedFee = index
            }
        }

        // set labels
        self.title = NSLocalizedString("id_send", comment: "")
        content.fastFeeButton.setTitle(NSLocalizedString("id_fast", comment: ""))
        content.mediumFeeButton.setTitle(NSLocalizedString("id_medium", comment: ""))
        content.slowFeeButton.setTitle(NSLocalizedString("id_slow", comment: ""))
        content.customFeeButton.setTitle(NSLocalizedString("id_custom", comment: ""))
        content.sendAllFundsButton.setTitle(NSLocalizedString(("id_send_all_funds"), comment: ""), for: .normal)
        content.reviewButton.setTitle(NSLocalizedString("id_review", comment: ""), for: .normal)
        content.recipientTitle.text = NSLocalizedString("id_recipient", comment: "").uppercased()
        content.sendingTitle.text = NSLocalizedString("id_sending", comment: "").uppercased()
        content.minerFeeTitle.text = NSLocalizedString("id_network_fee", comment: "").uppercased()

        // setup liquid view
        content.assetView.isHidden = !isLiquid
        content.currencySwitch.isHidden = isLiquid
        content.assetView.heightAnchor.constraint(equalToConstant: 0).isActive = !isLiquid
        content.assetView.layoutIfNeeded()

        // read-only/increase fee transaction
        content.amountTextField.isEnabled = !transaction.addresseesReadOnly
        content.amountTextField.isUserInteractionEnabled = !transaction.addresseesReadOnly
        content.sendAllFundsButton.isHidden = transaction.addresseesReadOnly
        content.maxAmountLabel.isHidden = transaction.addresseesReadOnly
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.assetNameLabel.text = assetTag == "btc" ? "L-BTC" : asset?.name
        content.domainNameLabel.text = asset?.entity?.domain ?? ""
        content.domainNameLabel.isHidden = asset?.entity?.domain.isEmpty ?? true
        content.currencySwitch.isHidden = assetTag != "btc"
        content.amountTextField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.reviewButton.addTarget(self, action: #selector(reviewButtonClick(_:)), for: .touchUpInside)
        content.sendAllFundsButton.addTarget(self, action: #selector(sendAllFundsButtonClick(_:)), for: .touchUpInside)
        content.currencySwitch.addTarget(self, action: #selector(currencySwitchClick(_:)), for: .touchUpInside)
        let assetTap = UITapGestureRecognizer(target: self, action: #selector(self.assetClick(_:)))
        content.assetClickableView.addGestureRecognizer(assetTap)

        let addressee = transaction.addressees.first!
        content.addressLabel.text = addressee.address
        reloadWalletBalance()
        reloadAmount()
        reloadCurrencySwitch()
        updateReviewButton(false)
        updateFeeButtons()
        updateTransaction()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.amountTextField.removeTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.reviewButton.removeTarget(self, action: #selector(reviewButtonClick(_:)), for: .touchUpInside)
        content.sendAllFundsButton.removeTarget(self, action: #selector(sendAllFundsButtonClick(_:)), for: .touchUpInside)
        content.currencySwitch.removeTarget(self, action: #selector(currencySwitchClick(_:)), for: .touchUpInside)
    }

    func reloadAmount() {
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
        let tag = assetTag ?? "btc"
        let details = "btc" != tag ? ["satoshi": addressee.satoshi, "asset_info": asset!.encode()!] : ["satoshi": addressee.satoshi]
        let (amount, _) = Balance.convert(details: details)!.get(tag: isFiat ? "fiat" : tag)
        content.amountTextField.text = amount
    }

    func reloadCurrencySwitch() {
        let settings = getGAService().getSettings()!
        let title = isFiat ? settings.getCurrency() : settings.denomination.string
        let color = isFiat ? UIColor.clear : UIColor.customMatrixGreen()
        content.currencySwitch.setTitle(title, for: UIControl.State.normal)
        content.currencySwitch.backgroundColor = color
        content.currencySwitch.setTitleColor(UIColor.white, for: UIControl.State.normal)
        updateFeeButtons()
    }

    func reloadWalletBalance() {
        let tag = assetTag ?? "btc"
        let details = "btc" != tag ? ["satoshi": wallet!.balance[tag]!.satoshi, "asset_info": asset!.encode()!] : ["satoshi": wallet!.balance[tag]!.satoshi]
        let (amount, denom) = Balance.convert(details: details)!.get(tag: isFiat ? "fiat" : tag)
        content.maxAmountLabel.text =  "\(amount) \(denom)"
    }

    @objc func sendAllFundsButtonClick(_ sender: UIButton) {
        content.sendAllFundsButton.isSelected = !content.sendAllFundsButton.isSelected
        content.amountTextField.isEnabled = !content.sendAllFundsButton.isSelected
        content.amountTextField.isUserInteractionEnabled = !content.sendAllFundsButton.isSelected
        updateTransaction()
        reloadAmount()
    }

    @objc func reviewButtonClick(_ sender: UIButton) {
        self.performSegue(withIdentifier: "next", sender: self)
    }

    @objc func assetClick(_ sender: UIButton) {
        self.navigationController?.popViewController(animated: true)
    }

    @objc func currencySwitchClick(_ sender: UIButton) {
        isFiat = !isFiat
        reloadCurrencySwitch()
        reloadWalletBalance()
        reloadAmount()
        updateTransaction()
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SendBTCConfirmationViewController {
            nextController.wallet = wallet
            nextController.transaction = transaction
        }
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        updateTransaction()
    }

    func getSatoshi() -> UInt64? {
        var amountText = content.amountTextField.text!
        amountText = amountText.replacingOccurrences(of: ",", with: ".")
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = (Double(amountText) != nil) ? amountText : "0"
        let isBtc = assetTag ?? "btc" == "btc"
        let denominationBtc = getGAService().getSettings()!.denomination.rawValue
        let key = isFiat ? "fiat" : isBtc ? denominationBtc : assetTag ?? "btc"
        let details = !isBtc ? [key: amountText, "asset_info": asset!.encode()!] : [key: amountText]
        return Balance.convert(details: details)?.satoshi
    }

    func updateTransaction() {
        guard let feeEstimate = feeEstimates[selectedFee] else { return }
        transaction.sendAll = content.sendAllFundsButton.isSelected
        transaction.feeRate = feeEstimate

        if isLiquid && assetTag == nil {
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

            let balance = Balance.convert(details: ["satoshi": feeSatoshi])!
            let (amount, denom) = balance.get(tag: isFiat ? "fiat" : "btc")
            feeButton.feerateLabel.text =  "\(amount) \(denom) (\(feeSatVByte) satoshi / vbyte)"
        }
        content.feeRateButtons[selectedFee]?.isSelect = true
    }

    func showFeeCustomPopup() {
        let alert = UIAlertController(title: NSLocalizedString("id_set_custom_fee_rate", comment: ""), message: "satoshi / byte", preferredStyle: .alert)
        alert.addTextField { (textField) in
            let feeRate: UInt64
            if let storedFeeRate = self.feeEstimates[self.content.feeRateButtons.count - 1] {
                feeRate = storedFeeRate
            } else if let oldFeeRate = self.oldFeeRate {
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
            let settings = getGAService().getSettings()!
            guard var amount = alert!.textFields![0].text else { return }
            amount = amount.replacingOccurrences(of: ",", with: ".")
            guard let number = Double(amount) else { return }
            if 1000 * number >= Double(UInt64.max) { return }
            let feeRate = UInt64(1000 * number)
            if feeRate < self.minFeeRate {
                Toast.show(String(format: NSLocalizedString("id_fee_rate_must_be_at_least_s", comment: ""), String(self.minFeeRate)))
                return
            }
            self.selectedFee = self.content.feeRateButtons.count - 1
            self.feeEstimates[self.content.feeRateButtons.count - 1] = feeRate
            settings.customFeeRate = feeRate
            self.changeSettings(settings)
            self.updateFeeButtons()
            self.updateTransaction()
        })
        self.present(alert, animated: true, completion: nil)
    }

    @objc func clickFeeButton(_ sender: UITapGestureRecognizer) {
        guard let view = sender.view else { return }
        let settings = getGAService().getSettings()!
        switch view {
        case content.fastFeeButton:
            settings.transactionPriority = .High
            self.selectedFee = 0
        case content.mediumFeeButton:
            settings.transactionPriority = .Medium
            self.selectedFee = 1
        case content.slowFeeButton:
            settings.transactionPriority = .Low
            self.selectedFee = 2
        case content.customFeeButton:
            showFeeCustomPopup()
        default:
            break
        }
        changeSettings(settings)
        updateFeeButtons()
        updateTransaction()
        dismissKeyboard(nil)
    }

    func changeSettings(_ settings: Settings) {
        let details = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(settings), options: .allowFragments) as? [String: Any]
        let session = getGAService().getSession()
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map {_ in
        self.startAnimating()
        }.compactMap(on: bgq) { _ in
            try session.changeSettings(details: details!)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
        }.catch { error in
            self.showAlert(error)
        }
    }
}

extension SendBtcDetailsViewController {

    func showAlert(_ error: Error) {
        let text: String
        if let error = error as? TwoFactorCallError {
            switch error {
            case .failure(let localizedDescription), .cancel(let localizedDescription):
                text = localizedDescription
            }
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: text)
        }
    }

    func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
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
    @IBOutlet weak var domainNameLabel: UILabel!
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
