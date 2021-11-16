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

    override func awakeFromNib() {
        super.awakeFromNib()
        btnExplorer.setStyle(.outlined)
        bgNote.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(noteAction: VoidToVoid?) {
        lblConfirmationsTitle.text = "Confirmations"
        lblConfirmationsHint.text = "1234"
        lblTxidTitle.text = "Transaction ID"
        lblTxidHint.text = "0000000000000000000aaee5aed08f8e7bedd29634d51455cfe18a13ba5022b0"
        btnExplorer.setTitle("View in Explorer", for: .normal)
        lblNoteTitle.text = "My notes"
        lblNoteTxt.text = ""
        self.noteAction = noteAction
    }

    @IBAction func btnCopy(_ sender: Any) {
        print("copy")
    }

    @IBAction func btnExplorer(_ sender: Any) {
        print("explorer")
    }

    @IBAction func btnNote(_ sender: Any) {
        print("note")
        noteAction?()
    }
}
