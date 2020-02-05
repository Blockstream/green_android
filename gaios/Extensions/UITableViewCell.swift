import UIKit

extension UITableViewCell {
    @objc func selectable(_ selectable: Bool) {
        isUserInteractionEnabled = selectable
        for subview in contentView.subviews {
            subview.alpha = selectable ? 1 : 0.6
        }
    }
}
