import Foundation
import UIKit
import PromiseKit
import greenaddress
import hw

public enum ResolverError: Error {
    case failure(localizedDescription: String)
    case cancel(localizedDescription: String)
}

public protocol PopupResolverDelegate {
    func code(_ method: String) -> Promise<String>
    func method(_ methods: [String]) -> Promise<String>
}

public enum TwoFactorCallError: Error {
    case failure(localizedDescription: String)
    case cancel(localizedDescription: String)
}

public class GDKResolver {
    
    let chain: String
    let connected: () -> Bool
    let twoFactorCall: TwoFactorCall
    let popupDelegate: PopupResolverDelegate?
    let hwDelegate: HwResolverDelegate?

    public init(_ twoFactorCall: TwoFactorCall,
         popupDelegate: PopupResolverDelegate? = nil,
         hwDelegate: HwResolverDelegate? = nil,
         chain: String,
         connected: @escaping() -> Bool = { true }) {
        self.twoFactorCall = twoFactorCall
        self.popupDelegate = popupDelegate
        self.hwDelegate = hwDelegate
        self.chain = chain
        self.connected = connected
    }

    public func resolve() -> Promise<[String: Any]> {
        func step() -> Promise<[String: Any]> {
            let bgq = DispatchQueue.global(qos: .background)
            return Guarantee().map(on: bgq) {
                try self.twoFactorCall.getStatus()!
            }.then { json in
                try self.resolving(json: json).map { _ in json }
            }.then(on: bgq) { json -> Promise<[String: Any]> in
                guard let status = json["status"] as? String else { throw GaError.GenericError() }
                if status == "done" {
                    return Promise<[String: Any]> { seal in seal.fulfill(json) }
                } else {
                    return step()
                }
            }
        }
        return step()
    }

    private func resolving(json: [String: Any]) throws -> Promise<Void> {
        guard let status = json["status"] as? String else { throw GaError.GenericError() }
        let bgq = DispatchQueue.global(qos: .background)
        switch status {
        case "done":
            return Guarantee().asVoid()
        case "error":
            let error = json["error"] as? String ?? ""
            throw TwoFactorCallError.failure(localizedDescription: error)
        case "call":
            return Promise().map(on: bgq) { try self.twoFactorCall.call() }
        case "request_code":
            let methods = json["methods"] as? [String] ?? []
            if methods.count > 1 {
                return Promise()
                    //.map { sender?.stopAnimating() }
                    .compactMap { self.popupDelegate }
                    .then { $0.method(methods) }
                    //.map { method in sender?.startAnimating(); return method }
                    .then(on: bgq) { code in self.waitConnection().map { return code} }
                    .map(on: bgq) { method in try self.twoFactorCall.requestCode(method: method) }
                    //.map { sender?.stopAnimating() }
            } else {
                return Promise().map(on: bgq) { try self.twoFactorCall.requestCode(method: methods[0]) }
            }
        case "resolve_code":
            // Hardware wallet interface resolver
            if let requiredData = json["required_data"] as? [String: Any],
                let action = requiredData["action"] as? String,
                let device = requiredData["device"] as? [String: Any],
                let json = try? JSONSerialization.data(withJSONObject: device, options: []),
                let hwdevice = try? JSONDecoder().decode(HWDevice.self, from: json) {
                return HWResolver().resolveCode(action: action, device: hwdevice, requiredData: requiredData, chain: chain)
                    .compactMap(on: bgq) { code in
                        try self.twoFactorCall.resolveCode(code: code)
                }
            }
            // Software wallet interface resolver
            let method = json["method"] as? String ?? ""
            return Promise()
                .compactMap { self.popupDelegate }
                .then { $0.code(method) }
                //.map { code in sender?.startAnimating(); return code }
                .then(on: bgq) { code in self.waitConnection().map { return code} }
                .compactMap(on: bgq) { code in try self.twoFactorCall.resolveCode(code: code) }
                //.map { sender?.stopAnimating() }
        default:
            return Guarantee().asVoid()
        }
    }

    func waitConnection() -> Promise<Void> {
        var attempts = 0
        func attempt() -> Promise<Void> {
            attempts += 1
            return Guarantee().map {
                let status = self.connected()
                if !status {
                    throw GaError.TimeoutError()
                }
            }.recover { error -> Promise<Void> in
                guard attempts < 5 else { throw error }
                return after(DispatchTimeInterval.seconds(3)).then(on: nil, attempt)
            }
        }
        return attempt()
    }
}
