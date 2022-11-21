import Foundation
import UIKit
import PromiseKit

class AccountViewModel {

    var wm: WalletManager { WalletManager.current! }
    var account: WalletItem!
    var cachedBalance: [(String, Int64)]
    var cachedTransactions = [Transaction]()
    var page = 0
    var fetchingTxs = false

    var accountCellModels: [AccountCellModel] {
        didSet {
            reloadSections?( [AccountSection.account], true )
        }
    }

    var txCellModels = [TransactionCellModel]() {
        didSet {
            reloadSections?( [AccountSection.transaction], true )
        }
    }

    var assetCellModels = [WalletAssetCellModel]() {
        didSet {
            reloadSections?( [AccountSection.assets], true )
        }
    }

    /// reload by section with animation
    var reloadSections: (([AccountSection], Bool) -> Void)?

    // on success
    var success: (() -> Void)?

    // on errors
    var error: ((Error) -> Void)?

    init(model: AccountCellModel, account: WalletItem, cachedBalance: [(String, Int64)]) {
        self.accountCellModels = [model]
        self.account = account
        self.cachedBalance = cachedBalance
    }

    func getTransactions(restart: Bool = true, max: Int? = nil) {
        if fetchingTxs {
            return
        }
        fetchingTxs = true
        wm.transactions(subaccounts: [account], first: self.cachedTransactions.count)
            .done { txs in
                if restart {
                    self.page = 0
                    self.cachedTransactions = []
                    self.txCellModels = []
                }
                print("-----------> \(self.page) \(txs.count)")
                self.page += 1
                self.cachedTransactions += txs
                self.txCellModels += txs
                    .map { ($0, self.getNodeBlockHeight(subaccountHash: $0.subaccount!)) }
                    .map { TransactionCellModel(tx: $0.0, blockHeight: $0.1) }
            }.ensure {
                self.fetchingTxs = false
            }.catch { err in
                print(err)
            }
    }

    func getBalance() {
        let assets = AssetAmountList(account.satoshi ?? [:]).sorted()
        self.assetCellModels = assets.map { WalletAssetCellModel(assetId: $0.0, satoshi: $0.1) }
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

    func archiveSubaccount() {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return
        }
        Guarantee()
            .then { session.updateSubaccount(subaccount: self.account.pointer, hidden: true) }
            .then { session.subaccount(self.account.pointer) }
            .compactMap { self.accountCellModels = [AccountCellModel(subaccount: $0)]}
            .then { self.wm.subaccounts() }
            .done { _ in self.success?() }
            .catch { err in self.error?(err) }
    }

    func renameSubaccount(name: String) {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return
        }
        Guarantee()
            .then { session.renameSubaccount(subaccount: self.account.pointer, newName: name) }
            .then { session.subaccount(self.account.pointer) }
            .compactMap { self.accountCellModels = [AccountCellModel(subaccount: $0)]}
            .then { self.wm.subaccounts() }
            .done { _ in self.success?() }
            .catch { err in self.error?(err) }
    }
}
