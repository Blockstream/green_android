import UIKit

class DiscloseCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnDisclose: UIButton!
    var onTap: (() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        bg.cornerRadius = 5.0
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()

    }

    func configure(model: DiscloseCellModel,
                   onTap: (() -> Void)?) {
        self.onTap = onTap
        self.lblTitle.text = model.title
        self.lblHint.text = model.hint
    }

    @IBAction func onTap(_ sender: Any) {
        onTap?()
    }
}
