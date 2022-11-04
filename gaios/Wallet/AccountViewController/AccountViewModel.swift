import Foundation
import UIKit
import PromiseKit

class AccountViewModel {

    var wm: WalletManager { WalletManager.current! }
    var presentingWallet: WalletItem? { wm.currentSubaccount }

    var accountCellModels: [AccountCellModel] = []
    var account: WalletItem

    var cachedTransactions = [Transaction]()

    var txCellModels = [TransactionCellModel]() {
        didSet {
            reloadSections?( [WalletSection.transaction], true )
        }
    }

    /// reload by section with animation
    var reloadSections: (([WalletSection], Bool) -> Void)?

    init(model: AccountCellModel, account: WalletItem) {
        self.accountCellModels = [model]
        self.account = account
    }

    func getTransactions(page: Int = 0, max: Int? = nil) {
        guard let subaccount = presentingWallet else { return }
        wm.transactions(subaccounts: [subaccount])
            .done { txs in
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
