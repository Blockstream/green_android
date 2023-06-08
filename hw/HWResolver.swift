import Foundation
import UIKit
import RxSwift
import Combine

public protocol HwResolverDelegate {
    func resolveCode(action: String, device: HWDevice, requiredData: [String: Any], chain: String?) async throws -> HWResolverResult
}

public class HWResolver: HwResolverDelegate {
    
    public init() { }
    
    public func resolveCode(action: String, device: HWDevice, requiredData: [String: Any], chain: String?) async throws -> HWResolverResult {
        let hw: HWProtocol = device.isJade ? Jade.shared : Ledger.shared
        let chain = chain ?? "mainnet"
        switch action {
        case "get_xpubs":
            guard let paths = requiredData["paths"] as? [[Int]] else {
                throw HWError.Abort("Invalid xpubs request")
            }
            let res = try await getXpubs(hw: hw, paths: paths, chain: chain)
            return HWResolverResult(xpubs: res)
        case "sign_message":
            guard let params = HWSignMessageParams.from(requiredData) as? HWSignMessageParams else {
                throw HWError.Abort("Invalid sign message request")
            }
            let res = try await signMessage(hw: hw, params: params)
            return HWResolverResult(signerCommitment: res.signerCommitment, signature: res.signature)
        case "sign_tx":
            let data = HWSignTxParams(requiredData)
            let res = try await signTransaction(hw: hw, params: data, chain: chain)
            return HWResolverResult(signerCommitments: res.signerCommitments, signatures: res.signatures)
        case "get_blinding_factors":
            let usedUtxos = requiredData["used_utxos"] as? [[String: Any]]
            let transactionOutputs = requiredData["transaction_outputs"] as? [[String: Any]]
            let params = HWBlindingFactorsParams(usedUtxos: usedUtxos ?? [], transactionOutputs: transactionOutputs ?? [])
            let res = try await getBlindingFactors(hw: hw, params: params)
            return HWResolverResult(assetblinders: res.assetblinders, amountblinders: res.amountblinders)
        case "get_blinding_nonces":
            guard let scripts = requiredData["scripts"] as? [String],
                  let publicKeys = requiredData["public_keys"] as? [String] else {
                throw HWError.Abort("Invalid nonces request")
            }
            var nonces = [String]()
            var keys = [String]()
            for (script, publicKey) in Array(zip(scripts, publicKeys)) {
                nonces += [try await getBlindingNonce(hw: hw, script: script, publicKey: publicKey)]
            }
             if let blindingKeysRequired = requiredData["blinding_keys_required"] as? Bool,
                blindingKeysRequired {
                 for script in scripts {
                     keys += [try await getBlindingKey(hw: hw, script: script)]
                 }
             }
             return HWResolverResult(nonces: nonces, publicKeys: keys)
        case "get_blinding_public_keys":
            guard let scripts = requiredData["scripts"] as? [String] else {
                throw HWError.Abort("Invalid public keys request")
            }
            
            var keys = [String]()
            for script in scripts {
                keys += [try await getBlindingKey(hw: hw, script: script)]
            }
            return HWResolverResult(publicKeys: keys)
        case "get_master_blinding_key":
            let res = try await getMasterBlindingKey(hw: hw)
            return HWResolverResult(masterBlindingKey: res)
        default:
            throw HWError.Abort("Invalid request")
        }
    }
    
    func getXpubs(hw: HWProtocol,
                  paths: [[Int]],
                  chain: String,
                  _ completion: @escaping (Result<[String], Error>) -> Void) {
        _ = hw.xpubs(network: chain, paths: paths)
            .subscribe(onNext: { data in
                completion(.success(data))
            }, onError: { err in
                completion(.failure(err))
            })
    }

    func getXpubs(hw: HWProtocol,
                  paths: [[Int]],
                  chain: String) async throws -> [String] {
        return try await withCheckedThrowingContinuation { continuation in
            getXpubs(hw: hw, paths: paths, chain: chain) { result in
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func signMessage(hw: HWProtocol,
                  params: HWSignMessageParams,
                  _ completion: @escaping (Result<HWSignMessageResult, Error>) -> Void) {
        _ = hw.signMessage(params)
            .subscribe(onNext: { data in
                completion(.success(data))
            }, onError: { err in
                completion(.failure(err))
            })
    }
    
    func signMessage(hw: HWProtocol,
                  params: HWSignMessageParams) async throws -> HWSignMessageResult {
        return try await withCheckedThrowingContinuation { continuation in
            signMessage(hw: hw, params: params) { result in
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    
    func signTransaction(hw: HWProtocol,
                         params: HWSignTxParams,
                         chain: String,
                         _ completion: @escaping (Result<HWSignTxResponse, Error>) -> Void) {
        // Increment connection timeout for sign transaction command
        Ledger.shared.TIMEOUT = 120
        _ = Observable.just(hw)
            .flatMap { hw -> Observable<HWSignTxResponse> in
                if chain.contains("liquid") {
                    return hw.signLiquidTransaction(network: chain, params: params)
                }
                return hw.signTransaction(network: chain, params: params)
            }.subscribe(onNext: { res in
                completion(.success(res))
                Ledger.shared.TIMEOUT = 30
            }, onError: { err in
                completion(.failure(err))
            })
    }
    
    func signTransaction(hw: HWProtocol,
                         params: HWSignTxParams,
                         chain: String) async throws -> HWSignTxResponse {
        return try await withCheckedThrowingContinuation { continuation in
            signTransaction(hw: hw, params: params, chain: chain) { result in
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func getBlindingFactors(hw: HWProtocol,
                  params: HWBlindingFactorsParams,
                  _ completion: @escaping (Result<HWBlindingFactorsResult, Error>) -> Void) {
        _ = hw.getBlindingFactors(params: params)
            .subscribe(onNext: { data in
                completion(.success(data))
            }, onError: { err in
                completion(.failure(err))
            })
    }

    func getBlindingFactors(hw: HWProtocol,
                  params: HWBlindingFactorsParams) async throws -> HWBlindingFactorsResult {
        return try await withCheckedThrowingContinuation { continuation in
            getBlindingFactors(hw: hw, params: params) { result in
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func getBlindingNonce(hw: HWProtocol,
                           script: String,
                           publicKey: String,
                  _ completion: @escaping (Result<String, Error>) -> Void) {
        _ = hw.getSharedNonce(pubkey: publicKey, scriptHex: script)
            .subscribe(onNext: { data in
                completion(.success(data!))
            }, onError: { err in
                completion(.failure(err))
            })
    }

    func getBlindingNonce(hw: HWProtocol,
                           script: String,
                           publicKey: String) async throws -> String {
        return try await withCheckedThrowingContinuation { continuation in
                getBlindingNonce(hw: hw, script: script, publicKey: publicKey) { result in
                    switch result {
                    case .success(let data):
                        continuation.resume(returning: data)
                    case .failure(let error):
                        continuation.resume(throwing: error)
                    }
            }
        }
    }

    func getBlindingKey(hw: HWProtocol,
                        script: String,
                  _ completion: @escaping (Result<String, Error>) -> Void) {
        _ = hw.getBlindingKey(scriptHex: script)
            .subscribe(onNext: { data in
                completion(.success(data!.description))
            }, onError: { err in
                completion(.failure(err))
            })
    }
    
    func getBlindingKey(hw: HWProtocol, script: String) async throws -> String {
        return try await withCheckedThrowingContinuation { continuation in
            getBlindingKey(hw: hw, script: script) { result in
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func getMasterBlindingKey(hw: HWProtocol,
                  _ completion: @escaping (Result<String, Error>) -> Void) {
        _ = hw.getMasterBlindingKey()
            .subscribe(onNext: { data in
                completion(.success(data.description))
            }, onError: { err in
                completion(.success(""))
            })
    }
    
    func getMasterBlindingKey(hw: HWProtocol) async throws -> String {
        return try await withCheckedThrowingContinuation { continuation in
            getMasterBlindingKey(hw: hw) { result in
                switch result {
                case .success(let data):
                    continuation.resume(returning: data)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
