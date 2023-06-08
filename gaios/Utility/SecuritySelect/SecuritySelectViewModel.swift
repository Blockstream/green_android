import Foundation
import UIKit

import gdk
import hw
import greenaddress


class SecuritySelectViewModel {

    var assetCellModel: AssetSelectCellModel?
    var asset: String {
        didSet {
            assetCellModel = AssetSelectCellModel(assetId: asset, satoshi: 0)
            reloadSections?([.asset], false)
        }
    }
    private var wm: WalletManager { WalletManager.current! }

    init(asset: String) {
        self.asset = asset
        self.assetCellModel = AssetSelectCellModel(assetId: asset, satoshi: 0)
    }

    /// reload by section with animation
    var reloadSections: (([SecuritySelectSection], Bool) -> Void)?

    // on success
    var success: (() -> Void)?

    // on errors
    var error: ((Error) -> Void)?

    var unarchiveCreateDialog: (( @escaping (Bool) -> ()) -> ())?

    var showAll = false {
        didSet {
            reloadSections?([SecuritySelectSection.policy], false)
        }
    }

    let bgq = DispatchQueue.global(qos: .background)

    func isAdvancedEnable() -> Bool {
        let asset = WalletManager.current?.registry.info(for: asset)
        if asset?.amp ?? false {
            return false
        } else {
            return true
        }
    }

    /// cell models
    func getPolicyCellModels() -> [PolicyCellModel] {
        let policies = policiesForAsset(for: asset, extended: showAll)
        return policies.map { PolicyCellModel.from(policy: $0) }
    }

    func policiesForAsset(for assetId: String, extended: Bool) -> [PolicyCellType] {
        let asset = WalletManager.current?.registry.info(for: asset)
        if asset?.amp ?? false { // amp liquid asset
            return [.Amp]
        } else if AssetInfo.btcId == assetId { // btc
            var list: [PolicyCellType] = [.Standard, .NativeSegwit, .Lightning, .TwoFAProtected, .TwoOfThreeWith2FA]
            if !extended {
                list = [.Standard, .Lightning, .TwoFAProtected]
            }
            if !AppSettings.shared.experimental {
                list.removeAll(where: { $0 == .Lightning })
            }
            return list
        } else { // liquid
            var list: [PolicyCellType] = [.Standard, .NativeSegwit, .TwoFAProtected, .Amp]
            if !extended {
                list = [.Standard, .TwoFAProtected]
            }
            return list
        }
    }

    func create(policy: PolicyCellType, asset: String, params: CreateSubaccountParams?) async throws -> WalletItem? {
        let network = policy.getNetwork(testnet: wm.testnet, liquid: asset != "btc")!
        let prominentSession = wm.prominentSession!
        if network.lightning, let session = wm.lightningSession {
            if session.logged {
                throw GaError.GenericError("Lightning account already exist")
            } else if wm.account.isHW {
                throw GaError.GenericError("Cannot create a lightning account for an hardware wallet")
            }
            try await session.connect()
            guard let credentials = try await prominentSession.getCredentials(password: "") else { throw GaError.GenericError() }
            try await session.register(credentials: credentials)
            try await wm.subaccounts()
            return try await session.subaccount(0)
        } else if let session = getSession(for: network) {
            let params = params ?? CreateSubaccountParams(name: uniqueName(policy.accountType, liquid: asset != "btc"),
                                                          type: policy.accountType,
                                                          recoveryMnemonic: nil,
                                                          recoveryXpub: nil)
            if !session.logged {
                try await registerSession(session: session)
            }
            let accounts = self.wm.subaccounts.filter { $0.gdkNetwork == session.gdkNetwork && $0.type == params.type && $0.hidden }
            let txs = try await self.wm.transactions(subaccounts: accounts)
            let account = try await self.createOrUnarchiveSubaccount(session: session, accounts: accounts, txs: txs, params: params)
            try await self.wm.subaccounts()
            return account
        } else {
           throw GaError.GenericError("Invalid session")
        }
    }

    func device() -> HWDevice {
        return wm.account.isJade ? .defaultJade(fmwVersion: nil) : .defaultLedger()
    }

    func registerSession(session: SessionManager) async throws {
        if session.gdkNetwork.liquid && wm.account.isLedger {
            throw GaError.GenericError("Liquid not supported on Ledger Nano X")
        } else if wm.account.isHW {
            let hw = wm.account.isJade ? HWDevice.defaultJade(fmwVersion: nil) : HWDevice.defaultLedger()
            try await registerSession(session: session, credentials: nil, hw: hw)
        } else if let prominentSession = wm.prominentSession {
            let credentials = try await prominentSession.getCredentials(password: "")
            try await registerSession(session: session, credentials: credentials, hw: nil)
        } else {
            throw GaError.GenericError("Invalid session")
        }
    }

    func registerSession(session: SessionManager, credentials: Credentials? = nil, hw: HWDevice? = nil) async throws {
        try await session.register(credentials: credentials, hw: hw)
        try await session.loginUser(credentials: credentials, hw: hw, restore: false)
        let subaccounts = try await session.subaccounts(true)
        let used = try await self.isUsedDefaultAccount(for: session, account: subaccounts.first)
        if used {
            try await session.updateSubaccount(subaccount: 0, hidden: true)
        }
        try await wm.subaccounts()
    }

    func isUsedDefaultAccount(for session: SessionManager, account: WalletItem?) async throws -> Bool {
        guard let account = account else {
            throw GaError.GenericError("No subaccount found")
        }
        if account.gdkNetwork.multisig {
            // check balance for multisig
            let balance = try await session.getBalance(subaccount: account.pointer, numConfs: 0)
            return balance.map { $0.value }.reduce(0, +) > 0
        }
        // check bip44Discovered on singlesig
        return account.bip44Discovered ?? false
    }

    func createOrUnarchiveSubaccount(session: SessionManager, accounts: [WalletItem], txs: [Transaction], params: CreateSubaccountParams) async throws -> WalletItem? {
        let items = accounts.map { account in
            (account, txs.filter { $0.subaccount == account.hashValue }.count)
        }
        let unfunded = items.filter { $0.1 == 0 }.map { $0.0 }.first
        if let unfunded = unfunded {
            // automatically unarchive it
            try await session.updateSubaccount(subaccount: unfunded.pointer, hidden: false)
            try await session.subaccount(unfunded.pointer)
        }
        let funded = items.filter { $0.1 > 0 }.map { $0.0 }.first
        if let funded = funded, let dialog = self.unarchiveCreateDialog {
            // ask user to unarchive o create a new one
            dialog() { create in
                Task {
                    if create {
                        try? await session.createSubaccount(params)
                    } else {
                        try? await session.updateSubaccount(subaccount: funded.pointer, hidden: false)
                        try? await session.subaccount(funded.pointer)
                    }
                }
            }
            return nil
        }
        // automatically create a new account
        return try await session.createSubaccount(params)
    }

    func getSession(for network: NetworkSecurityCase) -> SessionManager? {
        wm.sessions[network.network]
    }

    func uniqueName(_ type: AccountType, liquid: Bool) -> String {
        let network = liquid ? " Liquid " : " "
        let counter = wm.subaccounts.filter { $0.type == type && $0.gdkNetwork.liquid == liquid }.count
        if counter > 0 {
            return "\(type.string)\(network)\(counter+1)"
        }
        return "\(type.string)\(network)"
    }
}
