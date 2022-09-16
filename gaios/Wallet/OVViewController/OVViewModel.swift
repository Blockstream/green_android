import Foundation
import UIKit

class OVViewModel {

    var wm: WalletManager { WalletManager.current! }

    var subaccounts: [WalletItem] { wm.subaccounts }

    var cachedBalance = [String: UInt64]()

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
                self.cachedBalance = amounts
                let total = amounts.filter({$0.0 == "btc"}).map {$0.1}.reduce(0, +)
                self.balanceCellModel = OVBalanceCellModel(satoshi: total, numAssets: amounts.count - 2)
                self.assetCellModels = amounts.map { OVAssetCellModel(assetId: $0.0, satoshi: $0.1) }
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
