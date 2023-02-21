import Foundation
import UIKit
import PromiseKit

enum AmpEducationalMode {
    case table
    case header
    case hidden
}

class AccountViewModel {

    var wm: WalletManager { WalletManager.current! }
    var account: WalletItem!
    var cachedBalance: [(String, Int64)]
    var cachedTransactions = [Transaction]()
    var page = 0
    var fetchingTxs = false

    var showAssets: Bool {
        account.gdkNetwork.liquid
    }

    var watchOnly: Bool {
        AccountsManager.shared.current?.isWatchonly ?? false
    }

    var accountCellModels: [AccountCellModel] {
        didSet {
            reloadSections?( [AccountSection.account], true )
        }
    }
    var addingCellModels: [AddingCellModel] {
        let watchOnly = AccountsManager.shared.current?.isWatchonly ?? false
        let enabled2fa = account.session?.twoFactorConfig?.anyEnabled ?? false
        if account.type == .standard && !enabled2fa && !watchOnly {
            return [AddingCellModel()]
        }
        return []
    }

    var discloseCellModels: [DiscloseCellModel] {
        switch ampEducationalMode {
        case .table:
            return [DiscloseCellModel(title: "id_learn_more_about_amp_the_assets".localized, hint: "id_check_our_6_easy_steps_to_be".localized)]
        default:
            return []
        }
    }

    var ampEducationalMode: AmpEducationalMode {
        if account.type != .amp {
            return .hidden
        } else {
            let satoshi = account.satoshi?[account.gdkNetwork.getFeeAsset()] ?? 0
            let assets = AssetAmountList(self.account.satoshi ?? [:]).sorted()
            if satoshi > 0 || assets.count > 1 {
                return .header
            } else {
                return .table
            }
        }
    }

    var txCellModels = [TransactionCellModel]() {
        didSet {
            reloadSections?( [AccountSection.transaction], false )
        }
    }

    var assetCellModels = [WalletAssetCellModel]() {
        didSet {
            reloadSections?( [AccountSection.assets], true )
        }
    }

    /// reload by section with animation
    var reloadSections: (([AccountSection], Bool) -> Void)?

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
        wm.transactions(subaccounts: [account], first: (restart == true) ? 0 : self.cachedTransactions.count)
            .done { txs in
                if restart {
                    self.page = 0
                    self.cachedTransactions = []
                    self.txCellModels = []
                }
                print("-----------> \(self.page) \(txs.count)")
                if txs.count > 0 {
                    self.page += 1
                    self.cachedTransactions += txs
                    self.txCellModels += txs
                        .map { ($0, self.getNodeBlockHeight(subaccountHash: $0.subaccount!)) }
                        .map { TransactionCellModel(tx: $0.0, blockHeight: $0.1) }
                }
            }.ensure {
                self.fetchingTxs = false
                if self.txCellModels.count == 0 {
                    self.reloadSections?( [AccountSection.transaction], true )
                }
            }.catch { err in
                print(err)
            }
    }

    func getBalance() {
        wm.balances(subaccounts: [self.account])
            .done { _ in
                self.accountCellModels = [AccountCellModel(subaccount: self.account)]
                var assets = AssetAmountList(self.account.satoshi ?? [:]).sorted()
                self.assetCellModels = assets.map { WalletAssetCellModel(assetId: $0.0, satoshi: $0.1) }
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

    func archiveSubaccount() -> Promise<Void> {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return Promise().asVoid()
        }
        return Guarantee()
            .then { session.updateSubaccount(subaccount: self.account.pointer, hidden: true) }
            .then { session.subaccount(self.account.pointer) }
            .compactMap { self.accountCellModels = [AccountCellModel(subaccount: $0)]}
            .then { self.wm.subaccounts() }
            .asVoid()
    }

    func renameSubaccount(name: String) -> Promise<Void> {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return Promise().asVoid()
        }
        return Guarantee()
            .then { session.renameSubaccount(subaccount: self.account.pointer, newName: name) }
            .then { session.subaccount(self.account.pointer) }
            .compactMap { self.accountCellModels = [AccountCellModel(subaccount: $0)]}
            .then { self.wm.subaccounts() }
            .asVoid()
    }

    func handleEvent(_ notification: Notification) {
        let eventType = EventType(rawValue: notification.name.rawValue)
        switch eventType {
        case .Transaction:
            getBalance()
            getTransactions(restart: true, max: cachedTransactions.count)
        case .Block:
            if cachedTransactions.filter({ $0.blockHeight == 0 }).first != nil {
                getBalance()
            }
        case .Network:
            guard let dict = notification.userInfo as NSDictionary? else { return }
            guard let connected = dict["connected"] as? Bool else { return }
            guard let loginRequired = dict["login_required"] as? Bool else { return }
            if connected == true && loginRequired == false {
                getBalance()
            }
        default:
            break
        }
    }
}
