import Foundation
import UIKit
import PromiseKit

class AccountViewModel {

    var wm: WalletManager { WalletManager.current! }

    var accountCellModels: [AccountCellModel] = []
    var account: WalletItem
    var cachedBalance: [(String, Int64)]

    var cachedTransactions = [Transaction]()

    var txCellModels = [TransactionCellModel]() {
        didSet {
            reloadSections?( [AccountSection.transaction], true )
        }
    }

    /// reload by section with animation
    var reloadSections: (([AccountSection], Bool) -> Void)?

    init(model: AccountCellModel, account: WalletItem, cachedBalance: [(String, Int64)]) {
        self.accountCellModels = [model]
        self.account = account
        self.cachedBalance = cachedBalance
    }

    func getTransactions(page: Int = 0, max: Int? = nil) {
        wm.transactions(subaccounts: [account])
            .done { txs in
                print("-----------> \(txs.count)")
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
}
