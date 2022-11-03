import UIKit

class BaseCell: UITableViewCell {

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        if selected {
            UIView.animate(withDuration: 0.1, animations: { [weak self] in
                self?.alpha = 0.6
            }, completion: { [weak self] _ in
                UIView.animate(withDuration: 0.2) {
                    self?.alpha = 1.0
                }
            })
        }
    }

}
