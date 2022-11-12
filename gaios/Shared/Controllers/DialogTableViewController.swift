import Foundation
import UIKit

protocol DialogTableViewControllerDelegate: AnyObject {
    func didSelect(_ action: String?)
}

class DialogTableViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var tableViewVerticalConstaint: NSLayoutConstraint!

    var titleText = ""
    var items = [String]()

    weak var delegate: DialogTableViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0

        view.accessibilityIdentifier = AccessibilityIdentifiers.DialogReceiveMoreOptionsScreen.view
    }

    func setContent() {
        lblTitle.text = titleText
    }

    func setStyle() {
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(nil)
    }

    func dismiss(_ action: Any?) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            self.delegate?.didSelect(action as? String)
        })
    }

}
extension DialogTableViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        tableViewVerticalConstaint.constant = CGFloat(items.count) * CGFloat(44)
        return items.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "BasicCell")
        cell?.textLabel?.text = items[indexPath.row]
        return cell ?? UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        dismiss(items[indexPath.row])
    }
}
