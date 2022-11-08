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
        var txtCache = ""
        for (idx, amount) in amounts.enumerated() {
            let registry = WalletManager.current?.registry
            let asset = registry?.info(for: amount.key)
            if let balance = Balance.fromSatoshi(amount.value, asset: asset) {
                let (value, denom) = balance.toValue()
                let txtRight = "\(value) \(denom)"
                var txtLeft = ""
                if idx == 0 {
                    txtLeft = model.status ?? ""
                    txtCache = txtLeft
                } else {
                    if txtCache != model.status ?? "" {
                        txtLeft = model.status ?? ""
                    }
                }
                addStackRow(MultiLabelViewModel(txtLeft: txtLeft, txtRight: txtRight, style: amount.value > 0 ? .amountIn : .amountOut ))
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
