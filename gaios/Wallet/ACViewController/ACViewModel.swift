import Foundation
import UIKit
import PromiseKit

class ACViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    // load visible subaccounts
    var subaccounts: [WalletItem] {
        wm.subaccounts.filter { !($0.hidden ?? false) }
    }

    // reload all contents
    var reloadTableView: (() -> Void)?

    // account cell models
    var accountCellModels = [ACAccountCellModel]() {
        didSet {
            reloadTableView?()
        }
    }

    // transaction cell models
    var txCellModels = [ACTransactionCellModel]() {
        didSet {
            reloadTableView?()
        }
    }

    func getAccountCellModels(at indexPath: IndexPath) -> ACAccountCellModel {
        return accountCellModels[indexPath.row]
    }

    func getTransactionCellModels(at indexPath: IndexPath) -> ACTransactionCellModel {
        return txCellModels[indexPath.row]
    }

    func getSubaccounts(assetId: String) {
        let subaccounts = self.subaccounts.filter {
            (assetId == "btc" && !$0.gdkNetwork.liquid) ||
            (assetId != "btc" && $0.gdkNetwork.liquid)
        }
        wm.balances(subaccounts: subaccounts)
            .done { _ in
                self.accountCellModels = subaccounts.map {
                        ACAccountCellModel(subaccount: $0,
                                           assetId: assetId,
                                           satoshi: $0.satoshi?[assetId] ?? 0)
                    }
            }.catch { err in
                print(err)
            }
    }

    func getTransactions() {
        let blockHeight = wm.currentSession?.notificationManager?.blockHeight ?? 0
        wm.transactions(subaccounts: self.subaccounts)
            .done { txs in
                let txCellModels = txs.map { ACTransactionCellModel(tx: $0, blockHeight: blockHeight)}
                self.txCellModels = Array(txCellModels[...10])                
            }.catch { err in
                print(err)
            }
    }

}
