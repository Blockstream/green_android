import UIKit
import gdk

class TransactionDetailNoteCell: UITableViewCell {

    @IBOutlet weak var lblNoteTitle: UILabel!
    @IBOutlet weak var lblNoteTxt: UILabel!
    @IBOutlet weak var bgNote: UIView!
    @IBOutlet weak var btnNote: UIButton!
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

    func configure(title: String,
                   text: String?,
                   noteAction: VoidToVoid?) {

        lblNoteTitle.text = title.localized.lowercased().firstUppercased
        lblNoteTxt.text = text ?? ""
        self.noteAction = noteAction
        self.btnNote.isHidden = noteAction == nil
    }

    @IBAction func btnNote(_ sender: Any) {
        noteAction?()
    }
}
