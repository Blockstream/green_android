import Foundation
import PromiseKit
import UIKit
import gdk
import greenaddress
import hw

class ResolverManager {
    
    let resolver: GDKResolver
    
    init(_ factor: TwoFactorCall, chain: String, connected: @escaping() -> Bool = { true }) {
        resolver = GDKResolver(factor,
                               popupDelegate: PopupResolver(),
                               hwDelegate: HWResolver(),
                               chain: chain,
                               connected: connected)
        
    }

    func run() -> Promise<[String: Any]> {
        return resolver.resolve()
            .get { _ in
                DispatchQueue.main.async {
                    UIApplication.topViewController()?.stopAnimating()
                }
            }
    }
}

class CodeAlertController: UIAlertController {

    var willDisappearBlock: ((UIAlertController) -> Void)?
    var didDisappearBlock: ((UIAlertController) -> Void)?

    override func viewWillDisappear(_ animated: Bool) {
        willDisappearBlock?(self)
        super.viewWillDisappear(animated)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if self.textFields?.first?.text?.count == 6 {
            didDisappearBlock?(self)
        }
    }
}

class PopupResolver: NSObject, UITextFieldDelegate, PopupResolverDelegate {

    func code(_ method: String) -> Promise<String> {
        DispatchQueue.main.async {
            UIApplication.topViewController()?.stopAnimating()
        }
        return Promise { result in
            let methodDesc: String
            if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
            let title = String(format: NSLocalizedString("id_please_provide_your_1s_code", comment: ""), NSLocalizedString(methodDesc, comment: ""))
            let alert = CodeAlertController(title: title, message: "", preferredStyle: .alert)
            alert.addTextField {[weak self] (textField: UITextField!) in
                textField.placeholder = ""
                textField.keyboardType = .numberPad
                textField.delegate = self
                //textField.addTarget(self, action: #selector(alert.codeResolverTextFieldDidChange), for: .editingChanged)
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
                result.reject(TwoFactorCallError.cancel(localizedDescription: NSLocalizedString("id_action_canceled", comment: "")))
            })
            alert.didDisappearBlock = {[weak alert] _ in
                if alert?.textFields?.first?.text?.count == 6 {
                    result.fulfill((alert?.textFields?.first?.text)!)
                    DispatchQueue.main.async {
                        UIApplication.topViewController()?.startAnimating()
                    }
                }
            }
            DispatchQueue.main.async {
                UIApplication.topViewController()?.present(alert, animated: true, completion: nil)
            }
        }
    }

    func method(_ methods: [String]) -> Promise<String> {
        DispatchQueue.main.async {
            UIApplication.topViewController()?.stopAnimating()
        }
        return Promise { result in
            let alert = UIAlertController(title: NSLocalizedString("id_choose_twofactor_authentication", comment: ""), message: NSLocalizedString("id_choose_method_to_authorize_the", comment: ""), preferredStyle: .alert)
            methods.forEach { (method: String) in
                let methodDesc: String
                if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
                alert.addAction(UIAlertAction(title: NSLocalizedString(methodDesc, comment: ""), style: .default) { (_: UIAlertAction) in
                    result.fulfill(method)
                    DispatchQueue.main.async {
                        UIApplication.topViewController()?.startAnimating()
                    }
                })
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
                result.reject(TwoFactorCallError.cancel(localizedDescription: NSLocalizedString("id_action_canceled", comment: "")))
            })
            DispatchQueue.main.async {
                UIApplication.topViewController()?.present(alert, animated: true, completion: nil)
            }
        }
    }

    func textFieldDidChangeSelection(_ textField: UITextField) {
        DispatchQueue.main.async {
            if textField.text?.count == 6 {
                UIApplication.topViewController()?.dismiss(animated: true)
            }
        }
    }
}
