import Foundation
import UIKit
import PromiseKit

enum PolicyCellType: String, CaseIterable {
    case Standard
    //case Instant
    case TwoFAProtected
    case TwoOfThreeWith2FA
    case NativeSegwit
    //case Taproot
    case Amp
}

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

    var unarchiveCreateDialog: (() -> Promise<Bool>)?

    var showAll = false {
        didSet {
            reloadSections?([SecuritySelectSection.policy], false)
        }
    }

    /// cell models
    func getPolicyCellModels() -> [PolicyCellModel] {
        let policies = policiesForAsset(for: asset, extended: showAll)
        return policies.map { PolicyCellModel.from(policy: $0) }
    }

    func policiesForAsset(for assetId: String, extended: Bool) -> [PolicyCellType] {
        let asset = WalletManager.current?.registry.info(for: asset)
        if asset?.amp ?? false {
            return [.Amp]
        }
        if !extended {
            return [.Standard, .TwoFAProtected]
        }
        if AssetInfo.btcId == assetId {
            return [.Standard, .TwoFAProtected, .TwoOfThreeWith2FA, .NativeSegwit]
        }
        return [.Standard, .TwoFAProtected, .Amp, .NativeSegwit]
    }

    func create(policy: PolicyCellType, asset: String) -> Promise<WalletItem> {
        guard
            let network = getNetwork(for: policy, liquid: asset != "btc"),
            let session = getSession(for: network) else {
            return Promise(error: GaError.GenericError("Invalid session"))
        }
        let type = getAccountType(for: policy)
        return Guarantee()
            .then { !session.logged ? self.registerSession(session: session) : Promise().asVoid() }
            .map { self.wm.subaccounts.filter { $0.gdkNetwork == session.gdkNetwork && $0.type == type && $0.hidden ?? false } }
            .then { accounts in self.wm.transactions(subaccounts: accounts).map { (accounts, $0) } }
            .then { self.createOrUnarchiveSubaccount(session: session, accounts: $0.0, txs: $0.1, policy: policy) }
            .then { res in self.wm.subaccounts().map { _ in res } }
    }

    func registerSession(session: SessionManager) -> Promise<Void> {
        return Guarantee()
            .compactMap { self.wm.prominentSession }
            .then { $0.getCredentials(password: "") }
            .then { cred in session.registerSW(cred).then { session.loginWithCredentials(cred) } }
            .then { _ in session.subaccounts(true) }
            .then { self.isUsedDefaultAccount(for: session, account: $0.first) }
            .then { !$0 ? session.updateSubaccount(subaccount: 0, hidden: true).asVoid() : Promise().asVoid() }
            .then { self.wm.subaccounts() }
            .asVoid()
    }

    func isUsedDefaultAccount(for session: SessionManager, account: WalletItem?) -> Promise<Bool> {
        guard let account = account else {
            return Promise() { $0.reject(GaError.GenericError("No subaccount found")) }
        }
        if account.gdkNetwork.multisig {
            // check balance for multisig
            return session.getBalance(subaccount: account.pointer, numConfs: 0)
                .compactMap { $0.map { $0.value }.reduce(0, +) > 0 }
        }
        // check bip44Discovered on singlesig
        return Promise() { $0.fulfill(account.bip44Discovered ?? false) }
    }

    func createOrUnarchiveSubaccount(session: SessionManager, accounts: [WalletItem], txs: [Transaction], policy: PolicyCellType) -> Promise<WalletItem> {
        let items = accounts.map { account in
            (account, txs.filter { $0.subaccount == account.hashValue }.count)
        }
        let unfunded = items.filter { $0.1 == 0 }.map { $0.0 }.first
        if let unfunded = unfunded {
            // automatically unarchive it
            return session.updateSubaccount(subaccount: unfunded.pointer, hidden: false)
                .then { session.subaccount(unfunded.pointer) }
        }
        let funded = items.filter { $0.1 > 0 }.map { $0.0 }.first
        if let funded = funded, let dialog = self.unarchiveCreateDialog {
            // ask user to unarchive o create a new one
            return dialog().then { (create: Bool) -> Promise<WalletItem> in
                if create {
                    return self.createSubaccount(session: session, policy: policy)
                } else {
                    return session.updateSubaccount(subaccount: funded.pointer, hidden: false)
                        .then { session.subaccount(funded.pointer) }
                }
            }
        }
        // automatically create a new account
        return self.createSubaccount(session: session, policy: policy)
    }

    func createSubaccount(session: SessionManager, policy: PolicyCellType) -> Promise<WalletItem> {
        let cellModel = PolicyCellModel.from(policy: policy)
        let params = CreateSubaccountParams(name: cellModel.name,
                               type: getAccountType(for: policy),
                               recoveryMnemonic: nil,
                               recoveryXpub: nil)
        return session.createSubaccount(params)
    }

    func getNetwork(for policy: PolicyCellType, liquid: Bool) -> NetworkSecurityCase? {
        let btc: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .bitcoinSS, .TwoFAProtected: .bitcoinMS,
         .TwoOfThreeWith2FA: .bitcoinMS, .NativeSegwit: .bitcoinSS, .Amp: .bitcoinMS]
        let test: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .testnetSS, .TwoFAProtected: .testnetMS,
         .TwoOfThreeWith2FA: .testnetMS, .NativeSegwit: .testnetSS, .Amp: .testnetMS]
        let lbtc: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .liquidSS, .TwoFAProtected: .liquidMS,
         .TwoOfThreeWith2FA: .liquidMS, .NativeSegwit: .liquidSS, .Amp: .liquidMS]
        let ltest: [PolicyCellType: NetworkSecurityCase] =
        [.Standard: .testnetLiquidSS, .TwoFAProtected: .testnetLiquidMS,
         .TwoOfThreeWith2FA: .testnetLiquidMS, .NativeSegwit: .testnetLiquidSS, .Amp: .testnetLiquidMS]
        if liquid && wm.testnet { return ltest[policy] }
        if liquid && !wm.testnet { return lbtc[policy] }
        if !liquid && wm.testnet { return test[policy] }
        return btc[policy]
    }

    func getSession(for network: NetworkSecurityCase) -> SessionManager? {
        wm.sessions[network.network]
    }

    func getAccountType(for policy: PolicyCellType) -> AccountType {
        switch policy {
        case .Standard:
             // singlesig legacy segwit
            return .legacy
        case .TwoFAProtected:
            return .standard
        case .TwoOfThreeWith2FA:
            return .twoOfThree
        case .NativeSegwit:
            return .segWit
        case .Amp:
            return .amp
        }
    }
}

struct CreateSubaccountParams: Codable {
    enum CodingKeys: String, CodingKey {
        case name = "name"
        case type = "type"
        case recoveryMnemonic = "recovery_mnemonic"
        case recoveryXpub = "recovery_xpub"
    }
    let name: String
    let type: AccountType
    let recoveryMnemonic: String?
    let recoveryXpub: String?
}
