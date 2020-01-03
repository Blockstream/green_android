import Foundation
import UIKit
import PromiseKit

enum TwoFactorCallError: Error {
    case failure(localizedDescription: String)
    case cancel(localizedDescription: String)
}

extension TwoFactorCall {

    func resolve(connected: @escaping() -> Bool = { true }) -> Promise<[String: Any]> {
        func step() -> Promise<[String: Any]> {
            return Guarantee().map {
                try self.getStatus()!
            }.then { json in
                try self.resolving(json: json, connected: connected).map { _ in json }
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

    private func resolving(json: [String: Any], connected: @escaping() -> Bool = { true }) throws -> Promise<Void> {
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
                let sender = UIApplication.shared.keyWindow?.rootViewController
                let popup = PopupMethodResolver(sender!)
                return Promise()
                    .map { sender?.stopAnimating() }
                    .then { popup.method(methods) }
                    .map { method in sender?.startAnimating(); return method }
                    .then { code in self.waitConnection(connected).map { return code} }
                    .then { method in
                        try self.requestCode(method: method)
                    }
            } else {
                return try self.requestCode(method: methods[0])
            }
        case "resolve_code":
            // Ledger interface resolver
            if let requiredData = json["required_data"] as? [String: Any] {
                let action = requiredData["action"] as? String
                let ledgerResolver = LedgerResolver()
                return Promise().then {_ -> Promise<String> in
                    if action == "get_xpubs" {
                        return ledgerResolver.getXpubs(requiredData)
                    } else if action == "sign_message" {
                        return ledgerResolver.signMessage(requiredData)
                    } else if action == "sign_tx" {
                        return ledgerResolver.signTransaction(requiredData)
                    } else {
                        throw GaError.GenericError
                    }
                }.then { code in
                    return try self.resolveCode(code: code)
                }
            }
            // User interface resolver
            let method = json["method"] as? String ?? ""
            let sender = UIApplication.shared.keyWindow?.rootViewController
            let popup = PopupCodeResolver(sender!)
            return Promise()
                .map { sender?.stopAnimating() }
                .then { popup.code(method) }
                .map { code in sender?.startAnimating(); return code }
                .then { code in self.waitConnection(connected).map { return code} }
                .then { code in
                    return try self.resolveCode(code: code)
                }
        default:
            return Guarantee().asVoid()
        }
    }

    func waitConnection(_ connected: @escaping() -> Bool = { true }) -> Promise<Void> {
        var attempts = 0
        func attempt() -> Promise<Void> {
            attempts += 1
            return Guarantee().map {
                if !connected() {
                    throw GaError.TimeoutError
                }
            }.recover { error -> Promise<Void> in
                guard attempts < 3 else { throw error }
                return after(DispatchTimeInterval.seconds(3)).then(on: nil, attempt)
            }
        }
        return attempt()
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
