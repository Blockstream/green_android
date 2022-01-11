import Foundation
import UIKit
import PromiseKit

class SendConfirmViewController: KeyboardViewController {

    enum SendConfirmSection: Int, CaseIterable {
        case addressee = 0
        case fee = 1
        case change = 2
        case note = 3
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!

    var wallet: WalletItem?
    var transaction: Transaction?
    private var connected = true
    private var updateToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()

        updateToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        setContent()
        setStyle()
    }

    func setContent() {
        title = NSLocalizedString("id_review", comment: "")
        btnNext.setTitle(NSLocalizedString("id_send", comment: ""), for: .normal)
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func editNote() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogNoteViewController") as? DialogNoteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.prefill = transaction?.memo
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func reloadSections(_ sections: [SendConfirmSection], animated: Bool) {
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

    func send() {
        guard let transaction = transaction else { return }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            btnNext.isUserInteractionEnabled = false
            let account = AccountsManager.shared.current
            if account?.isHW ?? false {
                DropAlert().success(message: NSLocalizedString("id_please_follow_the_instructions", comment: ""), delay: 4)
            }
            return Guarantee()
        }.then(on: bgq) {
            signTransaction(transaction: transaction)
        }.then(on: bgq) { call in
            call.resolve(connected: {
                return self.connected
            })
        }.map(on: bgq) { resultDict in
            let result = resultDict["result"] as? [String: Any]
            if transaction.isSweep {
                let tx = result!["transaction"] as? String
                _ = try SessionManager.shared.broadcastTransaction(tx_hex: tx!)
                return nil
            } else {
                return try SessionManager.shared.sendTransaction(details: result!)
            }
        }.then(on: bgq) { (call: TwoFactorCall?) -> Promise<[String: Any]> in
            call?.resolve(connected: {
                return self.connected }) ?? Promise<[String: Any]> { seal in seal.fulfill([:]) }
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.executeOnDone()
        }.catch { error in
            self.btnNext.isUserInteractionEnabled = true
            switch error {
            case JadeError.Abort(let desc),
                 JadeError.Declined(let desc):
                self.showError(desc)
            case LedgerWrapper.LedgerError.IOError,
                 LedgerWrapper.LedgerError.InvalidParameter:
                self.showError(NSLocalizedString("id_operation_failure", comment: ""))
            case TwoFactorCallError.failure(let localizedDescription),
                 TwoFactorCallError.cancel(let localizedDescription):
                self.showError(localizedDescription)
            default:
                self.showError(error.localizedDescription)
            }
        }
    }

    func executeOnDone() {
        self.startAnimating(message: NSLocalizedString("id_transaction_sent", comment: ""))
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 1.1) {
            self.stopAnimating()
            self.navigationController?.popToRootViewController(animated: true)
        }
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        self.connected = connected ?? false
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = updateToken {
            NotificationCenter.default.removeObserver(token)
            updateToken = nil
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        send()
    }
}

extension SendConfirmViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return SendConfirmSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case SendConfirmSection.addressee.rawValue:
            return transaction?.addressees.count ?? 0
        case SendConfirmSection.fee.rawValue:
            return 1
        case SendConfirmSection.change.rawValue:
            return 0
        case SendConfirmSection.note.rawValue:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case SendConfirmSection.addressee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AddresseeCell") as? AddresseeCell {
                cell.configure(addressee: transaction?.addressees[indexPath.row], isSendAll: transaction?.sendAll ?? false)
                cell.selectionStyle = .none
                return cell
            }
        case SendConfirmSection.fee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeSummaryCell") as? FeeSummaryCell, let tx = self.transaction {
                cell.configure(tx)
            cell.selectionStyle = .none
            return cell
        }
        case SendConfirmSection.change.rawValue:
            break
        case SendConfirmSection.note.rawValue:
            let noteAction: VoidToVoid? = { [weak self] in
                self?.editNote()
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "NoteCell") as? NoteCell {
                cell.configure(note: transaction?.memo ?? "", noteAction: noteAction)
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

extension SendConfirmViewController: DialogNoteViewControllerDelegate {

    func didSave(_ note: String) {
        transaction?.memo = note
        reloadSections([SendConfirmSection.note], animated: false)
    }

    func didCancel() { }
}
