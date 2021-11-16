import UIKit

class TransactionDetailCell: UITableViewCell {

    @IBOutlet weak var confirmationsView: UIView!
    @IBOutlet weak var lblConfirmationsTitle: UILabel!
    @IBOutlet weak var lblConfirmationsHint: UILabel!
    @IBOutlet weak var lblTxidTitle: UILabel!
    @IBOutlet weak var lblTxidHint: UILabel!
    @IBOutlet weak var btnExplorer: UIButton!
    @IBOutlet weak var lblNoteTitle: UILabel!
    @IBOutlet weak var lblNoteTxt: UILabel!
    @IBOutlet weak var bgNote: UIView!
    var noteAction: VoidToVoid?
    var explorerAction: VoidToVoid?

    override func awakeFromNib() {
        super.awakeFromNib()
        btnExplorer.setStyle(.outlined)
        bgNote.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblNoteTxt.text = ""
    }

    func configure(transaction: Transaction, noteAction: VoidToVoid?, explorerAction: VoidToVoid?) {
        lblConfirmationsTitle.text = "Confirmations"
        lblConfirmationsHint.text = "1234"
        lblTxidTitle.text = NSLocalizedString("id_transaction_id", comment: "")
        lblTxidHint.text = transaction.hash
        btnExplorer.setTitle("View in Explorer", for: .normal)
        lblNoteTitle.text = NSLocalizedString("id_my_notes", comment: "")
        if !transaction.memo.isEmpty {
            lblNoteTxt.text = transaction.memo
        }
        self.noteAction = noteAction
        self.explorerAction = explorerAction
    }

    @IBAction func btnCopy(_ sender: Any) {
        print("copy")
    }

    @IBAction func btnExplorer(_ sender: Any) {
        explorerAction?()
    }

    @IBAction func btnNote(_ sender: Any) {
        noteAction?()
    }
}
