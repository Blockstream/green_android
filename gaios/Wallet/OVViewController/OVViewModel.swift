import Foundation
import UIKit

class OVViewModel {

    var wm: WalletManager { WalletManager.current! }

    var subaccounts: [WalletItem] { wm.subaccounts }

    var cachedBalance = [(String, Int64)]()

    var assetCellModels = [OVAssetCellModel]() {
        didSet {
            reloadTableView?()
        }
    }
    var balanceCellModel = OVBalanceCellModel() {
        didSet {
            reloadTableView?()
        }
    }
    var txCellModels = [OVTransactionCellModel]() {
        didSet {
            reloadTableView?()
        }
    }

    var reloadTableView: (() -> Void)?

    func getAssets() {
        wm.balances(subaccounts: self.subaccounts)
            .done { amounts in
                self.cachedBalance = amounts.map { ($0.key, $0.value) }.sorted(by: { (rhs, lhs) in
                    let lbtc = getGdkNetwork("liquid").getFeeAsset()
                    if rhs.0 == "btc" { return true
                    } else if lhs.0 == "btc" { return false
                    } else if rhs.0 == lbtc { return true
                    } else if lhs.0 == lbtc { return false
                    } else if self.wm.registry.hasImage(for: rhs.0) == true { return true
                    } else if self.wm.registry.hasImage(for: lhs.0) == true { return false
                    } else if self.wm.registry.info(for: rhs.0).ticker != nil { return true
                    } else if self.wm.registry.info(for: lhs.0).ticker != nil { return false
                    } else { return true}
                })
                let total = amounts.filter({$0.0 == "btc"}).map {$0.1}.reduce(0, +)
                self.balanceCellModel = OVBalanceCellModel(satoshi: total, numAssets: amounts.count - 2)
                self.assetCellModels = self.cachedBalance.map { OVAssetCellModel(assetId: $0.0, satoshi: $0.1) }
            }.catch { err in
                print(err)
            }
    }

    func getPendingTransactions() {
        let blockHeight = wm.currentSession?.notificationManager?.blockHeight ?? 0
        wm.transactions(subaccounts: self.subaccounts)
            .done { txs in
                self.txCellModels = txs.filter {
                    $0.blockHeight == 0 ||
                    ($0.isLiquid && blockHeight < $0.blockHeight + 1) ||
                    (!$0.isLiquid && blockHeight < $0.blockHeight + 5)
                }.map { OVTransactionCellModel(tx: $0, blockHeight: blockHeight)}
            }.catch { err in
                print(err)
            }
    }

    func getNodeBlockHeight(subaccountHash: Int) -> UInt32 {
        if let subaccount = self.wm.subaccounts.filter({ $0.hashValue == subaccountHash }).first,
            let network = subaccount.network,
            let session = self.wm.sessions[network],
            let blockHeight = session.notificationManager?.blockHeight {
                return blockHeight
        }
        return 0
    }

    func getAssetCellModels(at indexPath: IndexPath) -> OVAssetCellModel {
        return assetCellModels[indexPath.row]
    }

    func getBalanceCellModel() -> OVBalanceCellModel {
        return balanceCellModel
    }

    func getTransactionCellModel(at indexPath: IndexPath) -> OVTransactionCellModel {
        return txCellModels[indexPath.row]
    }
}
