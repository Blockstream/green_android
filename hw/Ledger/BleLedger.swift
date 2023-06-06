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
        let res = try await signSW(tx: params.transaction,
                         inputs: params.signingInputs,
                         outputs: params.txOutputs)
        return HWSignTxResponse(signatures: res, signerCommitments: [])
    }

    public func signSW(tx: HWTransaction?, inputs: [InputOutput], outputs: [InputOutput]) async throws -> [String] {
        let hwInputs = inputs.map { input -> [String: Any] in
            let bytes = inputBytes(input, isSegwit: true)!
            let sequence = input.sequence.uint32LE()
            return ["value": bytes, "sequence": Data(sequence), "trusted": false, "segwit": true]
        }
        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        let prevoutScript = inputs.first?.prevoutScript?.hexToData()
        _ = try await startUntrustedTransaction(txVersion: tx?.transactionVersion ?? 0, newTransaction: true, inputIndex: 0, usedInputList: hwInputs, redeemScript: prevoutScript!, segwit: true)
        let bytes = outputBytes(outputs)
        _ = try await finalizeInputFull(data: bytes!)
        let sigs = try await signSWInputs(hwInputs: hwInputs, inputs: inputs, version: tx?.transactionVersion ?? 0, locktime: tx?.transactionLocktime ?? 0)
        var strings = [String]()
        for sig in sigs {
            strings += [sig.hex]
        }
        return strings
    }

    public func signSWInputs(hwInputs: [[String: Any]], inputs: [InputOutput], version: UInt, locktime: UInt) async throws -> [Data] {
        var list = [Data]()
        for hwInput in hwInputs.enumerated() {
            list += [try await signSWInput(hwInput: hwInput.element, input: inputs[hwInput.offset], version: version, locktime: locktime)]
        }
        return list
    }

    public func signSWInput(hwInput: [String: Any], input: InputOutput, version: UInt, locktime: UInt) async throws -> Data {
        let prevoutScript = input.prevoutScript!.hexToData()
        _ = try await startUntrustedTransaction(txVersion: version, newTransaction: false, inputIndex: 0, usedInputList: [hwInput], redeemScript: prevoutScript, segwit: true)
        let userPaths: [Int] = input.userPath?.map { Int($0) } ?? []
        return try await untrustedHashSign(privateKeyPath: userPaths, pin: "0", lockTime: locktime, sigHashType: self.SIGHASH_ALL)
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
