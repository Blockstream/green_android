import Foundation
import UIKit

import greenaddress
import hw

public protocol PopupResolverDelegate {
    func code(_ method: String) async throws -> String
    func method(_ methods: [String]) async throws -> String
}

public enum ResolverError: Error {
    case failure(localizedDescription: String)
    case cancel(localizedDescription: String)
}

public enum TwoFactorCallError: Error {
    case failure(localizedDescription: String)
    case cancel(localizedDescription: String)
}

public class GDKResolver {
    
    let chain: String
    let connected: () -> Bool
    let twoFactorCall: TwoFactorCall?
    let popupDelegate: PopupResolverDelegate?
    let hwDelegate: HwResolverDelegate?

    public init(_ twoFactorCall: TwoFactorCall?,
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

    public func resolve() async throws -> [String: Any]? {
        let res = try self.twoFactorCall?.getStatus()
        let status = res?["status"] as? String
        if status == "done" {
            return res
        } else {
            try await resolving(res ?? [:])
            return try await resolve()
        }
    }

    private func resolving(_ res: [String: Any]) async throws {
        let status = res["status"] as? String
        print("\(chain) \(res)")
        switch status {
        case "done":
            break
        case "error":
            let error = res["error"] as? String ?? ""
            throw TwoFactorCallError.failure(localizedDescription: error)
        case "call":
            try self.twoFactorCall?.call()
        case "request_code":
            let methods = res["methods"] as? [String] ?? []
            if methods.count > 1 {
                let method = try await self.popupDelegate?.method(methods)
                try await self.waitConnection()
                try self.twoFactorCall?.requestCode(method: method)
            } else {
                try self.twoFactorCall?.requestCode(method: methods[0])
            }
        case "resolve_code":
            // Hardware wallet interface resolver
            if let requiredData = res["required_data"] as? [String: Any],
                let action = requiredData["action"] as? String,
                let device = requiredData["device"] as? [String: Any],
                let json = try? JSONSerialization.data(withJSONObject: device, options: []),
                let hwdevice = try? JSONDecoder().decode(HWDevice.self, from: json) {
                do {
                    let res = try await HWResolver().resolveCode(action: action, device: hwdevice, requiredData: requiredData, chain: chain)
                    try self.twoFactorCall?.resolveCode(code: res.stringify())
                } catch {
                    throw TwoFactorCallError.failure(localizedDescription: error.localizedDescription)
                }
            } else {
                // Software wallet interface resolver
                let method = res["method"] as? String ?? ""
                let code = try await self.popupDelegate?.code(method)
                try await self.waitConnection()
                try self.twoFactorCall?.resolveCode(code: code)
            }
        default:
            break
        }
    }

    func waitConnection() async throws {
        var attempts = 0
        func attempt() async throws {
            if attempts == 5 {
                throw GaError.TimeoutError()
            }
            attempts += 1
            let status = self.connected()
            if !status {
                try await Task.sleep(nanoseconds:  3 * 1_000_000_000)
                try await attempt()
            }
        }
        return try await attempt()
    }
}
