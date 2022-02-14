import Foundation
import UIKit
import PromiseKit
import RxSwift

protocol HWProtocol {

    func xpubs(paths: [[Int]]) -> Observable<[String]>

    func signMessage(path: [Int]?,
                     message: String?,
                     useAeProtocol: Bool?,
                     aeHostCommitment: String?,
                     aeHostEntropy: String?)
    -> Observable<(signature: String?, signerCommitment: String?)>

    func signTransaction(tx: [String: Any],
                         inputs: [[String: Any]],
                         outputs: [[String: Any]],
                         transactions: [String: String],
                         useAeProtocol: Bool) -> Observable<[String: Any]>

    // swiftlint:disable:next function_parameter_count
    func newReceiveAddress(network: String, subaccount: UInt32, branch: UInt32, pointer: UInt32, recoveryChainCode: String?, recoveryPubKey: String?, csvBlocks: UInt32) -> Observable<String>

    func getMasterBlindingKey() -> Observable<String>

    // Liquid calls
    func getBlindingKey(scriptHex: String) -> Observable<String?>
    func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?>

    func signLiquidTransaction(tx: [String: Any],
                               inputs: [[String: Any]],
                               outputs: [[String: Any]],
                               transactions: [String: String],
                               useAeProtocol: Bool) -> Observable<[String: Any]>
}
