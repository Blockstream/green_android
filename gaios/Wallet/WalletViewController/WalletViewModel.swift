import Foundation
import UIKit
import PromiseKit

class WalletViewModel {

    var wm: WalletManager { WalletManager.current! }

    var isTxLoading = true // on init is always true

    /// load visible subaccounts
    var subaccounts: [WalletItem] {
        wm.subaccounts.filter { !($0.hidden ?? false) }
    }

    /// Cached data
    var cachedSubaccounts = [WalletItem]()
    var cachedTransactions = [Transaction]()
    var cachedBalance = [(String, Int64)]()

    /// reload by section with animation
    var reloadSections: (([WalletSection], Bool) -> Void)?

    /// cell models
    var accountCellModels = [AccountCellModel]() {
        didSet {
            reloadSections?([WalletSection.account], false)
        }
    }
    var txCellModels = [TransactionCellModel]() {
        didSet {
            reloadSections?( [WalletSection.transaction], true )
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
    var assetSelectCellModels: [AssetSelectCellModel] {
        return cachedBalance.map { AssetSelectCellModel(assetId: $0.0, satoshi: $0.1) }
    }

    func loadSubaccounts() {
        cachedSubaccounts = self.subaccounts
        wm.balances(subaccounts: self.subaccounts)
            .done { _ in
                self.accountCellModels = self.subaccounts.map {
                    AccountCellModel(subaccount: $0)
                }
                self.getAssets()
                self.getTransactions()
            }.catch { err in
                print(err)
            }
    }

    func getTransactions(subaccounts: [WalletItem]? = nil, page: Int = 0, max: Int? = nil) {
        let accounts = subaccounts != nil ? subaccounts : self.subaccounts
        isTxLoading = true
        wm.transactions(subaccounts: accounts ?? [])
            .done { txs in
                self.isTxLoading = false
                self.cachedTransactions = Array(txs.sorted(by: >).prefix(max ?? txs.count))
                self.txCellModels = self.cachedTransactions
                    .map { ($0, self.getNodeBlockHeight(subaccountHash: $0.subaccount!)) }
                    .map { TransactionCellModel(tx: $0.0, blockHeight: $0.1) }
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
                self.balanceCellModel = BalanceCellModel(satoshi: total, numAssets: amounts.count, cachedBalance: self.cachedBalance)
            }.catch { err in
                print(err)
            }
    }
}
