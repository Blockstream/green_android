import Foundation
import UIKit
import PromiseKit

class SendViewController: KeyboardViewController {

    enum SendSection: Int, CaseIterable {
        case recipient = 0
        case addRecipient = 1
        case fee = 2
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!

    var wallet: WalletItem?
    var recipients: [Recipient] = []
    var isSendAll: Bool = false
    var isSweep: Bool = false

    var isLiquid: Bool {
        get {
            return AccountsManager.shared.current?.gdkNetwork?.liquid ?? false
        }
    }

    var isBtc: Bool {
        get {
            let ntw = AccountsManager.shared.current?.network
            return ntw == "mainnet" || ntw == "testnet"
        }
    }

    private var feeEstimates: [UInt64?] = {
        var feeEstimates = [UInt64?](repeating: 0, count: 4)
        guard let estimates = getFeeEstimates() else {
            // We use the default minimum fee rates when estimates are not available
            let defaultMinFee = AccountsManager.shared.current?.gdkNetwork?.liquid ?? false ? 100 : 1000
            return [UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee)]
        }
        for (index, value) in [3, 12, 24, 0].enumerated() {
            feeEstimates[index] = estimates[value]
        }
        feeEstimates[3] = nil
        return feeEstimates
    }()

    var transactionPriority: TransactionPriority = .High
    var customFee: UInt64 = 1000

    var transaction: Transaction?
    var isBumpFee: Bool = false

    func selectedFee() -> Int {
        switch transactionPriority {
        case .High:
            return 0
        case .Medium:
            return 1
        case .Low:
            return 2
        case .Custom:
            return 3
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        addRecipient()
    }

    func setContent() {
//        title = NSLocalizedString("id_send", comment: "")
        if let wallet = wallet {
            self.title = isSweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), wallet.localizedName()) : NSLocalizedString("id_send_to", comment: "")
        }
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func addRecipient() {
        let recipient = Recipient()
        if isBumpFee {
            let addressee = transaction?.addressees.first
            recipient.address = addressee?.address

            if let satoshi = addressee?.satoshi {
                let details = ["satoshi": satoshi]
                let (amount, _) = satoshi == 0 ? ("", "") : Balance.convert(details: details)?.get(tag: "btc") ?? ("", "")
                recipient.amount = amount
            }
        }

        recipient.assetId = isBtc ? "btc" : nil
        recipients.append(recipient)
        reloadSections([SendSection.recipient], animated: true)
    }

    func reloadSections(_ sections: [SendSection], animated: Bool) {
        DispatchQueue.main.async {
            if animated {
                self.tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            } else {
                UIView.performWithoutAnimation {
                    self.tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
                }
            }
        }
    }

//    func reloadAmount() {
//
//
//        guard let satoshi = transaction.addressees.first?.satoshi else { return }
//        let details = btc != assetId ? ["satoshi": satoshi, "asset_info": asset!.encode()!] : ["satoshi": satoshi]
//        let (amount, _) = satoshi == 0 ? ("", "") : Balance.convert(details: details)?.get(tag: isFiat ? "fiat" : assetId) ?? ("", "")
//        content.amountTextField.text = amount
//    }

    func getAddressee() -> [String: Any] {
        //handling only 1 recipient for the moment
        let recipient = recipients.first
        let addressInput: String = recipient?.address ?? ""

        let isBip21 = addressInput.starts(with: "bitcoin:") || addressInput.starts(with: "liquidnetwork:")
        let network = AccountsManager.shared.current?.gdkNetwork
        let policyAsset = recipients[0].assetId

        let satoshi = recipient?.getSatoshi() ?? 0

        var addressee: [String: Any] = [:]
        addressee["address"] = addressInput
        addressee["satoshi"] = satoshi

        if network?.liquid ?? false && !isBip21 {
            addressee["asset_id"] = policyAsset
        }

        return addressee
    }

    func getPrivateKey() -> String {
        //handling only 1 recipient for the moment
        let recipient = recipients.first
        return recipient?.address ?? ""
    }

    func createTransaction() {
        let subaccount = self.wallet!.pointer

        feeEstimates[3] = customFee
        guard let feeEstimate = feeEstimates[selectedFee()] else { return }
        let feeRate = feeEstimate

        self.startAnimating()
        let queue = DispatchQueue.global(qos: .default)
        firstly {
            return Guarantee()
        }.then(on: queue) {
            return try SessionManager.shared.getUnspentOutputs(details: ["subaccount": self.wallet?.pointer ?? 0, "num_confs": 0]).resolve()
        }.compactMap { data in

            if self.isBumpFee {
                var details: [String: Any] = [:]
                details = self.transaction!.details
                details["fee_rate"] = feeRate
                return details
            }
            if self.isSweep {
                var details: [String: Any] = [:]
                details["private_key"] = self.getPrivateKey()
                details["fee_rate"] = feeRate
                details["subaccount"] = subaccount
                details["utxos"] = [:]
                return details
            } else {
                let result = data["result"] as? [String: Any]
                let unspent = result?["unspent_outputs"] as? [String: Any]
                var details: [String: Any] = [:]
                details["addressees"] = [self.getAddressee()]
                details["fee_rate"] = feeRate
                details["subaccount"] = subaccount
                details["utxos"] = unspent ?? [:]
                if self.isSendAll == true {
                    details["send_all"] = true
                }
                return details
            }

        }.then(on: queue) { data in
            try SessionManager.shared.createTransaction(details: data).resolve()
        }.done { data in
            let result = data["result"] as? [String: Any]
            let tx: Transaction = Transaction(result ?? [:])
            if !tx.error.isEmpty {
                throw TransactionError.invalid(localizedDescription: NSLocalizedString(tx.error, comment: ""))
            } else {
                self.onTransactionReady(tx)
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
        }.finally {
            self.stopAnimating()
        }
    }

    func onTransactionReady(_ transaction: Transaction) {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendConfirmViewController") as? SendConfirmViewController {
            vc.wallet = wallet
            vc.transaction = transaction
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        createTransaction()
    }
}

extension SendViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return SendSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case SendSection.recipient.rawValue:
            return recipients.count
        case SendSection.addRecipient.rawValue:
            return 0 // 1
        case SendSection.fee.rawValue:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        let removeRecipientAction: VoidToVoid = {[weak self] in
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRecipientDeleteViewController") as? DialogRecipientDeleteViewController {
                vc.index = indexPath.row
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                self?.present(vc, animated: false, completion: nil)
            }
        }
        let needRefresh: VoidToVoid = {[weak self] in
            self?.tableView.beginUpdates()
            self?.tableView.endUpdates()
        }
        let selectAsset: VoidToVoid = {[weak self] in
            //        if self.isLiquid && !haveAssets { ...  }
            //        if let next = segue.destination as? AssetsListTableViewController {
            //            next.isSend = true
            //            next.wallet = wallet
            //            next.transaction = sender as? Transaction
            //        }
            let storyboard = UIStoryboard(name: "Assets", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetsListTableViewController") as? AssetsListTableViewController {
                vc.isSend = true
                vc.wallet = self?.wallet
                vc.index = indexPath.row
                vc.delegate = self
                self?.present(vc, animated: true, completion: nil)
            }
        }
        let qrAction: VoidToVoid = {[weak self] in
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogQRCodeScanViewController") as? DialogQRCodeScanViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.index = indexPath.row
                vc.delegate = self
                self?.present(vc, animated: false, completion: nil)
            }
        }
        let tapSendAll: VoidToVoid = {[weak self] in
            self?.isSendAll.toggle()
            self?.reloadSections([SendSection.recipient], animated: true)
        }
        switch indexPath.section {
        case SendSection.recipient.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "RecipientCell") as? RecipientCell {
                cell.configure(recipient: recipients[indexPath.row],
                               index: indexPath.row,
                               isMultiple: recipients.count > 1,
                               removeRecipient: removeRecipientAction,
                               needRefresh: needRefresh,
                               chooseAsset: selectAsset,
                               qrScan: qrAction,
                               walletItem: self.wallet,
                               tapSendAll: tapSendAll,
                               isSendAll: self.isSendAll,
                               isSweep: self.isSweep,
                               isBumpFee: self.isBumpFee)
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.addRecipient.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AddRecipientCell") as? AddRecipientCell {
                cell.configure {
                    [weak self] in
                    self?.addRecipient()
                }
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.fee.rawValue:
            let setCustomFee: VoidToVoid = {[weak self] in
                let storyboard = UIStoryboard(name: "Shared", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCustomFeeViewController") as? DialogCustomFeeViewController {
                    vc.modalPresentationStyle = .overFullScreen
                    vc.delegate = self
                    vc.storedFeeRate = self?.feeEstimates[3]
                    self?.present(vc, animated: false, completion: nil)
                }
            }
            let updatePriority: ((TransactionPriority) -> Void) = {[weak self] value in
                self?.transactionPriority = value
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeEditCell") as? FeeEditCell {
                cell.configure(setCustomFee: setCustomFee, updatePriority: updatePriority)
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 1.0
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1.0
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

    }
}

extension SendViewController: DialogRecipientDeleteViewControllerDelegate {
    func didCancel() {
        //
    }
    func didDelete(_ index: Int) {
        recipients.remove(at: index)
        reloadSections([SendSection.recipient], animated: true)
    }
}

extension SendViewController: AssetsListTableViewControllerDelegate {
    func didSelect(assetId: String, index: Int?) {
        if let index = index {
            recipients[index].assetId = assetId
            reloadSections([SendSection.recipient], animated: false)
        }
    }
}

extension SendViewController: DialogQRCodeScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        if let index = index {
            recipients[index].address = value
            reloadSections([SendSection.recipient], animated: false)
        }
    }
    func didStop() {
        //
    }
}

extension SendViewController: DialogCustomFeeViewControllerDelegate {
    func didSave(fee: UInt64?) {
        feeEstimates[3] = fee ?? 1000
        customFee = feeEstimates[3] ?? 1000
    }
}
