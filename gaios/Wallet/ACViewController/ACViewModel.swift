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

    // Cached data
    var cachedSubaccounts = [WalletItem]()
    var cachedTransactions = [Transaction]()

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
        cachedSubaccounts = subaccounts
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

    func getTransactions(subaccounts: [WalletItem]? = nil, page: Int = 0, max: Int? = nil) {
        wm.transactions(subaccounts: subaccounts ?? self.subaccounts)
            .done { txs in
                self.cachedTransactions = Array(txs.sorted(by: >).prefix(max ?? txs.count))
                self.txCellModels = self.cachedTransactions
                    .map { ($0, self.getNodeBlockHeight(subaccountHash: $0.subaccount!)) }
                    .map { ACTransactionCellModel(tx: $0.0, blockHeight: $0.1) }
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
}
