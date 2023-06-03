import UIKit
import gdk

class TransactionDetailNoteCell: UITableViewCell {

    @IBOutlet weak var lblNoteTitle: UILabel!
    @IBOutlet weak var lblNoteTxt: UILabel!
    @IBOutlet weak var bgNote: UIView!

    var noteAction: VoidToVoid?

    override func awakeFromNib() {
        super.awakeFromNib()

        bgNote.layer.cornerRadius = 5.0
        lblNoteTitle.setStyle(.sectionTitle)
        lblNoteTxt.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblNoteTxt.text = ""
    }

    func configure(transaction: Transaction,
                   noteAction: VoidToVoid?) {

        lblNoteTitle.text = NSLocalizedString("id_my_notes", comment: "").lowercased().firstUppercased
        if !transaction.memo.isEmpty {
            lblNoteTxt.text = transaction.memo
        } else {
            lblNoteTxt.text = ""
        }
        self.noteAction = noteAction
    }

    @IBAction func btnNote(_ sender: Any) {
        noteAction?()
    }
}
