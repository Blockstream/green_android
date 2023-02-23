import Foundation
import UIKit
import PromiseKit

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
    var inputType: InputType = .transaction // for analytics
    var addressInputType: AnalyticsManager.AddressInputType = .paste // for analytics
    private var remoteAlert: RemoteAlert?

    override func viewDidLoad() {
        super.viewDidLoad()

        sliderView.delegate = self

        updateToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        setContent()

        view.accessibilityIdentifier = AccessibilityIdentifiers.SendConfirmScreen.view
        sliderView.accessibilityIdentifier = AccessibilityIdentifiers.SendConfirmScreen.viewSlider

        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")

        self.remoteAlert = RemoteAlertManager.shared.getAlert(screen: .sendConfirm, network: AccountsManager.shared.current?.networkName)

        AnalyticsManager.shared.recordView(.sendConfirm, sgmt: AnalyticsManager.shared.subAccSeg(AccountsManager.shared.current, walletType: viewModel.account.type))
    }

    func setContent() {
        title = NSLocalizedString("id_review", comment: "")
    }

    func editNote() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogNoteViewController") as? DialogNoteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.prefill = viewModel.tx.memo
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func remoteAlertDismiss() {
        remoteAlert = nil
        reloadSections([SendConfirmSection.remoteAlerts], animated: true)
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

    func dismissHWSummary() {
        presentedViewController?.dismiss(animated: false, completion: nil)
    }

    func send() {
        let account = AccountsManager.shared.current
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
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
            }
            return Guarantee()
        }.then(on: bgq) {
            self.viewModel.send()
        }.ensure {
            if account?.isHW ?? false {
                self.dismissHWSummary()
            }
            self.stopAnimating()
        }.done { _ in
            self.executeOnDone()
        }.catch { [weak self] error in

            if account?.isHW ?? false {
                self?.dismissHWSummary()
            }
            var prettyError: String?
            self?.sliderView.isUserInteractionEnabled = true
            self?.sliderView.reset()
            switch error {
            case JadeError.Abort(let desc),
                 JadeError.Declined(let desc):
                self?.showError(desc)
                prettyError = desc
            case LedgerWrapper.LedgerError.IOError,
                 LedgerWrapper.LedgerError.InvalidParameter:
                self?.showError(NSLocalizedString("id_operation_failure", comment: ""))
                prettyError = "id_operation_failure"
            case TwoFactorCallError.failure(let localizedDescription),
                 TwoFactorCallError.cancel(let localizedDescription):
                self?.showError(localizedDescription)
                prettyError = localizedDescription
            case TransactionError.invalid(let localizedDescription):
                self?.showError(localizedDescription)
                prettyError = localizedDescription
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                self?.showError(NSLocalizedString("id_you_are_not_connected", comment: ""))
                prettyError = "id_you_are_not_connected"
            default:
                self?.showError(error.localizedDescription)
                prettyError = error.localizedDescription
            }
            AnalyticsManager.shared.failedTransaction(account: AccountsManager.shared.current, error: error, prettyError: prettyError)
        }
    }

    func executeOnDone() {
        let isSendAll = viewModel.tx.sendAll
        let withMemo = !viewModel.tx.memo.isEmpty

        let transSgmt = AnalyticsManager.TransactionSegmentation(transactionType: inputType,
                                                     addressInputType: addressInputType,
                                                     sendAll: isSendAll)
        AnalyticsManager.shared.sendTransaction(account: AccountsManager.shared.current,
                               walletItem: viewModel.account,
                               transactionSgmt: transSgmt, withMemo: withMemo)

        self.startAnimating(message: NSLocalizedString("id_transaction_sent", comment: ""))
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 1.1) { [weak self] in
            self?.stopAnimating()
            self?.navigationController?.popToRootViewController(animated: true)

            StoreReviewHelper
                .shared
                .request(isSendAll: isSendAll,
                         account: AccountsManager.shared.current,
                         walletType: self?.viewModel.account.type)
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
}

extension SendConfirmViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return SendConfirmSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case SendConfirmSection.remoteAlerts.rawValue:
            return self.remoteAlert != nil ? 1 : 0
        case SendConfirmSection.addressee.rawValue:
            return viewModel.tx.addressees.count
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
        case SendConfirmSection.remoteAlerts.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell, let remoteAlert = self.remoteAlert {
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

extension SendConfirmViewController: DialogNoteViewControllerDelegate {

    func didSave(_ note: String) {
        viewModel.tx.memo = note
        reloadSections([SendConfirmSection.note], animated: false)
    }

    func didCancel() { }
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
