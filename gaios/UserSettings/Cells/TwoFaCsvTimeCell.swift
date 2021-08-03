import UIKit

class TwoFaCsvTimeCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgRadio: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        lblTitle.textColor = .white
        lblHint.textColor = UIColor.customGrayLight()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(item: Settings.CsvTime, current: Int?) {
        self.lblTitle.text = item.label()
        self.lblHint.text = item.description()
        if current == item.value() {
            self.imgRadio?.image = UIImage(named: "selected_circle")!
        } else {
            self.imgRadio?.image = UIImage(named: "unselected_circle")!
        }
    }
}
