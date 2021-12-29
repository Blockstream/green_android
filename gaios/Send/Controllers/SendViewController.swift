import Foundation
import UIKit
import PromiseKit

class SendViewController: KeyboardViewController {

    enum SendSection: Int, CaseIterable {
        case recipient = 0
        case addRecipient = 1
        case fee = 2
    }

    var wallet: WalletItem?

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!

    var recipients: [Recipient] = []

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

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        addRecipient()
    }

    func setContent() {
        title = NSLocalizedString("id_send", comment: "")
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func addRecipient() {
        let recipient = Recipient()
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
//        if content.sendAllFundsButton.isSelected {
//            content.amountTextField.text = NSLocalizedString("id_all", comment: "")
//            return
//        }
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
        let policyAsset = network?.policyAsset ?? "btc"

        let satoshi = recipient?.getSatoshi() ?? 0

        var addressee: [String: Any] = [:]
        addressee["address"] = addressInput
        addressee["satoshi"] = satoshi

        if network?.liquid ?? false && !isBip21 {
            addressee["asset_id"] = policyAsset
        }

        return addressee
    }

    func createTransaction() {
        let subaccount = self.wallet!.pointer
        let feeRate = getFeeEstimates()?.first ?? 1000

        self.startAnimating()
        let queue = DispatchQueue.global(qos: .default)
        firstly {
            return Guarantee()
        }.then(on: queue) {
            return try SessionManager.shared.getUnspentOutputs(details: ["subaccount": self.wallet?.pointer ?? 0, "num_confs": 0]).resolve()
        }.compactMap { data in
            let result = data["result"] as? [String: Any]
            let unspent = result?["unspent_outputs"] as? [String: Any]
            return ["addressees": [self.getAddressee()], "fee_rate": feeRate, "subaccount": subaccount, "utxos": unspent ?? [:]]
        }.then(on: queue) { data in
            try SessionManager.shared.createTransaction(details: data).resolve()
        }.done { data in
            let tx: Transaction = Transaction(data)
            print(tx)
            //got to next screen
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
/*
        Guarantee().compactMap { [unowned self] _ -> [String: Any] in

            // user input can be a bitcoin or liquid uri as well as an address
            var addressee: [String: Any] = ["address": userInput]
            if network?.liquid ?? false && !isBip21 {
                // insert dummy policy asset to validate address
                addressee["asset_id"] = policyAsset
            }
            return ["addressees": [addressee], "fee_rate": feeRate, "subaccount": subaccount, "utxos": [:]]

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
            // fetch utxos to create transaction
            return try SessionManager.shared.getUnspentOutputs(details: ["subaccount": self.wallet?.pointer ?? 0, "num_confs": 0]).resolve()
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
//            self.qrCodeReaderBackgroundView.startScan()
        }.finally {
            self.stopAnimating()
//            self.updateButton(!self.textView.text.isEmpty)
        }
 */
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
            return 1
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
                               walletItem: self.wallet)
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
                    vc.index = indexPath.row
                    vc.delegate = self
                    self?.present(vc, animated: false, completion: nil)
                }
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeCell") as? FeeCell {
                cell.configure(setCustomFee: setCustomFee)
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
    func didSave(fee: String, index: Int?) {
        print(fee, index ?? 0)
    }
}
