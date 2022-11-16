import Foundation
import UIKit

extension UIViewController {

    func showAlert(title: String, message: String) {
        DispatchQueue.main.async {
            let alert = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in })
            self.present(alert, animated: true, completion: nil)
        }
    }

    func showError(_ message: String) {
        DispatchQueue.main.async {
            let alert = UIAlertController(title: NSLocalizedString("id_error", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in })
            self.present(alert, animated: true, completion: nil)
        }
    }

    func showError(_ err: Error) {
        DispatchQueue.main.async {
            let alert = UIAlertController(title: NSLocalizedString("id_error", comment: ""), message: self.getError(err), preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in })
            self.present(alert, animated: true, completion: nil)
        }
    }

    func getError(_ err: Error) -> String {
        switch err {
        case AuthenticationTypeHandler.AuthError.CanceledByUser, AuthenticationTypeHandler.AuthError.SecurityError, AuthenticationTypeHandler.AuthError.KeychainError:
            return err.localizedDescription
        case LoginError.connectionFailed:
            return "id_connection_failed"
        case LoginError.walletNotFound:
            return "id_wallet_not_found"
        case GaError.NotAuthorizedError:
            return "NotAuthorizedError"
        case GaError.GenericError(let txt):
            return txt ?? "id_operation_failed"
        case TwoFactorCallError.cancel(let txt):
            return txt
        case TwoFactorCallError.failure(let txt):
            return txt
        default:
            return err.localizedDescription
        }
    }
}

extension UIViewController {
    func hideKeyboardWhenTappedAround() {
        let tap = UITapGestureRecognizer(target: self, action: #selector(UIViewController.dismissKeyboardTappedAround))
        tap.cancelsTouchesInView = false
        view.addGestureRecognizer(tap)
    }

    @objc func dismissKeyboardTappedAround() {
        view.endEditing(true)
    }
}

@nonobjc extension UIViewController {
    func add(_ child: UIViewController, frame: CGRect? = nil) {
        addChild(child)

        if let frame = frame {
            child.view.frame = frame
        }

        view.addSubview(child.view)
        child.didMove(toParent: self)
    }

    func remove() {
        willMove(toParent: nil)
        view.removeFromSuperview()
        removeFromParent()
    }
}
