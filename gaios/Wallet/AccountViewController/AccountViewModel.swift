import Foundation
import UIKit
import PromiseKit
import gdk

enum AmpEducationalMode {
    case table
    case header
    case hidden
}

class AccountViewModel {

    private var wm: WalletManager { WalletManager.current! }
    private var cachedBalance: [(String, Int64)]
    private var cachedTransactions = [Transaction]()
    private let bgq = DispatchQueue.global(qos: .background)
    var account: WalletItem!
    var page = 0
    var fetchingTxs = false

    var showAssets: Bool {
        account.gdkNetwork.liquid
    }

    var watchOnly: Bool {
        wm.account.isWatchonly
    }

    var satoshi: Int64 {
        cachedBalance.first(where: { $0.0 == account.gdkNetwork.getFeeAsset() })?.1 ?? account.btc ?? 0
    }

    var accountCellModels: [AccountCellModel] {
        didSet {
            reloadSections?([AccountSection.account], true)
        }
    }

    var addingCellModels: [AddingCellModel] {
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
        Guarantee()
            .then(on: bgq) { self.wm.transactions(subaccounts: [self.account], first: (restart == true) ? 0 : self.cachedTransactions.count) }
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
            }.catch { err in print(err) }
    }

    func getBalance() {
        Guarantee()
            .then(on: bgq) { self.wm.balances(subaccounts: [self.account]) }
            .done { _ in
                self.cachedBalance = AssetAmountList(self.account.satoshi ?? [:]).sorted()
                self.accountCellModels = [AccountCellModel(subaccount: self.account, satoshi: self.satoshi)]
                self.assetCellModels = self.cachedBalance.map { WalletAssetCellModel(assetId: $0.0, satoshi: $0.1) }
            }.catch { err in print(err.localizedDescription)}
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
            .then(on: bgq) { session.updateSubaccount(subaccount: self.account.pointer, hidden: true) }
            .then(on: bgq) { self.wm.subaccount(account: self.account) }
            .compactMap { self.account = $0 }
            .compactMap { self.accountCellModels = [AccountCellModel(subaccount: self.account, satoshi: self.satoshi)] }
            .asVoid()
    }

    func renameSubaccount(name: String) -> Promise<Void> {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return Promise().asVoid()
        }
        return Guarantee()
            .then(on: bgq) { session.renameSubaccount(subaccount: self.account.pointer, newName: name) }
            .then(on: bgq) { self.wm.subaccount(account: self.account) }
            .compactMap { self.account = $0 }
            .compactMap { self.accountCellModels = [AccountCellModel(subaccount: self.account, satoshi: self.satoshi)] }
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
