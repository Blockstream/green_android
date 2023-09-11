import Foundation

import UIKit
import gdk
import greenaddress
import hw

class ResolverManager {
    
    let resolver: GDKResolver
    let session: SessionManager?
    
    init(_ factor: TwoFactorCall?, chain: String, connected: @escaping() -> Bool = { true }, hwDevice: HWProtocol?, session: SessionManager? = nil) {
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
            codeCustomDialog(method)
        }
    }

    func codeCustomDialog(_ method: String) {
        let methodDesc: String
        if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
        
        let twoFAFlow = UIStoryboard(name: "TwoFAFlow", bundle: nil)
        guard let vc = twoFAFlow.instantiateViewController(withIdentifier: "TwoFAViewController") as? TwoFAViewController else { return }
            
        vc.commontitle = methodDesc
        
        vc.onCancel = { [weak self] in
            if let appDelegate = UIApplication.shared.delegate as? AppDelegate{
                appDelegate.resolveControllerOff()
            }
            self?.textContinuation?.resume(throwing: TwoFactorCallError.cancel(localizedDescription: "id_action_canceled".localized))
        }
        
        vc.onCode = { [weak self] code in
            if let appDelegate = UIApplication.shared.delegate as? AppDelegate{
                appDelegate.resolveControllerOff()
            }
            self?.textContinuation?.resume(returning: code)
            DispatchQueue.main.async {
                UIApplication.topViewController()?.startAnimating()
            }
        }
        
        DispatchQueue.main.async {
            if let appDelegate = UIApplication.shared.delegate as? AppDelegate{
                appDelegate.resolveControllerOn(vc)
            }
        }
    }

    func method(_ methods: [String]) async throws -> String {
        DispatchQueue.main.async {
            UIApplication.topViewController()?.stopAnimating()
        }
        return try await withCheckedThrowingContinuation { continuation in
            textContinuation = continuation
            methodCustomDialog(methods)
        }
    }
    func methodCustomDialog(_ methods: [String]) {

        let twoFAFlow = UIStoryboard(name: "TwoFAFlow", bundle: nil)
        guard let vc = twoFAFlow.instantiateViewController(withIdentifier: "TwoFAMethodViewController") as? TwoFAMethodViewController else { return }
            
        vc.methods = methods
        
        vc.onCancel = { [weak self] in
            if let appDelegate = UIApplication.shared.delegate as? AppDelegate{
                appDelegate.resolveControllerOff()
            }
            self?.textContinuation?.resume(throwing: TwoFactorCallError.cancel(localizedDescription: "id_action_canceled".localized))
        }
        
        vc.onType = { [weak self] tfType in
            if let appDelegate = UIApplication.shared.delegate as? AppDelegate{
                appDelegate.resolveControllerOff()
            }
            self?.textContinuation?.resume(returning: tfType.rawValue)
            DispatchQueue.main.async {
                UIApplication.topViewController()?.startAnimating()
            }
        }
        
        DispatchQueue.main.async {
            if let appDelegate = UIApplication.shared.delegate as? AppDelegate{
                appDelegate.resolveControllerOn(vc)
            }
        }
    }
//    func methodDialog(_ methods: [String]) {
//        let alert = UIAlertController(title: NSLocalizedString("id_choose_twofactor_authentication", comment: ""), message: NSLocalizedString("id_choose_method_to_authorize_the", comment: ""), preferredStyle: .alert)
//        methods.forEach { (method: String) in
//            let methodDesc: String
//            if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
//            alert.addAction(UIAlertAction(title: NSLocalizedString(methodDesc, comment: ""), style: .default) { (_: UIAlertAction) in
//                self.textContinuation?.resume(returning: method)
//                DispatchQueue.main.async {
//                    UIApplication.topViewController()?.startAnimating()
//                }
//            })
//        }
//        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
//            self.textContinuation?.resume(throwing: TwoFactorCallError.cancel(localizedDescription: "id_action_canceled".localized))
//        })
//        DispatchQueue.main.async {
//            UIApplication.topViewController()?.present(alert, animated: true, completion: nil)
//        }
//    }

    func textFieldDidChangeSelection(_ textField: UITextField) {
        DispatchQueue.main.async {
            if textField.text?.count == 6 {
                UIApplication.topViewController()?.dismiss(animated: true)
            }
        }
    }
}
