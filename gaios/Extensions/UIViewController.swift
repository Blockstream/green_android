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
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in })
            self.present(alert, animated: true, completion: nil)
        }
    }

    func showAnalyticsConsent() {
        if AMan.S.consent == .notDetermined {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.3) {
                let storyboard = UIStoryboard(name: "Shared", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCountlyConsentViewController") as? DialogCountlyConsentViewController {
                    vc.modalPresentationStyle = .overFullScreen
                    self.present(vc, animated: true, completion: nil)
                }
            }
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
