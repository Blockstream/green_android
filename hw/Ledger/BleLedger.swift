import Foundation
import greenaddress

public class BleLedger: BleLedgerCommands, HWProtocol {
    
    let SIGHASH_ALL: UInt8 = 1
    
    public func xpubs(network: String, paths: [[Int]]) async throws -> [String] {
        var res = [String]()
        for path in paths {
            res += [try await xpubs(network: network, path: path)]
        }
        return res
    }
    
    public func xpubs(network: String, path: [Int]) async throws -> String {
        let isMainnet = ["mainnet", "liquid"].contains(network)
        let data = try await pubKey(path: path)
        let chainCode = Array((data["chainCode"] as? Data)!)
        let publicKey = Array((data["publicKey"] as? Data)!)
        let compressed = compressPublicKey(publicKey) ?? []
        let base58 = try! bip32KeyToBase58(isMainnet: isMainnet, pubKey: compressed, chainCode: chainCode)
        return base58
    }
    
    public func signMessage(_ params: HWSignMessageParams) async throws -> HWSignMessageResult {
        _ = try await signMessagePrepare(path: params.path, message: params.message.data(using: .utf8) ?? Data())
        let res = try await signMessageSign(pin: [0])
        let signature = res["signature"] as? [UInt8]
        return HWSignMessageResult(signature: signature?.hex, signerCommitment: nil)
    }
    
    public func signTransaction(network: String, params: HWSignTxParams) async throws -> HWSignTxResponse {
        if params.useAeProtocol {
            throw HWError.Abort("Hardware Wallet does not support the Anti-Exfil protocol");
        }
        var sw = false
        var p2sh = false
        params.signingInputs.forEach {
            if $0.isSegwit {
                sw = true
            } else {
                p2sh = true
            }
        }
        if p2sh || !sw {
            throw HWError.Abort("Supported only segwit");
        }
        
        let res = try await signSW(tx: params.transaction,
                         inputs: params.signingInputs,
                         outputs: params.txOutputs,
                         transactions: params.signingTxs
        )
        return HWSignTxResponse(signatures: res, signerCommitments: [])
    }

    public func signSW(tx: String?, inputs: [InputOutput], outputs: [InputOutput], transactions: [String: String]) async throws -> [String] {
        var hwInputs = [[String: Any]]()
        for input in inputs {
            guard let txHex = transactions[input.txHash ?? ""] else {
                throw HWError.Abort("Previous transaction \(input.txHash ?? "") not found")
            }
            let txData = txHex.hexToData()
            let tx = HWTransactionLedger(data: txData)
            let trustedInput = try await getTrustedInput(transaction: tx, index: Int(input.ptIdx), sequence: input.sequence, segwit: true)
            hwInputs += [trustedInput]
        }
        
        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        guard let wallyTx = Wally.txFromBytes(tx: tx?.hexToBytes() ?? [], elements: false, segwit: true) else {
            throw HWError.Abort("Invalid transaction")
        }
        
        let version = Wally.txVersion(wallyTx: wallyTx)
        let script0 = inputs.first?.prevoutScript?.hexToData()
        _ = try await startUntrustedTransaction(txVersion: version, newTransaction: true, inputIndex: 0, usedInputList: hwInputs, redeemScript: script0!, segwit: true)
        let bytes = outputBytes(outputs)
        _ = try await finalizeInputFull(data: bytes!)
        let locktime = Wally.txLocktime(wallyTx: wallyTx)
        
        // Sign each input
        var sigs = [Data]()
        for hwInput in hwInputs.enumerated() {
            let input = inputs[hwInput.offset]
            if !input.isSegwit { continue } // sign segwit only
            let singleInput = [hwInput.element]
            let script = input.prevoutScript?.hexToData()
            _ = try await startUntrustedTransaction(txVersion: version,
                                                    newTransaction: false,
                                                    inputIndex: 0,
                                                    usedInputList: singleInput,
                                                    redeemScript: script!,
                                                    segwit: true)
            let userPaths: [Int] = input.userPath?.map { Int($0) } ?? []
            let sig = try await untrustedHashSign(privateKeyPath: userPaths,
                                                  pin: "0",
                                                  lockTime: locktime,
                                                  sigHashType: self.SIGHASH_ALL)
            sigs += [sig]
        }
        var strings = [String]()
        for input in inputs.enumerated() {
            if input.offset < sigs.count {
                strings += [sigs[input.offset].hex]
            }
        }
        return strings
    }

    // swiftlint:disable:next function_parameter_count
    public func newReceiveAddress(chain: String, mainnet: Bool, multisig: Bool, chaincode: String?, recoveryPubKey: String?, walletPointer: UInt32?, walletType: String?, path: [UInt32], csvBlocks: UInt32) async throws -> String {
        fatalError()
    }
    
    public func getMasterBlindingKey() async throws -> String {
        fatalError()
    }
    
    public func getBlindingKey(scriptHex: String) async throws -> String {
        fatalError()
    }
    
    public func getSharedNonce(pubkey: String, scriptHex: String) async throws -> String {
        fatalError()
    }
    
    public func getBlindingFactors(params: HWBlindingFactorsParams) async throws -> HWBlindingFactorsResult {
        fatalError()
    }
    
    public func signLiquidTransaction(network: String, params: HWSignTxParams) async throws -> HWSignTxResponse {
        fatalError()
    }
}
