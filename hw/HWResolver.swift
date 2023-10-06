import Foundation
import UIKit
import Combine
import Semaphore

public protocol HwResolverDelegate {
    func resolveCode(action: String, device: HWDevice, requiredData: [String: Any], chain: String?, hwDevice: HWProtocol?) async throws -> HWResolverResult
}

public class HWResolver: HwResolverDelegate {
    static let semaphore = AsyncSemaphore(value: 1)
    public init() {}

    public func resolveCode(
        action: String,
        device: HWDevice,
        requiredData: [String: Any],
        chain: String?,
        hwDevice: HWProtocol?)
    async throws -> HWResolverResult {
        await HWResolver.semaphore.wait()
        do {
            let res = try await resolve(action: action, device: device, requiredData: requiredData, chain: chain, hwDevice: hwDevice)
            HWResolver.semaphore.signal()
            return res
        } catch {
            HWResolver.semaphore.signal()
            throw error
        }
    }

    private func resolve(
        action: String,
        device: HWDevice,
        requiredData: [String: Any],
        chain: String?,
        hwDevice: HWProtocol?)
    async throws -> HWResolverResult {
        guard let hw = hwDevice else {
            throw HWError.Abort("No hw found")
        }
        let chain = chain ?? "mainnet"
        switch action {
        case "get_xpubs":
            guard let paths = requiredData["paths"] as? [[Int]] else {
                throw HWError.Abort("Invalid xpubs request")
            }
            let res = try await hw.xpubs(network: chain, paths: paths)
            return HWResolverResult(xpubs: res)
        case "sign_message":
            guard let params = HWSignMessageParams.from(requiredData) as? HWSignMessageParams else {
                throw HWError.Abort("Invalid sign message request")
            }
            let res = try await hw.signMessage(params)
            return HWResolverResult(signerCommitment: res.signerCommitment, signature: res.signature)
        case "sign_tx":
            let params = HWSignTxParams(requiredData)
            let tx = chain.contains("liquid") ? try await hw.signLiquidTransaction(network: chain, params: params) : try await hw.signTransaction(network: chain, params: params)
            return HWResolverResult(signerCommitments: tx.signerCommitments, signatures: tx.signatures)
        case "get_blinding_factors":
            let usedUtxos = requiredData["used_utxos"] as? [[String: Any]]
            let transactionOutputs = requiredData["transaction_outputs"] as? [[String: Any]]
            let params = HWBlindingFactorsParams(usedUtxos: usedUtxos ?? [], transactionOutputs: transactionOutputs ?? [])
            let res = try await hw.getBlindingFactors(params: params)
            HWResolver.semaphore.signal()
            return HWResolverResult(assetblinders: res.assetblinders, amountblinders: res.amountblinders)
        case "get_blinding_nonces":
            guard let scripts = requiredData["scripts"] as? [String],
                  let publicKeys = requiredData["public_keys"] as? [String] else {
                throw HWError.Abort("Invalid nonces request")
            }
            var nonces = [String]()
            var keys = [String]()
            for (script, publicKey) in Array(zip(scripts, publicKeys)) {
                let res = try await hw.getSharedNonce(pubkey: publicKey, scriptHex: script)
                    nonces += [res]
            }
            if let blindingKeysRequired = requiredData["blinding_keys_required"] as? Bool,
               blindingKeysRequired {
                for script in scripts {
                    let res = try await hw.getBlindingKey(scriptHex: script)
                    keys += [res]
                }
            }
            return HWResolverResult(nonces: nonces, publicKeys: keys)
        case "get_blinding_public_keys":
            guard let scripts = requiredData["scripts"] as? [String] else {
                HWResolver.semaphore.signal()
                throw HWError.Abort("Invalid public keys request")
            }
            var keys = [String]()
            for script in scripts {
                let res = try await hw.getBlindingKey(scriptHex: script)
                keys += [res]
            }
            return HWResolverResult(publicKeys: keys)
        case "get_master_blinding_key":
            let res = try? await hw.getMasterBlindingKey()
            return HWResolverResult(masterBlindingKey: res ?? "")
        default:
            throw HWError.Abort("Invalid request")
        }
    }
}
