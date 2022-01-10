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

    override func viewDidLoad() {
        super.viewDidLoad()

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

    @IBAction func btnNext(_ sender: Any) {
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
                cell.configure(addressee: transaction?.addressees[indexPath.row])
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
