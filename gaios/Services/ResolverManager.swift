import Foundation

import UIKit
import gdk
import greenaddress
import hw

class ResolverManager {
    
    let resolver: GDKResolver
    let session: SessionManager?
    
    init(_ factor: TwoFactorCall, chain: String, connected: @escaping() -> Bool = { true }, hwDevice: HWProtocol?, session: SessionManager? = nil) {
        self.session = session
        resolver = GDKResolver(factor,
                               popupDelegate: PopupResolver(),
                               hwDelegate: HWResolver(),
                               hwDevice: hwDevice,
                               chain: chain,
                               connected: connected
        )
        
    }

    func run() async throws -> [String: Any]? {
        let res = try await resolver.resolve()
        DispatchQueue.main.async {
            UIApplication.topViewController()?.stopAnimating()
        }
        return res
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

    private var textContinuation: CheckedContinuation<String, Error>?
    
    func code(_ method: String) async throws -> String {
        DispatchQueue.main.async {
            UIApplication.topViewController()?.stopAnimating()
        }
        return try await withCheckedThrowingContinuation { continuation in
            textContinuation = continuation
            codeDialog(method)
        }
    }

    func codeDialog(_ method: String) {
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
            self.textContinuation?.resume(throwing: TwoFactorCallError.cancel(localizedDescription: "id_action_canceled".localized))
        })
        alert.didDisappearBlock = {[weak alert] _ in
            if let text = alert?.textFields?.first?.text, text.count == 6 {
                self.textContinuation?.resume(returning: text)
                DispatchQueue.main.async {
                    UIApplication.topViewController()?.startAnimating()
                }
            }
        }
        DispatchQueue.main.async {
            UIApplication.topViewController()?.present(alert, animated: true, completion: nil)
        }
    }

    func method(_ methods: [String]) async throws -> String {
        DispatchQueue.main.async {
            UIApplication.topViewController()?.stopAnimating()
        }
        return try await withCheckedThrowingContinuation { continuation in
            textContinuation = continuation
            methodDialog(methods)
        }
    }
    func methodDialog(_ methods: [String]) {
        let alert = UIAlertController(title: NSLocalizedString("id_choose_twofactor_authentication", comment: ""), message: NSLocalizedString("id_choose_method_to_authorize_the", comment: ""), preferredStyle: .alert)
        methods.forEach { (method: String) in
            let methodDesc: String
            if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
            alert.addAction(UIAlertAction(title: NSLocalizedString(methodDesc, comment: ""), style: .default) { (_: UIAlertAction) in
                self.textContinuation?.resume(returning: method)
                DispatchQueue.main.async {
                    UIApplication.topViewController()?.startAnimating()
                }
            })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
            self.textContinuation?.resume(throwing: TwoFactorCallError.cancel(localizedDescription: "id_action_canceled".localized))
        })
        DispatchQueue.main.async {
            UIApplication.topViewController()?.present(alert, animated: true, completion: nil)
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
