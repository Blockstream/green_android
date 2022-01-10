import UIKit

class NoteCell: UITableViewCell {

    @IBOutlet weak var lblNoteTitle: UILabel!
    @IBOutlet weak var lblNoteTxt: UILabel!
    @IBOutlet weak var bgNote: UIView!
    var noteAction: VoidToVoid?

    override func awakeFromNib() {
        super.awakeFromNib()
        lblNoteTitle.text = NSLocalizedString("id_my_notes", comment: "")
        bgNote.layer.cornerRadius = 5.0
    }

    override func prepareForReuse() {
        lblNoteTxt.text = ""
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(note: String, noteAction: VoidToVoid?) {
        self.noteAction = noteAction
        self.lblNoteTxt.text = note
    }

    @IBAction func btnNote(_ sender: Any) {
        noteAction?()
    }
}
