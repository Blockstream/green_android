import UIKit

class TransactionStatusCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblDate: UILabel!
    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var iconCheck: UIImageView!
    @IBOutlet weak var lblStep: UILabel!
    @IBOutlet weak var arc: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.borderWidth = 1.0
        bg.layer.borderColor = UIColor.gray.cgColor
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(num: Int) {
        lblDate.text = "JUN 22, 2018 - 12:12:!2"
        lblStatus.text = "Pending Confirmation"
        lblStep.text = "\(num)/6"
        arc.subviews.forEach { $0.removeFromSuperview() }
        arc.addSubview(ArcView(frame: arc.frame, num: num))
        iconCheck.isHidden = num != 6
        lblStep.isHidden = num == 6
    }
}
