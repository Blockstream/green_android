import UIKit
extension UITableView {
    func reloadData(completion:@escaping () -> Void) {
        UIView.animate(withDuration: 0, animations: reloadData) { _ in completion() }
    }
}
