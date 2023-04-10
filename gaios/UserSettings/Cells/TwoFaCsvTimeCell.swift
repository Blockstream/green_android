import UIKit
import gdk

class TwoFaCsvTimeCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgRadio: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.font = UIFont.systemFont(ofSize: 16.0, weight: .bold)
        lblTitle.textColor = .white
        lblHint.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        lblHint.textColor = .white.withAlphaComponent(0.6)
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(item: Settings.CsvTime, current: Int?, gdkNetwork: GdkNetwork) {
        self.lblTitle.text = item.label()
        self.lblHint.text = item.description()
        if current == item.value(for: gdkNetwork) {
            self.imgRadio?.image = UIImage(named: "selected_circle")!
        } else {
            self.imgRadio?.image = UIImage(named: "unselected_circle")!
        }
    }
}
