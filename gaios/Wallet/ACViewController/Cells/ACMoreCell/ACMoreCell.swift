import UIKit

class ACMoreCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var btnMore: UIButton!

    var onTap: (() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.borderWidth = 2.0
        bg.borderColor = .white
        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(onTap: (() -> Void)?) {
        btnMore.setTitle("See all transactions", for: .normal)
        self.onTap = onTap
    }

    @IBAction func btnMore(_ sender: Any) {
        onTap?()
    }
}
