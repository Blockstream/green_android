import UIKit

protocol NoteCellDelegate: AnyObject {
    func noteAction()
}

class NoteCell: UITableViewCell {

    @IBOutlet weak var lblNoteTitle: UILabel!
    @IBOutlet weak var lblNoteTxt: UILabel!
    @IBOutlet weak var bgNote: UIView!

    weak var delegate: NoteCellDelegate?
    var isLightning = false

    override func awakeFromNib() {
        super.awakeFromNib()
        lblNoteTitle.text = (NSLocalizedString("id_my_notes", comment: "").lowercased()).firstUppercased
        bgNote.layer.cornerRadius = 5.0
    }

    override func prepareForReuse() {
        lblNoteTxt.text = ""
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(note: String, isLightning: Bool) {
        self.lblNoteTxt.text = note
        self.isLightning = isLightning
        if isLightning {
            lblNoteTxt.alpha = 0.7
        }
    }

    @IBAction func btnNote(_ sender: Any) {
        if !isLightning {
            delegate?.noteAction()
        }
    }
}
