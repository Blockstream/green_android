import Foundation
import gdk

extension Transaction {

    var subaccountItem: WalletItem? {
        return WalletManager.current?.subaccounts.filter({ $0.hashValue == subaccount }).first
    }
    
    var feeAsset: String {
        subaccountItem?.gdkNetwork.getFeeAsset() ?? ""
    }

    var assetamounts: AssetAmountList {
        get {
            return AssetAmountList(amounts).sorted()
        }
    }

    var amountsWithoutFees: AssetAmountList {
        if type == .some(.redeposit) {
            return []
        }
        var amounts = assetamounts
        // remove L-BTC asset only if fee on outgoing transactions
        if type == .some(.outgoing) || type == .some(.mixed) {
            amounts = amounts.filter({ !($0.0 == feeAsset && abs($0.1) == Int64(fee)) })
        }
        return amounts
    }
}
