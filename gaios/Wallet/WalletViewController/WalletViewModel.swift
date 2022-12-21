import Foundation
import UIKit
import PromiseKit

class WalletViewModel {

    var wm: WalletManager? { WalletManager.current }

    var session: SessionManager? {
        return wm?.prominentSession
    }

    var isTxLoading = true // on init is always true

    /// load visible subaccounts
    var subaccounts: [WalletItem] {
        wm?.subaccounts.filter { !($0.hidden ?? false) } ?? []
    }

    var headerIcon: UIImage {
        return UIImage(named: wm?.currentSubaccount?.gdkNetwork.mainnet == true ? "ic_wallet" : "ic_wallet_testnet")!.maskWithColor(color: .white)
    }
    /// Cached data
    var cachedSubaccounts = [WalletItem]()
    var cachedTransactions = [Transaction]()
    var cachedBalance = [(String, Int64)]()

    /// reload by section with animation
    var reloadSections: (([WalletSection], Bool) -> Void)?

    /// to avoid duplication of observers in account detail
    var reloadAccountView: (() -> Void)?

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
    var alertCardCellModel = [AlertCardCellModel]() {
        didSet {
            reloadSections?([WalletSection.card], false)
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

    init() {
        self.remoteAlert = RemoteAlertManager.shared.getAlert(screen: .overview, network: AccountsManager.shared.current?.networkName)
    }

    func loadSubaccounts() {
        cachedSubaccounts = self.subaccounts
        wm?.balances(subaccounts: self.subaccounts)
            .done { _ in
                let models = self.subaccounts.map { AccountCellModel(subaccount: $0) }
                if models.count > 0 { self.accountCellModels = models }
                self.getAssets()
                self.getTransactions(max: 10)
            }.catch { err in
                print(err)
            }
    }

    func getTransactions(subaccounts: [WalletItem]? = nil, max: Int? = nil) {
        let accounts = subaccounts != nil ? subaccounts : self.subaccounts
        isTxLoading = true
        wm?.transactions(subaccounts: accounts ?? [])
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
        if let subaccount = self.wm?.subaccounts.filter({ $0.hashValue == subaccountHash }).first,
            let network = subaccount.network,
            let session = self.wm?.sessions[network],
            let blockHeight = session.notificationManager?.blockHeight {
                return blockHeight
        }
        return 0
    }

    func getAssets() {
        wm?.balances(subaccounts: self.subaccounts)
            .done { amounts in
                self.cachedBalance = AssetAmountList(amounts).sorted()

                /// FIX total balance match
                // let total = amounts.filter({$0.0 == "btc"}).map {$0.1}.reduce(0, +)
                var total: Int64 = 0
                for subacc in self.subaccounts {
                    let satoshi = subacc.satoshi?[subacc.gdkNetwork.getFeeAsset()] ?? 0
                    if let converted = Balance.fromSatoshi(satoshi) {
                        total += converted.satoshi
                    }
                }
                self.balanceCellModel = BalanceCellModel(satoshi: total,
                                                         cachedBalance: self.cachedBalance,
                                                         mode: self.balanceDisplayMode
                )
                self.reloadAccountView?()
            }.catch { err in
                print(err)
            }
    }

    func rotateBalanceDisplayMode() {
        var isBTC = false
        if let session = self.session, let settings = session.settings {
            isBTC = settings.denomination == .BTC
        }
        balanceDisplayMode = balanceDisplayMode.next(isBTC)
        getAssets()
    }

    func reloadAlertCards() {
        var cards: [AlertCardType] = []
        guard let wm = wm else { return }
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
        // All sessions should login with the passphrase
        if AccountsManager.shared.current?.isEphemeral ?? false {
            // Bip39 ephemeral wallet
            cards.append(.ephemeralWallet)
        }
        if session?.gdkNetwork.mainnet == false {
            // Testnet wallet
            cards.append(AlertCardType.testnetNoValue)
        }
        if Balance.fromSatoshi(0)?.toFiat().0 == "n/a" {
            // Price provider not available
            cards.append(AlertCardType.fiatMissing)
        }
        /// countly alerts
        if let remoteAlert = remoteAlert {
            cards.append(AlertCardType.remoteAlert(remoteAlert))
        }

        /// load system messages
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
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

    func handleEvent(_ notification: Notification) {
        guard let eventType = EventType(rawValue: notification.name.rawValue) else { return }

        print("..... \(eventType.rawValue)")
        switch eventType {
        case .Transaction:
            loadSubaccounts()
        case .Block:
//            if cachedTransactions.filter({ $0.blockHeight == 0 }).first != nil {
                loadSubaccounts()
//            }
        case .AssetsUpdated:
            loadSubaccounts()
        case .Network:
            guard let dict = notification.userInfo as NSDictionary? else { return }
            guard let connected = dict["connected"] as? Bool else { return }
            guard let loginRequired = dict["login_required"] as? Bool else { return }
            if connected == true && loginRequired == false {
                DispatchQueue.main.async { [weak self] in
                    self?.loadSubaccounts()
                }
            }
        case .Settings, .Ticker, .TwoFactorReset:
            loadSubaccounts()
        default:
            break
        }
        reloadAlertCards()
    }
}
