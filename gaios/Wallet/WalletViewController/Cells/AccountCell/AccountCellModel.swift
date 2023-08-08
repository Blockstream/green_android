import Foundation
import UIKit
import gdk

struct AccountCellModel {
    
    var name: String { account.localizedName }
    var lblType: String { account.type.path.uppercased() }
    var hasTxs: Bool { account.hasTxs }
    var networkType: NetworkSecurityCase { account.networkType }
    var balanceStr: String = ""
    var fiatStr: String = ""
    var value: String = ""
    var fiat: String = ""
    
    var account: WalletItem
    var satoshi: Int64? {
        didSet {
            let assetId = account.gdkNetwork.getFeeAsset()
            if let satoshi = satoshi,let converted = Balance.fromSatoshi(satoshi, assetId: assetId) {
                let (amount, denom) = converted.toValue()
                balanceStr = "\(amount) \(denom)"
                let (fAmount, fDenom) = converted.toFiat()
                fiatStr = "\(fAmount) \(fDenom)"
            }
        }
    }
}
