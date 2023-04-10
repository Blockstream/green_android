import UIKit
import gdk

class TransactionDetailCell: UITableViewCell {

    @IBOutlet weak var lblTxidTitle: UILabel!
    @IBOutlet weak var lblTxidHint: UILabel!
    @IBOutlet weak var btnExplorer: UIButton!
    @IBOutlet weak var bgArea: UIView!

    var noteAction: VoidToVoid?
    var explorerAction: VoidToVoid?
    var copyHash: ((String) -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
        bgArea.layer.cornerRadius = 5.0
        lblTxidTitle.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        lblTxidTitle.textColor = .white.withAlphaComponent(0.4)
        lblTxidHint.font = UIFont.systemFont(ofSize: 16.0, weight: .semibold)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblTxidTitle.text = ""
    }

    func configure(transaction: Transaction,
                   explorerAction: VoidToVoid?,
                   copyHash: ((String) -> Void)?) {
        lblTxidTitle.text = NSLocalizedString("id_transaction_id", comment: "")
        lblTxidHint.text = transaction.hash
        self.explorerAction = explorerAction
        self.copyHash = copyHash
    }

    @IBAction func btnCopy(_ sender: Any) {
        copyHash?( lblTxidHint.text ?? "" )
    }

    @IBAction func btnExplorer(_ sender: Any) {
        explorerAction?()
    }
}
