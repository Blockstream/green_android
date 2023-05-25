import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import greenaddress

final public class Ledger: LedgerCommands, HWProtocol {

    public static let shared = Ledger()
    let SIGHASH_ALL: UInt8 = 1

    // swiftlint:disable:next function_parameter_count
    public func signTransaction(network: String, tx: AuthTx, inputs: [AuthTxInput], outputs: [AuthTxOutput],
                         transactions: [String: String], useAeProtocol: Bool) -> Observable<AuthSignTransactionResponse> {
        return signSW(tx: tx, inputs: inputs, outputs: outputs)
            .compactMap { AuthSignTransactionResponse(signatures: $0, signerCommitments: []) }
    }

    public func signSW(tx: AuthTx, inputs: [AuthTxInput], outputs: [AuthTxOutput]) -> Observable<[String]> {
        let hwInputs = inputs.map { input -> [String: Any] in
            let bytes = inputBytes(input, isSegwit: true)!
            let sequence = (input.sequence ?? 0).uint32LE()
            return ["value": bytes, "sequence": Data(sequence), "trusted": false, "segwit": true]
        }
        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        let prevoutScript = inputs.first?.prevoutScript.hexToData()
        return startUntrustedTransaction(txVersion: tx.transactionVersion ?? 0, newTransaction: true, inputIndex: 0, usedInputList: hwInputs, redeemScript: prevoutScript!, segwit: true)
        .flatMap { _ -> Observable<[String: Any]> in
            let bytes = self.outputBytes(outputs)
            return self.finalizeInputFull(data: bytes!)
        }.flatMap { _ -> Observable<[Data]> in
            return self.signSWInputs(hwInputs: hwInputs, inputs: inputs, version: tx.transactionVersion ?? 0, locktime: tx.transactionLocktime ?? 0)
        }.flatMap { sigs -> Observable<[String]> in
            var strings = [String]()
            for sig in sigs {
                let string = Array(sig).map { String(format: "%02hhx", $0) }.joined()
                strings.append(string)
            }
            return Observable.just(strings)
        }
    }

    public func signSWInputs(hwInputs: [[String: Any]], inputs: [AuthTxInput], version: UInt, locktime: UInt) -> Observable<[Data]> {
        let allObservables = hwInputs
            .enumerated()
            .map { hwInput -> Observable<Data> in
                return Observable.just(hwInput.element)
                    .flatMap { _ in self.signSWInput(hwInput: hwInput.element, input: inputs[hwInput.offset], version: version, locktime: locktime) }
                    .asObservable()
                    .take(1)
        }
        return Observable.concat(allObservables).reduce([], accumulator: { result, element in
            result + [element]
        })
    }

    public func signSWInput(hwInput: [String: Any], input: AuthTxInput, version: UInt, locktime: UInt) -> Observable<Data> {
        let prevoutScript = input.prevoutScript.hexToData()
        return startUntrustedTransaction(txVersion: version, newTransaction: false, inputIndex: 0, usedInputList: [hwInput], redeemScript: prevoutScript, segwit: true)
        .flatMap { _ -> Observable <Data> in
            let userPaths: [Int] = input.userPath.map { Int($0) }
            return self.untrustedHashSign(privateKeyPath: userPaths, pin: "0", lockTime: locktime, sigHashType: self.SIGHASH_ALL)
        }
    }

    public func signMessage(path: [Int]?,
                     message: String?,
                     useAeProtocol: Bool?,
                     aeHostCommitment: String?,
                     aeHostEntropy: String?)
    -> Observable<(signature: String?, signerCommitment: String?)> {
        return signMessagePrepare(path: path ?? [], message: message?.data(using: .utf8) ?? Data())
            .flatMap { _ in self.signMessageSign(pin: [0])}
            .compactMap { res in
                let signature = res["signature"] as? [UInt8]
                let hexSig = signature!.map { String(format: "%02hhx", $0) }.joined()
                return (signature: hexSig, signerCommitment: nil)
        }
    }

    public func xpubs(network: String, paths: [[Int]]) -> Observable<[String]> {
        let allObservables = paths
            .map {
                Observable.just($0)
                    .flatMap { self.xpubs(network: network, path: $0) }
                    .asObservable()
                    .take(1)
        }
        return Observable.concat(allObservables)
        .reduce([], accumulator: { result, element in
            result + [element]
        })
    }

    public func xpubs(network: String, path: [Int]) -> Observable<String> {
        let isMainnet = ["mainnet", "liquid"].contains(network)
        return self.pubKey(path: path)
            .flatMap { data -> Observable<String> in
                let chainCode = Array((data["chainCode"] as? Data)!)
                let publicKey = Array((data["publicKey"] as? Data)!)
                let compressed = compressPublicKey(publicKey) ?? []
                let base58 = try! bip32KeyToBase58(isMainnet: isMainnet, pubKey: compressed, chainCode: chainCode)
                return Observable.just(base58)
        }
    }

    // swiftlint:disable:next function_parameter_count
    public func newReceiveAddress(chain: String,
                                  mainnet: Bool,
                                  multisig: Bool,
                                  chaincode: String?,
                                  recoveryPubKey: String?,
                                  walletPointer: UInt32?,
                                  walletType: String?,
                                  path: [UInt32],
                                  csvBlocks: UInt32) -> Observable<String> {
        return Observable.error(HWError.Abort(""))
    }

    // Liquid not support
    public func getBlindingKey(scriptHex: String) -> Observable<String?> {
        return Observable.error(HWError.Abort(""))
    }

    public func getBlindingNonce(pubkey: String, scriptHex: String) -> Observable<String?> {
        return Observable.error(HWError.Abort(""))
    }

    // swiftlint:disable:next function_parameter_count
    public func signLiquidTransaction(network: String, tx: AuthTx, inputs: [AuthTxInput], outputs: [AuthTxOutput], transactions: [String: String], useAeProtocol: Bool) -> Observable<AuthSignTransactionResponse> {
        return Observable.error(HWError.Abort(""))
    }

    public func nonces(bscripts: [[String: Any]]) -> Observable<[String?]> {
        return Observable.error(HWError.Abort(""))
    }

    public func blindingKey(scriptHex: String) -> Observable<String?> {
        return Observable.error(HWError.Abort(""))
    }
    public func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?> {
        return Observable.error(HWError.Abort(""))
    }

    public func getMasterBlindingKey() -> Observable<String> {
        return Observable.error(HWError.Abort(""))
    }
    
    public func getBlindingFactor(params: BlindingFactorsParams) -> Observable<BlindingFactorsResult> {
        return Observable.error(HWError.Abort(""))
    }
}
