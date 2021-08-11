import Foundation
import UIKit
import PromiseKit
import RxSwift

protocol HWProtocol {

    var device: HWDevice { get }
    var connected: Bool { get }

    func xpubs(paths: [[Int]]) -> Observable<[String]>

    func signMessage(path: [Int]?,
                     message: String?,
                     useAeProtocol: Bool?,
                     aeHostCommitment: String?,
                     aeHostEntropy: String?)
    -> Observable<(signature: String?, signerCommitment: String?)>

    // swiftlint:disable:next function_parameter_count
    func signTransaction(tx: [String: Any],
                         inputs: [[String: Any]],
                         outputs: [[String: Any]],
                         transactions: [String: String],
                         addressTypes: [String],
                         useAeProtocol: Bool) -> Observable<[String: Any]>

    // swiftlint:disable:next function_parameter_count
    func newReceiveAddress(network: String, subaccount: UInt32, branch: UInt32, pointer: UInt32, recoveryChainCode: String?, recoveryPubKey: String?, csvBlocks: UInt32) -> Observable<String>

    func getMasterBlindingKey() -> Observable<String>

    // Liquid calls
    func getBlindingKey(scriptHex: String) -> Observable<String?>
    func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?>
    // swiftlint:disable:next function_parameter_count
    func signLiquidTransaction(tx: [String: Any],
                               inputs: [[String: Any]],
                               outputs: [[String: Any]],
                               transactions: [String: String],
                               addressTypes: [String],
                               useAeProtocol: Bool) -> Observable<[String: Any]>
    func clear()

}
