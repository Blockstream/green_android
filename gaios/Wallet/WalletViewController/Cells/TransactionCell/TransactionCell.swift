import UIKit

class TransactionCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var innerStack: UIStackView!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        innerStack.subviews.forEach { $0.removeFromSuperview() }
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: TransactionCellModel) {
        self.imgView.image = model.icon

        var amounts = [(key: String, value: Int64)]()
        let feeAsset = WalletManager.current?.currentSession?.gdkNetwork.getFeeAsset()
        if model.tx.type == .redeposit,
           let feeAsset = feeAsset {
            amounts = [(key: feeAsset, value: -1 * Int64(model.tx.fee))]
        } else {
            amounts = Transaction.sort(model.tx.amounts)
            // remove L-BTC asset only if fee on outgoing transactions
            if model.tx.type == .some(.outgoing) || model.tx.type == .some(.mixed) {
                amounts = amounts.filter({ !($0.key == feeAsset && abs($0.value) == Int64(model.tx.fee)) })
            }
        }

        amounts.forEach {
            let registry = WalletManager.current?.registry
            let asset = registry?.info(for: $0.key)
            if let balance = Balance.fromSatoshi($0.value, asset: asset) {
                let (value, denom) = balance.toValue()
                let txtRight = "\(value) \(denom)"
                addStackRow(MultiLabelViewModel(txtLeft: model.status, txtRight: txtRight, style: $0.value > 0 ? .amountIn : .amountOut ))
            }
        }
        addStackRow(MultiLabelViewModel(txtLeft: model.date, txtRight: model.subaccountName, style: .simple))
    }

    func addStackRow(_ model: MultiLabelViewModel) {
        if let row = Bundle.main.loadNibNamed("MultiLabelView", owner: self, options: nil)?.first as? MultiLabelView {
            row.configure(model)
            innerStack.addArrangedSubview(row)
        }
    }

}
