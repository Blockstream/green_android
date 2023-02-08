import UIKit

class TransactionCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var progressBar: UIView!
    @IBOutlet weak var innerStack: UIStackView!
    @IBOutlet weak var progressWidth: NSLayoutConstraint!
    @IBOutlet weak var activity: UIActivityIndicatorView!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        progressBar.cornerRadius = 5.0
        progressBar.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
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

        var txtCache = ""
        let registry = WalletManager.current?.registry
        for (idx, amount) in model.amounts.enumerated() {
            let asset = registry?.info(for: amount.key)
            if let balance = Balance.fromSatoshi(amount.value, assetId: amount.key) {
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
        addStackRow(MultiLabelViewModel(txtLeft: model.statusUI().label,
                                        txtRight: model.subaccount?.localizedName() ?? "",
                                        style: model.statusUI().style))

        if !model.tx.memo.isEmpty {
            addStackRow(MultiLabelViewModel(txtLeft: model.tx.memo,
                                            txtRight: "",
                                            style: .simple))
        }

        progressWidth.constant = (UIScreen.main.bounds.width - 50.0) * (model.statusUI().progress ?? 0.0)
//        let unconf = model.statusUI().style == .unconfirmed
//        imgView.isHidden = unconf
//        activity.isHidden = !unconf
//        activity.startAnimating()
        activity.isHidden = true
    }

    func addStackRow(_ model: MultiLabelViewModel) {
        if let row = Bundle.main.loadNibNamed("MultiLabelView", owner: self, options: nil)?.first as? MultiLabelView {
            row.configure(model)
            innerStack.addArrangedSubview(row)
        }
    }

}
