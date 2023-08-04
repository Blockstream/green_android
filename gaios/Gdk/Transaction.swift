import Foundation
import BreezSDK
import gdk

extension Transaction {

    var subaccountItem: WalletItem? {
        return WalletManager.current?.subaccounts.filter({ $0.hashValue == subaccount }).first
    }
    
    var feeAsset: String {
        subaccountItem?.gdkNetwork.getFeeAsset() ?? ""
    }

    func amountsWithFees() -> [String: Int64] {
        if type == .redeposit {
            return [feeAsset: -1 * Int64(fee)]
        } else {
            // remove L-BTC asset only if fee on outgoing transactions
            if type == .some(.outgoing) || type == .some(.mixed) {
                return amounts.filter({ !($0.key == feeAsset && abs($0.value) == Int64(fee)) })
            }
        }
        return amounts
    }
    
    var amountsWithoutFees: [String: Int64] {
        if type == .some(.redeposit) {
            return [:]
        }
        // remove L-BTC asset only if fee on outgoing transactions
        if type == .some(.outgoing) || type == .some(.mixed) {
            return amounts.filter({ !($0.0 == feeAsset && abs($0.1) == Int64(fee)) })
        }
        return amounts
    }

    public var isLightning: Bool {
        self.subaccountItem?.gdkNetwork.lightning ?? false
    }

    func isUnconfirmed(block: UInt32) -> Bool {
        if isLightning {
            return isInProgressSwap ?? (blockHeight == 0)
        } else if blockHeight == 0 {
            return true
        } else {
            return false
        }
    }

    func isPending(block: UInt32) -> Bool {
        if isLightning {
            return isInProgressSwap ?? (blockHeight == 0)
        } else if blockHeight == 0 {
            return true
        } else if isLiquid && block < blockHeight + 1 && block >= blockHeight {
            return true
        } else if !isLiquid && block < blockHeight + 5 && block >= blockHeight {
            return true
        } else {
            return false
        }
    }

    static func fromPayment(_ payment: Payment, subaccount: Int) -> Transaction {
        var tx = Transaction([:])
        tx.subaccount = subaccount
        tx.blockHeight = payment.pending ? 0 : UInt32(payment.paymentTime)
        tx.canRBF = false
        tx.memo = payment.description ?? ""
        tx.fee = payment.feeMsat / 1000
        tx.createdAtTs = payment.paymentTime * 1000000
        tx.feeRate = 0
        tx.hash = payment.id
        tx.type = payment.paymentType == .received ? .incoming : .outgoing
        tx.amounts = ["btc": payment.amountSatoshi]
        switch payment.details {
        case .ln(let data):
            switch data.lnurlSuccessAction {
            case .message(let data):
                tx.message = data.message
            case .aes(let data):
                tx.plaintext = [data.description: data.plaintext]
            case .url(let data):
                tx.url = [data.description: data.url]
            default:
                break
            }
        default:
            break
        }
        return tx
    }

    static func fromSwapInfo(_ swapInfo: SwapInfo, subaccount: Int, isRefundableSwap: Bool) -> Transaction {
        var tx = Transaction([:])
        tx.subaccount = subaccount
        tx.blockHeight = isRefundableSwap ? UInt32.max : 0
        tx.canRBF = false
        tx.memo = ""
        tx.fee = 0
        tx.feeRate = 0
        tx.createdAtTs = swapInfo.createdAt
        tx.hash = swapInfo.paymentHash.hex
        tx.type = .mixed
        tx.inputs = [["address": swapInfo.bitcoinAddress]]
        tx.outputs = []
        tx.amounts = ["btc": Int64(swapInfo.confirmedSats + swapInfo.unconfirmedSats)]
        tx.isInProgressSwap = swapInfo.confirmedSats > 0 && !isRefundableSwap
        tx.isRefundableSwap = isRefundableSwap
        return tx
    }
}
