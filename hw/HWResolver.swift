import Foundation
import UIKit
import PromiseKit
import RxSwift

public protocol HwResolverDelegate {
    func resolveCode(action: String, device: HWDevice, requiredData: [String: Any], chain: String?) -> Promise<HWResolverResult>
}

public class HWResolver: HwResolverDelegate {

    public init() { }

    public func resolveCode(action: String, device: HWDevice, requiredData: [String: Any], chain: String?) -> Promise<HWResolverResult> {
        let hw: HWProtocol = device.isJade ? Jade.shared : Ledger.shared
        let chain = chain ?? "mainnet"
        switch action {
        case "get_xpubs":
            guard let paths = requiredData["paths"] as? [[Int]] else {
                return Promise { $0.reject(HWError.Abort("Invalid xpubs request")) }
            }
            return getXpubs(hw: hw, paths: paths, chain: chain).compactMap { HWResolverResult(xpubs: $0) }
        case "sign_message":
            guard let params = HWSignMessageParams.from(requiredData) as? HWSignMessageParams else {
                return Promise { $0.reject(HWError.Abort("Invalid sign message request")) }
            }
            return signMessage(hw: hw, params: params).compactMap { HWResolverResult(signerCommitment: $0.signerCommitment, signature: $0.signature) }
        case "sign_tx":
            let data = HWSignTxParams(requiredData)
            return signTransaction(hw: hw, params: data, chain: chain).compactMap { HWResolverResult(signerCommitments: $0.signerCommitments, signatures: $0.signatures) }
        case "get_blinding_factors":
            let usedUtxos = requiredData["used_utxos"] as? [[String: Any]]
            let transactionOutputs = requiredData["transaction_outputs"] as? [[String: Any]]
            let params = HWBlindingFactorsParams(usedUtxos: usedUtxos ?? [], transactionOutputs: transactionOutputs ?? [])
            return getBlindingFactors(hw: hw, params: params).compactMap { HWResolverResult(assetblinders: $0.assetblinders, amountblinders: $0.amountblinders) }
        case "get_blinding_nonces":
            guard let scripts = requiredData["scripts"] as? [String],
                  let publicKeys = requiredData["public_keys"] as? [String] else {
                return Promise { $0.reject(HWError.Abort("Invalid nonces request")) }
            }
            var nonces = [String]()
            return getBlindingNonces(hw: hw, scripts: scripts, publicKeys: publicKeys)
                .compactMap { nonces = $0 }
                .then { _ -> Promise<[String]> in
                    if let blindingKeysRequired = requiredData["blinding_keys_required"] as? Bool,
                       blindingKeysRequired {
                        return self.getBlindingPublicKeys(hw: hw, scripts: scripts)
                    }
                    return Promise<[String]> { seal in seal.fulfill([]) }
                }.compactMap { HWResolverResult(nonces: nonces, publicKeys: $0) }
        case "get_blinding_public_keys":
            guard let scripts = requiredData["scripts"] as? [String] else {
                return Promise { $0.reject(HWError.Abort("Invalid public keys request")) }
            }
            return getBlindingPublicKeys(hw: hw, scripts: scripts).compactMap { HWResolverResult(publicKeys: $0)
            }
        case "get_master_blinding_key":
            return getMasterBlindingKey(hw: hw).compactMap { HWResolverResult(masterBlindingKey: $0)
            }
        default:
            return Promise { $0.reject(HWError.Abort("Invalid request")) }
        }
    }

    func getXpubs(hw: HWProtocol, paths: [[Int]], chain: String) -> Promise<[String]> {
        return Promise { seal in
            _ = hw.xpubs(network: chain, paths: paths)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func signMessage(hw: HWProtocol, params: HWSignMessageParams) -> Promise<HWSignMessageResult> {
        return Promise { seal in
            _ = Observable.just(params)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in hw.signMessage(params) }
                .takeLast(1)
                .subscribe(onNext: { res in
                    seal.fulfill(res)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func signTransaction(hw: HWProtocol, params: HWSignTxParams, chain: String) -> Promise<HWSignTxResponse> {
        return Promise { seal in
            // Increment connection timeout for sign transaction command
            Ledger.shared.TIMEOUT = 120
            _ = Observable.just(hw)
                .flatMap { hw -> Observable<HWSignTxResponse> in
                    if chain.contains("liquid") {
                        return hw.signLiquidTransaction(network: chain, params: params)
                    }
                    return hw.signTransaction(network: chain, params: params)
                }.subscribe(onNext: { res in
                    seal.fulfill(res)
                    Ledger.shared.TIMEOUT = 30
                }, onError: { err in
                    seal.reject(err)
                })
            }
    }

    func getBlindingFactors(hw: HWProtocol, params: HWBlindingFactorsParams) -> Promise<HWBlindingFactorsResult> {
        return Promise { seal in
            _ = hw.getBlindingFactors(params: params)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingNonce(hw: HWProtocol, pubkey: String, script: String) -> Promise<String> {
        return Promise { seal in
            _ = hw.getSharedNonce(pubkey: pubkey, scriptHex: script)
                .subscribe(onNext: { data in
                    seal.fulfill(data!.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingNonces(hw: HWProtocol, scripts: [String], publicKeys: [String]) -> Promise<[String]> {
        return Promise<[String]>.chain(Array(zip(scripts, publicKeys)), 1) { self.getBlindingNonce(hw: hw, pubkey: $0.1, script: $0.0) }
    }

    func getBlindingKey(hw: HWProtocol, script: String) -> Promise<String> {
        return Promise { seal in
            _ = hw.getBlindingKey(scriptHex: script)
                .subscribe(onNext: { data in
                    seal.fulfill(data!.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingPublicKeys(hw: HWProtocol, scripts: [String]) -> Promise<[String]> {
        return Promise<[String]>.chain(scripts, 1) { self.getBlindingKey(hw: hw, script: $0) }
    }

    func getMasterBlindingKey(hw: HWProtocol) -> Promise<String> {
        return Promise { seal in
            _ = hw.getMasterBlindingKey()
                .subscribe(onNext: { data in
                    seal.fulfill(data.description)
                }, onError: { _ in
                    seal.fulfill("")
                })
        }
    }
}
