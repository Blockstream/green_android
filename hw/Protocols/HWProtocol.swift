import Foundation
import UIKit

import RxSwift

public protocol HWProtocol {

    func xpubs(network: String, paths: [[Int]]) -> Observable<[String]>
    func signMessage(_ params: HWSignMessageParams) -> Observable<HWSignMessageResult>

    // swiftlint:disable:next function_parameter_count
    func signTransaction(network: String, params: HWSignTxParams) -> Observable<HWSignTxResponse>

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


    // Liquid calls
    func getMasterBlindingKey() -> Observable<String>
    func getBlindingKey(scriptHex: String) -> Observable<String?>
    func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?>
    func getBlindingFactors(params: HWBlindingFactorsParams) -> Observable<HWBlindingFactorsResult>
    func signLiquidTransaction(network: String, params: HWSignTxParams) -> Observable<HWSignTxResponse>
}
