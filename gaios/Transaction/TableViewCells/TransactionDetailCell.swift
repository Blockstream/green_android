import UIKit

class TransactionDetailCell: UITableViewCell {

    @IBOutlet weak var lblTxidTitle: UILabel!
    @IBOutlet weak var lblTxidHint: UILabel!
    @IBOutlet weak var btnExplorer: UIButton!
    @IBOutlet weak var lblNoteTitle: UILabel!
    @IBOutlet weak var lblNoteTxt: UILabel!
    @IBOutlet weak var bgNote: UIView!
    @IBOutlet weak var copyIcon: UIImageView!

    var noteAction: VoidToVoid?
    var explorerAction: VoidToVoid?
    var copyHash: ((String) -> Void)?

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

    func configure(transaction: Transaction,
                   noteAction: VoidToVoid?,
                   explorerAction: VoidToVoid?,
                   copyHash: ((String) -> Void)?) {
        let color: UIColor = .white
        copyIcon.image = copyIcon.image?.maskWithColor(color: color)
<<<<<<< HEAD
=======
        lblConfirmationsTitle.text = NSLocalizedString("id_confirmations", comment: "")
        confirmationsView.isHidden = true // transaction.blockHeight == 0
        lblConfirmationsHint.text = "\(WalletManager.current?.currentSession?.notificationManager?.blockHeight ?? 0 - transaction.blockHeight + 1)"
>>>>>>> 68c5aa77 (unification: add active session in wallet manager)
        lblTxidTitle.text = NSLocalizedString("id_transaction_id", comment: "")
        lblTxidHint.text = transaction.hash
        btnExplorer.setTitle(NSLocalizedString("id_view_in_explorer", comment: ""), for: .normal)
        lblNoteTitle.text = NSLocalizedString("id_my_notes", comment: "")
        if !transaction.memo.isEmpty {
            lblNoteTxt.text = transaction.memo
        } else {
            lblNoteTxt.text = ""
        }
        self.noteAction = noteAction
        self.explorerAction = explorerAction
        self.copyHash = copyHash
    }

    @IBAction func btnCopy(_ sender: Any) {
        copyHash?( lblTxidHint.text ?? "" )
    }

    @IBAction func btnExplorer(_ sender: Any) {
        explorerAction?()
    }

    @IBAction func btnNote(_ sender: Any) {
        noteAction?()
    }
}
