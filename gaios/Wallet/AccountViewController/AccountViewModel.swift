import Foundation
import UIKit

import gdk

enum AmpEducationalMode {
    case table
    case header
    case hidden
}

class AccountViewModel {

    private var wm: WalletManager { WalletManager.current! }
    private var cachedBalance: AssetAmountList
    var cachedTransactions = [Transaction]()
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
        cachedBalance.amounts.first(where: { $0.0 == account.gdkNetwork.getFeeAsset() })?.1 ?? account.btc ?? 0
    }

    var inboundCellModels: [LTInboundCellModel] {
        if isLightning {
            let amount = wm.lightningSession?.nodeState?.inboundLiquiditySatoshi ?? 0
            return [LTInboundCellModel(amount: amount)]
        }
        return []
    }

    var sweepCellModels: [LTSweepCellModel] {
        let amount = wm.lightningSession?.nodeState?.onchainBalanceSatoshi
        if isLightning, let amount = amount, amount > 0 {
            return [LTSweepCellModel(amount: amount)]
        }
        return []
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

    var isLightning: Bool {
        return account.type == .lightning
    }

    var ampEducationalMode: AmpEducationalMode {
        if account.type != .amp {
            return .hidden
        } else {
            let satoshi = account.satoshi?[account.gdkNetwork.getFeeAsset()] ?? 0
            let assets = cachedBalance.amounts
            if satoshi > 0 || assets.count > 1 {
                return .header
            } else {
                return .table
            }
        }
    }

    var accountCellModels: [AccountCellModel]
    var txCellModels = [TransactionCellModel]()
    var assetCellModels = [WalletAssetCellModel]()

    init(model: AccountCellModel, account: WalletItem, cachedBalance: AssetAmountList) {
        self.accountCellModels = [model]
        self.account = account
        self.cachedBalance = cachedBalance
    }

    func getTransactions(restart: Bool = true, max: Int? = nil) async throws {
        if fetchingTxs {
            return
        }
        fetchingTxs = true
        do {
            let txs = try await wm.transactions(subaccounts: [account], first: (restart == true) ? 0 : cachedTransactions.count)
            if restart {
                page = 0
                cachedTransactions = []
            }
            if txs.count > 0 {
                page += 1
            }
            cachedTransactions += txs
            cachedTransactions = Array((cachedTransactions)
                .sorted(by: >)
                .prefix(max ?? cachedTransactions.count))
            txCellModels = cachedTransactions
                .map { ($0, getNodeBlockHeight(subaccountHash: $0.subaccount!)) }
                .map { TransactionCellModel(tx: $0.0, blockHeight: $0.1) }
        } catch { print(error) }
        fetchingTxs = false
    }

    func getBalance() async throws {
        if let balances = try? await wm.balances(subaccounts: [account]) {
            cachedBalance = AssetAmountList(balances)
        }
        accountCellModels = [AccountCellModel(account: account, satoshi: satoshi)]
        assetCellModels = cachedBalance.amounts.map { WalletAssetCellModel(assetId: $0.0, satoshi: $0.1) }
    }

    func getNodeBlockHeight(subaccountHash: Int) -> UInt32 {
        if let subaccount = self.wm.subaccounts.filter({ $0.hashValue == subaccountHash }).first,
           let network = subaccount.network,
           let session = self.wm.sessions[network] {
            return session.blockHeight
        }
        return 0
    }

    func archiveSubaccount() async throws {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return
        }
        try await session.updateSubaccount(subaccount: account.pointer, hidden: true)
        account = try await wm.subaccount(account: account)
        accountCellModels = [AccountCellModel(account: account, satoshi: satoshi)]
    }

    func removeSubaccount() async throws {
        guard let prominentSession = wm.prominentSession,
                let session = wm.sessions[account.gdkNetwork.network] else {
            return
        }
        if let credentials = try await prominentSession.getCredentials(password: ""),
           let walletId = session.walletIdentifier(credentials: credentials)
        {
            try await session.disconnect()
            session.removeDatadir(walletHashId: walletId.walletHashId )
            LightningRepository.shared.remove(for: walletId.walletHashId)
            _ = try await wm.subaccounts()
        }
    }

    func renameSubaccount(name: String) async throws {
        guard let session = wm.sessions[account.gdkNetwork.network] else {
            return
        }
        try await session.renameSubaccount(subaccount: account.pointer, newName: name)
        account = try await wm.subaccount(account: account)
        accountCellModels = [AccountCellModel(account: account, satoshi: satoshi)]
    }

    func ltRecoverFundsViewModel() -> LTRecoverFundsViewModel {
        LTRecoverFundsViewModel(wallet: account,
                                address: nil,
                                amount: wm.lightningSession?.nodeState?.onchainBalanceSatoshi)
    }
    
    func ltRecoverFundsViewModel(tx: Transaction) -> LTRecoverFundsViewModel {
        let amount = tx.amounts["btc"].map {UInt64($0)}
        return LTRecoverFundsViewModel(wallet: account,
                                address: tx.addressees.first?.address,
                                amount: amount)
    }
}
