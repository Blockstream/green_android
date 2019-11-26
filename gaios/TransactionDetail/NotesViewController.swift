import UIKit
import PromiseKit

class NotesViewController: UIViewController {

    @IBOutlet weak var dismissButton: UIButton!
    @IBOutlet weak var saveButton: UIButton!
    @IBOutlet weak var memoTextView: UITextView!

    var transaction: Transaction!
    var updateTransaction: (_ transaction: Transaction) -> Void = { _ in }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        saveButton.isHidden = true
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        if !transaction.memo.isEmpty {
            memoTextView.text = transaction.memo
        }
        memoTextView.delegate = self
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        memoTextView.becomeFirstResponder()
    }

    @IBAction func dismissButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }

    @IBAction func saveButtonTapped(_ sender: Any) {
        guard let memo = memoTextView.text else { return }
        saveButton.isEnabled = false
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) { _ in
            try gaios.getSession().setTransactionMemo(txhash_hex: self.transaction.hash, memo: memo, memo_type: 0)
            self.transaction.memo = memo
            }.ensure {
                self.saveButton.isEnabled = true
            }.done {
                self.saveButton.isHidden = true
                self.view.endEditing(true)
                self.dismiss(animated: true) {
                    self.updateTransaction(self.transaction)
                }
            }.catch { _ in}
    }
}

extension NotesViewController: UITextViewDelegate {

    func textViewDidChangeSelection(_ textView: UITextView) {
        saveButton.isHidden = !(textView.text?.count ?? 0 > 0)
    }
}
