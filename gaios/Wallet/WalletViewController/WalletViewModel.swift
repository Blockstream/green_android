import Foundation
import UIKit
import PromiseKit

class WalletViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    var presentingWallet: WalletItem? { wm.currentSubaccount }

    // load visible subaccounts
    var subaccounts: [WalletItem] {
        wm.subaccounts.filter { !($0.hidden ?? false) }
    }

    // Cached data
    var cachedSubaccounts = [WalletItem]()
    var cachedTransactions = [Transaction]()

    // reload all contents
    var reloadTableView: (() -> Void)?  // to remove !!!!!!!!!!!!!!!

    // reload by section with animation
    var reloadSections: (([WalletSection], Bool) -> Void)?

    // account cell models
    var accountCellModels = [AccountCellModel]() {
        didSet {
            reloadSections?([WalletSection.account], false)
        }
    }

    // transaction cell models
    var txCellModels = [ACTransactionCellModel]() {
        didSet {
//            reloadSections?( [ACSection.transaction], true )
        }
    }

    var balanceCellModel = BalanceCellModel() {
        didSet {
            reloadTableView?()   ///???
        }
    }

//    func getAccountCellModels(at indexPath: IndexPath) -> ACAccountCellModel {
//        return accountCellModels[indexPath.row]
//    }

    func getTransactionCellModels(at indexPath: IndexPath) -> ACTransactionCellModel {
        return txCellModels[indexPath.row]
    }

//    func getSubaccounts(assetId: String) {
//        let allSubaccounts = self.subaccounts.filter {
//            (assetId == "btc" && !$0.gdkNetwork.liquid) ||
//            (assetId != "btc" && $0.gdkNetwork.liquid)
//        }
//        cachedSubaccounts = allSubaccounts
//        wm.balances(subaccounts: allSubaccounts)
//            .done { _ in
//                self.accountCellModels = allSubaccounts.map {
//                        ACAccountCellModel(subaccount: $0,
//                                           assetId: assetId,
//                                           satoshi: $0.satoshi?[assetId] ?? 0)
//                    }
//            }.catch { err in
//                print(err)
//            }
//    }

    func loadSubaccounts() {
        let allSubaccounts = self.subaccounts
        cachedSubaccounts = self.subaccounts
        wm.balances(subaccounts: allSubaccounts)
            .done { _ in
                self.accountCellModels = allSubaccounts.map {
                    AccountCellModel(subaccount: $0)
//                                           assetId: assetId,
//                                           satoshi: $0.satoshi?[assetId] ?? 0)
                    }
            }.catch { err in
                print(err)
            }
    }

    func getTransactions(assetId: String, subaccounts: [WalletItem]? = nil, page: Int = 0, max: Int? = nil) {
        let allSubaccounts = self.subaccounts.filter {
            (assetId == "btc" && !$0.gdkNetwork.liquid) ||
            (assetId != "btc" && $0.gdkNetwork.liquid)
        }
        cachedSubaccounts = allSubaccounts
        wm.transactions(subaccounts: subaccounts ?? allSubaccounts)
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

    func getBalanceCellModel() -> BalanceCellModel {
        return balanceCellModel
    }
}
