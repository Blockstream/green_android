import Foundation
import UIKit

import BreezSDK
import gdk
import greenaddress
import hw

class SendConfirmViewController: KeyboardViewController {

    enum SendConfirmSection: Int, CaseIterable {
        case remoteAlerts = 0
        case addressee = 1
        case fee = 2
        case change = 3
        case note = 4
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var sliderView: SliderView!

    var viewModel: SendConfirmViewModel!

    private var connected = true
    private var updateToken: NSObjectProtocol?
    var inputType: TxType = .transaction // for analytics
    var addressInputType: AnalyticsManager.AddressInputType = .paste // for analytics

    override func viewDidLoad() {
        super.viewDidLoad()

        sliderView.delegate = self

        updateToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        setContent()

        view.accessibilityIdentifier = AccessibilityIdentifiers.SendConfirmScreen.view
        sliderView.accessibilityIdentifier = AccessibilityIdentifiers.SendConfirmScreen.viewSlider

        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")

        AnalyticsManager.shared.recordView(.sendConfirm, sgmt: AnalyticsManager.shared.subAccSeg(AccountsRepository.shared.current, walletItem: viewModel.account))
    }

    func setContent() {
        title = NSLocalizedString("id_review", comment: "")
    }

    func editNote() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogEditViewController") as? DialogEditViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.prefill = viewModel.tx.memo
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func remoteAlertDismiss() {
        viewModel.remoteAlert = nil
        reloadSections([SendConfirmSection.remoteAlerts], animated: true)
    }

    @MainActor
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

    func dismissHWSummary() {
        presentedViewController?.dismiss(animated: false, completion: nil)
    }

    func presentLightning() {
        let ltFlow = UIStoryboard(name: "LTFlow", bundle: nil)
        if let vc = ltFlow.instantiateViewController(withIdentifier: "LTConfirmingViewController") as? LTConfirmingViewController {
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
        }
    }

    func send() {
        let account = WalletManager.current?.account
        AnalyticsManager.shared.startSendTransaction()
        sliderView.isUserInteractionEnabled = false
        if account?.isHW ?? false {
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogSendHWSummaryViewController") as? DialogSendHWSummaryViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.transaction = viewModel.tx
                vc.isLedger = account?.isLedger ?? false
                vc.account = viewModel.account
                present(vc, animated: false, completion: nil)
            }
        } else if viewModel.isLightning {
            self.presentLightning()
        } else {
            viewModel.isLightning ? self.presentLightning() :  self.startAnimating()
        }
        Task {
            do {
                try await self.viewModel.send()
                executeOnDone()
            } catch {
                failure(error)
            }
        }
    }

    @MainActor
    func failure(_ error: Error) {
        let account = WalletManager.current?.account
        if account?.isHW ?? false {
            self.dismissHWSummary()
        }
        self.viewModel.isLightning ? self.dismiss(animated: true) : self.stopAnimating()
        self.sliderView.isUserInteractionEnabled = true
        self.sliderView.reset()
        let prettyError: String = {
            switch error {
            case BreezSDK.SdkError.Generic(let msg),
                BreezSDK.SdkError.LspConnectFailed(let msg),
                BreezSDK.SdkError.PersistenceFailure(let msg),
                BreezSDK.SdkError.ReceivePaymentFailed(let msg):
                return msg
            case HWError.Abort(let desc),
                HWError.Declined(let desc):
                return desc
            case BleLedgerConnection.LedgerError.IOError,
                BleLedgerConnection.LedgerError.InvalidParameter:
                return "id_operation_failure"
            case TwoFactorCallError.failure(let localizedDescription),
                TwoFactorCallError.cancel(let localizedDescription):
                return localizedDescription
            case TransactionError.invalid(let localizedDescription):
                return localizedDescription
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                return "id_you_are_not_connected"
            default:
                return error.localizedDescription
            }
        }()
        self.showBreezError(prettyError.localized)
        let isSendAll = self.viewModel.tx.sendAll
        let withMemo = !self.viewModel.tx.memo.isEmpty
        let transSgmt = AnalyticsManager.TransactionSegmentation(transactionType: self.inputType,
                                                                 addressInputType: self.addressInputType,
                                                                 sendAll: isSendAll)
        AnalyticsManager.shared.failedTransaction(account: AccountsRepository.shared.current,
                                                  walletItem: self.viewModel.account,
                                                  transactionSgmt: transSgmt, withMemo: withMemo, error: error, prettyError: prettyError)
        AnalyticsManager.shared.recordException(prettyError)
}

    @MainActor
    func showBreezError(_ message: String) {
        let ltFlow = UIStoryboard(name: "LTFlow", bundle: nil)
        if let vc = ltFlow.instantiateViewController(withIdentifier: "LTErrorViewController") as? LTErrorViewController {
            vc.delegate = self
            vc.errorStr = message
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
        }
    }
    
    @MainActor
    func executeOnDone() {
        let isSendAll = viewModel.tx.sendAll
        let withMemo = !viewModel.tx.memo.isEmpty
        let transSgmt = AnalyticsManager.TransactionSegmentation(transactionType: inputType,
                                                     addressInputType: addressInputType,
                                                     sendAll: isSendAll)
        AnalyticsManager.shared.endSendTransaction(account: AccountsRepository.shared.current,
                               walletItem: viewModel.account,
                               transactionSgmt: transSgmt, withMemo: withMemo)
        if viewModel.wm?.account.isHW ?? false {
            self.dismissHWSummary()
        } else if viewModel.isLightning {
            self.dismiss(animated: true)
        } else {
            self.startAnimating(message: NSLocalizedString("id_transaction_sent", comment: ""))
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 1.1) { [weak self] in
                self?.stopAnimating()
                self?.navigationController?.popToRootViewController(animated: true)
                StoreReviewHelper
                    .shared
                    .request(isSendAll: isSendAll,
                             account: AccountsRepository.shared.current,
                             walletItem: self?.viewModel.account)
            }
        }
    }

    func updateConnection(_ notification: Notification) {
        if let data = notification.userInfo,
              let json = try? JSONSerialization.data(withJSONObject: data, options: []),
              let connection = try? JSONDecoder().decode(Connection.self, from: json) {
            self.connected = connection.connected
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = updateToken {
            NotificationCenter.default.removeObserver(token)
            updateToken = nil
        }
    }

    func openFeedback(_ errorStr: String?) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogFeedbackViewController") as? DialogFeedbackViewController,
           let errorStr = errorStr, let nodeId = viewModel.nodeId() {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.isLightningScope = true
            vc.nodeId = nodeId
            vc.breezErrStr = errorStr
            present(vc, animated: false, completion: nil)
        }
    }
}

extension SendConfirmViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return SendConfirmSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case SendConfirmSection.remoteAlerts.rawValue:
            return viewModel.remoteAlert != nil ? 1 : 0
        case SendConfirmSection.addressee.rawValue:
            return viewModel.tx.addressees.count
        case SendConfirmSection.fee.rawValue:
            return viewModel.account.type == .lightning ? 0 : 1
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
        case SendConfirmSection.remoteAlerts.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell, let remoteAlert = viewModel.remoteAlert {
                cell.configure(AlertCardCellModel(type: .remoteAlert(remoteAlert)),
                                   onLeft: nil,
                                   onRight: (remoteAlert.link ?? "" ).isEmpty ? nil : { () in
                    SafeNavigationManager.shared.navigate(remoteAlert.link)
                },
                                   onDismiss: {[weak self] in
                                 self?.remoteAlertDismiss()
                    })
                cell.selectionStyle = .none
                return cell
            }
        case SendConfirmSection.addressee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AddresseeCell") as? AddresseeCell {
                cell.configure(cellModel: viewModel.addresseeCellModels[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        case SendConfirmSection.fee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeSummaryCell") as? FeeSummaryCell {
                cell.configure(viewModel.tx)
                cell.selectionStyle = .none
                return cell
            }
        case SendConfirmSection.change.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ChangeCell") as? ChangeCell {
                cell.configure(viewModel.tx)
                cell.selectionStyle = .none
                return cell
            }
        case SendConfirmSection.note.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "NoteCell") as? NoteCell {
                cell.configure(note: viewModel.tx.memo)
                cell.delegate = self
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

extension SendConfirmViewController: DialogEditViewControllerDelegate {

    func didSave(_ note: String) {
        viewModel.tx.memo = note
        reloadSections([SendConfirmSection.note], animated: false)
    }

    func didClose() { }
}

extension SendConfirmViewController: NoteCellDelegate {

    func noteAction() {
        editNote()
    }
}

extension SendConfirmViewController: SliderViewDelegate {
    func sliderThumbIsMoving(_ sliderView: SliderView) {
    }

    func sliderThumbDidStopMoving(_ position: Int) {
        if position == 1 {
            send()
        }
    }
}

extension SendConfirmViewController: LTErrorViewControllerDelegate {
    func onReport(_ errorStr: String?) {
        openFeedback(errorStr)
    }
    
    func onDone() {
        //
    }
}

extension SendConfirmViewController: DialogFeedbackViewControllerDelegate {
    func didSend(rating: Int, email: String?, comment: String) {
        AnalyticsManager.shared.recordFeedback(rating: rating, email: email, comment: comment)
        DropAlert().info(message: "id_thank_you_for_your_feedback".localized)
    }
    
    func didCancel() {
        //
    }
}
