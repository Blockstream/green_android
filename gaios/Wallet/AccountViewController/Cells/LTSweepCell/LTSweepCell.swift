import UIKit
import lightning
import gdk

class LTSweepCellModel {

    var amount: UInt64
    var denom: String

    var title: String {
        String(format: "id_you_can_sweep_s_of_your_funds".localized, "\(amount) \(denom)")
    }

    init(amount: UInt64, denom: String = "sats") {
        self.amount = amount
        self.denom = denom
    }
}

class LTSweepCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lbl: UILabel!
    @IBOutlet weak var btnMoreInfo: UIButton!
    var onInfo: (() -> Void)?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        lbl.textColor = UIColor.white.withAlphaComponent(0.7)
        lbl.font = UIFont.systemFont(ofSize: 12.0, weight: .semibold)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: LTSweepCellModel,
                   onInfo: (() -> Void)? = nil) {
        self.lbl.text = model.title
        btnMoreInfo.setTitle("id_sweep".localized, for: .normal)
        btnMoreInfo.setStyle(.inline)
        self.onInfo = onInfo
    }

    @IBAction func btnMoreInfo(_ sender: Any) {
        onInfo?()
    }
}
