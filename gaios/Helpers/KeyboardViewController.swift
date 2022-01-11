import Foundation
import UIKit

class KeyboardViewController: UIViewController {
    var keyboardDismissGesture: UIGestureRecognizer?

    private var showToken: NSObjectProtocol?
    private var hideToken: NSObjectProtocol?

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        showToken = NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main, using: keyboardWillShow)
        hideToken = NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main, using: keyboardWillHide)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = showToken {
            NotificationCenter.default.removeObserver(token)
            showToken = nil
        }
        if let token = hideToken {
            NotificationCenter.default.removeObserver(token)
            hideToken = nil
        }
    }

    func keyboardWillShow(notification: Notification) {
        if keyboardDismissGesture == nil {
            keyboardDismissGesture = UITapGestureRecognizer(target: self, action: #selector(KeyboardViewController.dismissKeyboard))
            view.addGestureRecognizer(keyboardDismissGesture!)
        }
    }

    func keyboardWillHide(notification: Notification) {
        if keyboardDismissGesture != nil {
            view.removeGestureRecognizer(keyboardDismissGesture!)
            keyboardDismissGesture = nil
        }
    }

    @objc func dismissKeyboard() {
        view.endEditing(true)
    }
}
