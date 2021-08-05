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
            let bgq = DispatchQueue.global(qos: .background)
            return Guarantee().map(on: bgq) {
                try self.getStatus()!
            }.then { json in
                try self.resolving(json: json, connected: connected).map { _ in json }
            }.then(on: bgq) { json -> Promise<[String: Any]> in
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
        let bgq = DispatchQueue.global(qos: .background)
        switch status {
        case "done":
            return Guarantee().asVoid()
        case "error":
            let error = json["error"] as? String ?? ""
            throw TwoFactorCallError.failure(localizedDescription: NSLocalizedString(error, comment: ""))
        case "call":
            return Promise().map(on: bgq) { try self.call() }
        case "request_code":
            let methods = json["methods"] as? [String] ?? []
            if methods.count > 1 {
                let sender = UIApplication.topViewController()
                let popup = PopupMethodResolver(sender!)
                return Promise()
                    .map { sender?.stopAnimating() }
                    .then { popup.method(methods) }
                    .map { method in sender?.startAnimating(); return method }
                    .then(on: bgq) { code in self.waitConnection(connected).map { return code} }
                    .map(on: bgq) { method in try self.requestCode(method: method) }
            } else {
                return Promise().map(on: bgq) { try self.requestCode(method: methods[0]) }
            }
        case "resolve_code":
            // Ledger interface resolver
            if let requiredData = json["required_data"] as? [String: Any] {
                let action = requiredData["action"] as? String
                return Promise().then(on: bgq) {_ -> Promise<String> in
                    switch action {
                    case "get_xpubs":
                        return HWResolver.shared.getXpubs(requiredData)
                    case "sign_message":
                        return HWResolver.shared.signMessage(requiredData)
                    case "sign_tx":
                        return HWResolver.shared.signTransaction(requiredData)
                    case "get_balance", "get_transactions", "get_unspent_outputs", "get_subaccounts", "get_subaccount", "get_expired_deposits":
                        return HWResolver.shared.getBlindingNonces(requiredData)
                    case "create_transaction":
                        return HWResolver.shared.getBlindingKeys(requiredData)
                    case "get_receive_address":
                        let address = requiredData["address"] as? [String: Any]
                        let script = address?["blinding_script_hash"] as? String
                        return HWResolver.shared.getBlindingKey(script: script!)
                            .compactMap { bkey in
                                return "{\"blinding_key\":\"\(bkey)\"}"
                            }
                    default:
                        throw GaError.GenericError
                    }
                }.then { code in
                    return Promise().map(on: bgq) { try self.resolveCode(code: code) }
                }
            }
            // User interface resolver
            let method = json["method"] as? String ?? ""
            let sender = UIApplication.topViewController()
            let popup = PopupCodeResolver(sender!)
            return Promise()
                .map { sender?.stopAnimating() }
                .then { popup.code(method) }
                .map { code in sender?.startAnimating(); return code }
                .then(on: bgq) { code in self.waitConnection(connected).map { return code} }
                .then(on: bgq) { code in
                    return Promise().map(on: bgq) { try self.resolveCode(code: code) }
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
                let status = connected()
                if !status {
                    throw GaError.TimeoutError
                }
            }.recover { error -> Promise<Void> in
                guard attempts < 5 else { throw error }
                return after(DispatchTimeInterval.seconds(3)).then(on: nil, attempt)
            }
        }
        return attempt()
    }
}

extension UIViewController {

    @objc func codeResolverTextFieldDidChange(_ textField: UITextField) {
        if textField.text?.count == 6 {
            dismiss(animated: true) { }
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

class PopupCodeResolver {
    private let viewController: UIViewController

    init(_ view: UIViewController) {
        self.viewController = view
    }

    func code(_ method: String) -> Promise<String> {
        return Promise { result in
            let methodDesc: String
            if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
            let title = String(format: NSLocalizedString("id_please_provide_your_1s_code", comment: ""), NSLocalizedString(methodDesc, comment: ""))
            let alert = CodeAlertController(title: title, message: "", preferredStyle: .alert)
            alert.addTextField {[weak self] (textField: UITextField!) in
                textField.placeholder = ""
                textField.keyboardType = .numberPad
                textField.addTarget(self, action: #selector(self?.viewController.codeResolverTextFieldDidChange(_:)), for: .editingChanged)
            }
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
                result.reject(TwoFactorCallError.cancel(localizedDescription: NSLocalizedString("id_action_canceled", comment: "")))
            })
            alert.didDisappearBlock = {[weak alert] _ in
                if alert?.textFields?.first?.text?.count == 6 {
                    result.fulfill((alert?.textFields?.first?.text)!)
                }
            }
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
                if method == TwoFactorType.email.rawValue { methodDesc = "id_email" } else if method == TwoFactorType.phone.rawValue { methodDesc = "id_phone_call" } else if method == TwoFactorType.sms.rawValue { methodDesc = "id_sms" } else { methodDesc = "id_authenticator_app" }
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
