import Foundation
import UIKit
import PromiseKit
import RxSwift

public protocol HWProtocol {

    func xpubs(network: String, paths: [[Int]]) -> Observable<[String]>

    func signMessage(path: [Int]?,
                     message: String?,
                     useAeProtocol: Bool?,
                     aeHostCommitment: String?,
                     aeHostEntropy: String?)
    -> Observable<(signature: String?, signerCommitment: String?)>

    // swiftlint:disable:next function_parameter_count
    func signTransaction(network: String,
                         tx: AuthTx,
                         inputs: [AuthTxInput],
                         outputs: [AuthTxOutput],
                         transactions: [String: String],
                         useAeProtocol: Bool) -> Observable<AuthSignTransactionResponse>

    // swiftlint:disable:next function_parameter_count
    func newReceiveAddress(chain: String,
                                  mainnet: Bool,
                                  multisig: Bool,
                                  chaincode: String?,
                                  recoveryPubKey: String?,
                                  walletPointer: UInt32?,
                                  walletType: String?,
                                  path: [UInt32],
                                  csvBlocks: UInt32) -> Observable<String>

    func getMasterBlindingKey() -> Observable<String>

    // Liquid calls
    func getBlindingKey(scriptHex: String) -> Observable<String?>
    func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?>
    func getBlindingFactor(params: BlindingFactorsParams) -> Observable<BlindingFactorsResult>
    
    // swiftlint:disable:next function_parameter_count
    func signLiquidTransaction(network: String,
                               tx: AuthTx,
                               inputs: [AuthTxInput],
                               outputs: [AuthTxOutput],
                               transactions: [String: String],
                               useAeProtocol: Bool) -> Observable<AuthSignTransactionResponse>
}
