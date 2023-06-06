import Foundation
import UIKit

public protocol HWProtocol {

    func xpubs(network: String, paths: [[Int]]) async throws -> [String]
    func signMessage(_ params: HWSignMessageParams) async throws -> HWSignMessageResult

    // swiftlint:disable:next function_parameter_count
    func signTransaction(network: String, params: HWSignTxParams) async throws -> HWSignTxResponse

    // swiftlint:disable:next function_parameter_count
    func newReceiveAddress(chain: String,
                                  mainnet: Bool,
                                  multisig: Bool,
                                  chaincode: String?,
                                  recoveryPubKey: String?,
                                  walletPointer: UInt32?,
                                  walletType: String?,
                                  path: [UInt32],
                                  csvBlocks: UInt32) async throws -> String


    // Liquid calls
    func getMasterBlindingKey() async throws -> String
    func getBlindingKey(scriptHex: String) async throws -> String
    func getSharedNonce(pubkey: String, scriptHex: String) async throws -> String
    func getBlindingFactors(params: HWBlindingFactorsParams) async throws -> HWBlindingFactorsResult
    func signLiquidTransaction(network: String, params: HWSignTxParams) async throws -> HWSignTxResponse
}
