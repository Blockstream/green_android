import UIKit

class OverviewAccountIdCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnAction: UIButton!

    var onAction:(() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.layer.borderWidth = 1.0
        bg.layer.borderColor = UIColor.customGrayLight().cgColor
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(onAction: VoidToVoid?) {
        lblTitle.text = NSLocalizedString("id_amp_id", comment: "")
        btnAction.setTitle("  " + "GET ID" + "  ", for: .normal)
        btnAction.setStyle(.primary)
        self.onAction = onAction
    }

    @IBAction func btnAction(_ sender: Any) {
        onAction?()
    }
}
