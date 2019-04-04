import Foundation
import UIKit
import PromiseKit
import NVActivityIndicatorView

enum TwoFactorCallError: Error {
    case failure(localizedDescription: String)
    case cancel(localizedDescription: String)
}

extension TwoFactorCall {

    func resolve(_ sender: UIViewController) -> Promise<[String: Any]> {
        func step() -> Promise<[String: Any]> {
            return Guarantee().map {
                try self.getStatus()!
            }.then { json in
                try self.resolving(sender: sender, json: json).map { _ in json }
            }.then { json -> Promise<[String: Any]> in
                guard let status = json["status"] as? String else { throw GaError.GenericError }
                if status == "done" {
                    return Promise<[String: Any]> { seal in seal.fulfill(json) }
                } else {
                    return step()
                }
            }
        }
        return step()
    }

    func resolving(sender: UIViewController, json: [String: Any]) throws -> Promise<Void> {
        guard let status = json["status"] as? String else { throw GaError.GenericError }
        switch status {
        case "done":
            return Guarantee().asVoid()
        case "error":
            let error = json["error"] as? String ?? ""
            throw TwoFactorCallError.failure(localizedDescription: NSLocalizedString(error, comment: ""))
        case "call":
            return try self.call()
        case "request_code":
            let methods = json["methods"] as? [String] ?? []
            if methods.count > 1 {
                let popup = PopupMethodResolver(sender)
                return Promise()
                    .map { sender.stopAnimating() }
                    .then { popup.method(methods) }
                    .map { method in sender.startAnimating(); return method }
                    .then { method in
                        try self.requestCode(method: method)
                    }
            } else {
                return try self.requestCode(method: methods[0])
            }
        case "resolve_code":
            let method = json["method"] as? String ?? ""
            let popup = PopupCodeResolver(sender)
            return Promise()
                .map { sender.stopAnimating() }
                .then { popup.code(method) }
                .map { code in sender.startAnimating(); return code }
                .then { code in
                    return try self.resolveCode(code: code)
                }
        default:
            return Guarantee().asVoid()
        }
    }
}

class PopupCodeResolver {
    private let viewController: UIViewController

    init(_ view: UIViewController) {
        self.viewController = view
    }

    func code(_ method: String) -> Promise<String> {
        return Promise { result in
            let methodDesc: String
            if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_google_authenticator" }
            let title = String(format: NSLocalizedString("id_please_provide_your_1s_code", comment: ""), NSLocalizedString(methodDesc, comment: ""))
            let alert = UIAlertController(title: title, message: "", preferredStyle: .alert)
            alert.addTextField { (textField) in
                textField.placeholder = ""
                textField.keyboardType = .numberPad
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
                result.reject(TwoFactorCallError.cancel(localizedDescription: NSLocalizedString("id_action_canceled", comment: "")))
            })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_next", comment: ""), style: .default) { (_: UIAlertAction) in
                let textField = alert.textFields![0]
                result.fulfill(textField.text!)
            })
            DispatchQueue.main.async {
                self.viewController.present(alert, animated: true, completion: nil)
            }
        }
    }
}

class PopupMethodResolver {
    let viewController: UIViewController

    init(_ view: UIViewController) {
        self.viewController = view
    }

    func method(_ methods: [String]) -> Promise<String> {
        return Promise { result in
            let alert = UIAlertController(title: NSLocalizedString("id_choose_twofactor_authentication", comment: ""), message: NSLocalizedString("id_choose_method_to_authorize_the", comment: ""), preferredStyle: .alert)
            methods.forEach { (method: String) in
                let methodDesc: String
                if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_google_authenticator" }
                alert.addAction(UIAlertAction(title: NSLocalizedString(methodDesc, comment: ""), style: .default) { (_: UIAlertAction) in
                    result.fulfill(method)
                })
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
                result.reject(TwoFactorCallError.cancel(localizedDescription: NSLocalizedString("id_action_canceled", comment: "")))
            })
            DispatchQueue.main.async {
                self.viewController.present(alert, animated: true, completion: nil)
            }
        }
    }
}
