import Foundation
import UIKit
import PromiseKit
import gdk

class WalletViewModel {

    var wm: WalletManager? { WalletManager.current }

    var session: SessionManager? {
        return wm?.prominentSession
    }

    var isTxLoading = true // on init is always true
    var isBalanceLoading = true

    /// load visible subaccounts
    var subaccounts: [WalletItem] {
        wm?.subaccounts.filter { !($0.hidden) } ?? []
    }

    var watchOnly: Bool {
        wm?.account.isWatchonly ?? false
    }

    var headerIcon: UIImage {
        return UIImage(named: wm?.prominentNetwork.gdkNetwork?.mainnet == true ? "ic_wallet" : "ic_wallet_testnet")!.maskWithColor(color: .white)
    }
    /// Cached data
    var cachedTransactions = [Transaction]()
    var cachedBalance = [(String, Int64)]()

    /// reload by section with animation
    var reloadSections: (([WalletSection], Bool) -> Void)?

    /// to avoid duplication of observers in account detail
    var reloadAccountView: (() -> Void)?

    /// if no accounts show the layer
    var welcomeLayerVisibility: (() -> Void)?

    /// expandNewAccount
    var preselectAccount: ((Int) -> Void)?

    /// cell models
    var accountCellModels = [AccountCellModel]() {
        didSet {
            DispatchQueue.main.async {
                self.reloadSections?([WalletSection.account], false)
            }
        }
    }
    var txCellModels = [TransactionCellModel]() {
        didSet {
            DispatchQueue.main.async {
                self.reloadSections?( [WalletSection.transaction, .account ], false )
            }
        }
    }
    var balanceCellModel: BalanceCellModel? {
        didSet {
            isBalanceLoading = false
            DispatchQueue.main.async {
                self.reloadSections?([WalletSection.balance], false)
            }
        }
    }
    var alertCardCellModel = [AlertCardCellModel]() {
        didSet {
            DispatchQueue.main.async {
                self.reloadSections?([WalletSection.card], false)
            }
        }
    }
    var walletAssetCellModels: [WalletAssetCellModel] {
        return cachedBalance
            .sorted()
            .nonZero()
            .map { WalletAssetCellModel(assetId: $0.0, satoshi: $0.1) }
    }
    var remoteAlert: RemoteAlert?

    var balanceDisplayMode: BalanceDisplayMode = .denom

    var analyticsDone = false

    let bgq = DispatchQueue.global(qos: .background)

    init() {
        remoteAlert = RemoteAlertManager.shared.alerts(screen: .walletOverview, networks: wm?.activeNetworks ?? []).first
    }

    func loadSubaccounts() {
        self.accountCellModels = self.subaccounts.map { AccountCellModel(subaccount: $0, satoshi: $0.btc) }
    }

    func selectSubaccount(_ newAccount: WalletItem? = nil) {
        if let idx = self.accountCellModels.firstIndex(where: {$0.account == newAccount}) {
            self.preselectAccount?(idx)
        }
    }

    func loadBalances() {
        Guarantee()
            .compactMap { self.wm }
            .then(on: bgq) { $0.balances(subaccounts: self.subaccounts) }
            .map(on: bgq) { self.cachedBalance = AssetAmountList($0).sorted() }
            .map(on: bgq) { self.accountCellModels = self.subaccounts.map { AccountCellModel(subaccount: $0, satoshi: $0.btc) }}
            .compactMap(on: bgq) { self.sumBalances(self.subaccounts) }
            .compactMap(on: bgq) {  self.balanceCellModel = BalanceCellModel(satoshi: $0,
                                                                             cachedBalance: self.cachedBalance,
                                                                             mode: self.balanceDisplayMode ) }
            .done { _ in
                self.reloadAccountView?()
                self.welcomeLayerVisibility?()
                self.callAnalytics()
            }.catch { err in print(err) }
    }

    func loadTransactions(max: Int? = nil) {
        isTxLoading = true
        Guarantee()
            .compactMap { self.wm }
            .then(on: bgq) { $0.transactions(subaccounts: self.subaccounts) }
            .done(on: bgq) { txs in
                self.isTxLoading = false
                self.cachedTransactions = Array(txs.sorted(by: >).prefix(max ?? txs.count))
                self.txCellModels = self.cachedTransactions
                    .map { ($0, self.getNodeBlockHeight(subaccountHash: $0.subaccount!)) }
                    .map { TransactionCellModel(tx: $0.0, blockHeight: $0.1) }
            }.catch { err in print(err) }
    }

    func getNodeBlockHeight(subaccountHash: Int) -> UInt32 {
        if let subaccount = self.wm?.subaccounts.filter({ $0.hashValue == subaccountHash }).first,
            let network = subaccount.network,
            let session = self.wm?.sessions[network],
            let blockHeight = session.notificationManager?.blockHeight {
                return blockHeight
        }
        return 0
    }

    func sumBalances(_ subaccounts: [WalletItem]) -> Int64 {
        var total: Int64 = 0
        for subacc in subaccounts {
            let satoshi = subacc.satoshi?[subacc.gdkNetwork.getFeeAsset()] ?? 0
            if let converted = Balance.fromSatoshi(satoshi, assetId: subacc.gdkNetwork.getFeeAsset()) {
                total += converted.satoshi
            }
        }
        return total
    }

    func rotateBalanceDisplayMode() {
        var isBTC = false
        if let session = self.session, let settings = session.settings {
            isBTC = settings.denomination == .BTC
        }
        balanceDisplayMode = balanceDisplayMode.next(isBTC)
        loadBalances()
    }

    func loadDisputeCards() -> [AlertCardType] {
        guard let wm = wm else { return [] }
        var cards: [AlertCardType] = []
        wm.sessions.values.forEach { session in
            if session.logged && session.isResetActive ?? false,
                let twoFaReset = session.twoFactorConfig?.twofactorReset {
                let message = TwoFactorResetMessage(twoFactorReset: twoFaReset, network: session.gdkNetwork.network)
                if twoFaReset.isDisputeActive {
                    cards.append(.dispute(message))
                } else {
                    cards.append(.reset(message))
                }
            }
        }
        return cards
    }

    func loadMetadataCards() -> [AlertCardType] {
        guard let wm = wm else { return [] }
        var cards: [AlertCardType] = []
        // All sessions should login with the passphrase
        if wm.account.isEphemeral {
            // Bip39 ephemeral wallet
            cards.append(.ephemeralWallet)
        }
        if session?.gdkNetwork.mainnet == false {
            // Testnet wallet
            cards.append(AlertCardType.testnetNoValue)
        }
        // countly alerts
        if let remoteAlert = remoteAlert {
            cards.append(AlertCardType.remoteAlert(remoteAlert))
        }
        // Failure login session
        cards += wm.failureSessions
            .filter {
                switch $0.value {
                case TwoFactorCallError.failure(localizedDescription: let txt):
                    return txt != "id_login_failed"
                default:
                    return true
                }
            }.map { AlertCardType.login($0.key, $0.value) }
        return cards
    }

    func reloadAlertCards() {
        guard let wm = wm, let session = session else { return }
        var cards: [AlertCardType] = []
        Guarantee()
            .compactMap(on: bgq) { cards += self.loadMetadataCards() }
            .compactMap(on: bgq) { cards += self.loadDisputeCards() }
            .compactMap(on: bgq) { Balance.fromSatoshi(0, assetId: session.gdkNetwork.getFeeAsset())?.toFiat().0 == "n/a" }
            .map { $0 ? cards.append(AlertCardType.fiatMissing) : () }
            .then(on: bgq) { wm.loadSystemMessages() }
            .done { (messages: [SystemMessage]) in
                messages.forEach { msg in
                    if !msg.text.isEmpty {
                        cards.append(AlertCardType.systemMessage(msg))
                    }
                }
                self.alertCardCellModel = cards.map { AlertCardCellModel(type: $0) }
            }.catch { err in
                self.alertCardCellModel = cards.map { AlertCardCellModel(type: $0) }
                print(err.localizedDescription)
            }
    }

    func reload() {
        loadSubaccounts()
        loadBalances()
        loadTransactions(max: 10)
    }

    func handleEvent(_ notification: Notification) {
        guard let eventType = EventType(rawValue: notification.name.rawValue) else { return }
        print("..... \(eventType.rawValue)")
        switch eventType {
        case .Transaction:
            reload()
        case .Block:
            if cachedTransactions.filter({ $0.blockHeight == 0 }).first != nil {
                reload()
            }
        case .AssetsUpdated:
            reload()
        case .Network:
            guard let dict = notification.userInfo as NSDictionary? else { return }
            guard let connected = dict["connected"] as? Bool else { return }
            guard let loginRequired = dict["login_required"] as? Bool else { return }
            if connected == true && loginRequired == false {
                reload()
            }
        case .Settings, .Ticker, .TwoFactorReset:
            reload()
        default:
            break
        }
    }

    func onCreateAccount(_ wallet: WalletItem) {
        analyticsDone = false
        selectSubaccount(wallet)
    }

    func callAnalytics() {

        if analyticsDone == true { return }
        analyticsDone = true

        var accountsFunded: Int = 0
        subaccounts.forEach { item in
            let assets = item.satoshi ?? [:]
            for (_, value) in assets where value > 0 {
                    accountsFunded += 1
                    break
            }
        }
        let walletFunded: Bool = accountsFunded > 0
        let accounts: Int = subaccounts.count
        let accountsTypes: String = Array(Set(subaccounts.map { $0.type.rawValue })).sorted().joined(separator: ",")

        AnalyticsManager.shared.activeWallet(account: AccountsRepository.shared.current,
                                             walletData: AnalyticsManager.WalletData(walletFunded: walletFunded,
                                                                                     accountsFunded: accountsFunded,
                                                                                     accounts: accounts,
                                                                                     accountsTypes: accountsTypes))
    }
}
