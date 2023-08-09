import UIKit
import gdk

class TransactionCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var innerStack: UIStackView!
    @IBOutlet weak var activity: UIActivityIndicatorView!
    @IBOutlet weak var progressBar: UIProgressView!
    private var timer: Timer?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        progressBar.cornerRadius = 5.0
        progressBar.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
    }
    
    deinit {
        timer?.invalidate()
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        innerStack.subviews.forEach { $0.removeFromSuperview() }
        progressBar.progress = 0
        timer?.invalidate()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: TransactionCellModel, hideBalance: Bool) {
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
                addStackRow(MultiLabelViewModel(txtLeft: txtLeft,
                                                txtRight: txtRight,
                                                hideBalance: hideBalance,
                                                style: amount.value > 0 ? .amountIn : .amountOut ))
            }
        }
        addStackRow(MultiLabelViewModel(txtLeft: model.statusUI().label,
                                        txtRight: model.subaccount?.localizedName ?? "",
                                        hideBalance: nil,
                                        style: model.statusUI().style))

        if !(model.tx.memo?.isEmpty ?? true) {
            addStackRow(MultiLabelViewModel(txtLeft: model.tx.memo,
                                            txtRight: "",
                                            hideBalance: nil,
                                            style: .simple))
        }

        progressBar.progress = model.statusUI().progress ?? 0
        if model.statusUI().style == .unconfirmed {
            progressLoop()
        }
        activity.isHidden = true
    }

    func progressLoop() {
        var i: Float = 0
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.02, repeats: true) { timer in
            DispatchQueue.main.async {
                self.progressBar.progress = i.truncatingRemainder(dividingBy: 100) / 100
                i += 1
            }
        }
    }

    func addStackRow(_ model: MultiLabelViewModel) {
        if let row = Bundle.main.loadNibNamed("MultiLabelView", owner: self, options: nil)?.first as? MultiLabelView {
            row.configure(model)
            innerStack.addArrangedSubview(row)
        }
    }

}
