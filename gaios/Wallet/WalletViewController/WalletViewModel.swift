import Foundation
import UIKit
import PromiseKit

class WalletViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    var presentingWallet: WalletItem? { wm.currentSubaccount }

    var cachedBalance = [(String, Int64)]()

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

    var balanceCellModel: BalanceCellModel? {
        didSet {
            reloadSections?([WalletSection.balance], false)
        }
    }

    var walletAssetCellModels: [WalletAssetCellModel] {
        return cachedBalance.map { WalletAssetCellModel(assetId: $0.0, satoshi: $0.1) }
    }
    
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
                self.getAssets()
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

                // self.balanceCellModel = OVBalanceCellModel(satoshi: total, numAssets: amounts.count - 2)
                self.balanceCellModel = BalanceCellModel(satoshi: total, numAssets: amounts.count)
            }.catch { err in
                print(err)
            }
    }
}
