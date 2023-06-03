import UIKit

class LTInboundCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblInbound: UILabel!
    @IBOutlet weak var btnMoreInfo: UIButton!
    var onInboundInfo: (() -> Void)?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        lblInbound.textColor = UIColor.white.withAlphaComponent(0.7)
        lblInbound.font = UIFont.systemFont(ofSize: 12.0, weight: .semibold)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: LTInboundCellModel,
                   onInboundInfo: (() -> Void)? = nil) {
        self.lblInbound.text = model.title
        btnMoreInfo.setTitle("id_more_info".localized, for: .normal)
        btnMoreInfo.setStyle(.inline)
        self.onInboundInfo = onInboundInfo
    }

    @IBAction func btnMoreInfo(_ sender: Any) {
        onInboundInfo?()
    }
}
