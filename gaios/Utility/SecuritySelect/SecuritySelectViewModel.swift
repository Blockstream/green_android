import Foundation
import UIKit
import PromiseKit

enum PolicyCellType: String, CaseIterable {
    case Standard
    case Instant
    case TwoFAProtected
    case TwoOfThreeWith2FA
    case NativeSegwit
    case Taproot
}

class SecuritySelectViewModel {

    var accounts: [WalletItem]
    var assetCellModel: AssetSelectCellModel?
    var asset: String {
        didSet {
            assetCellModel = AssetSelectCellModel(assetId: asset, satoshi: 0)
        }
    }
    var wm: WalletManager { WalletManager.current! }

    init(accounts: [WalletItem], asset: String) {
        self.accounts = accounts
        self.asset = asset
        self.assetCellModel = AssetSelectCellModel(assetId: asset, satoshi: 0)
    }

    /// reload by section with animation
    var reloadSections: (([SecuritySelectSection], Bool) -> Void)?

    // on success
    var success: (() -> Void)?

    // on errors
    var error: ((Error) -> Void)?

    var showAll = false {
        didSet {
            reloadSections?([SecuritySelectSection.policy], false)
        }
    }

    /// cell models
    func getPolicyCellModels() -> [PolicyCellModel] {
        let cells = PolicyCellType.allCases.map { PolicyCellModel.from(policy: $0) }
        return showAll ? cells : Array(cells[0...2])
    }

    func create(policy: PolicyCellType, asset: String) {
        let isLiquid = asset != "btc"
        let network = getNetwork(for: policy, liquid: isLiquid)
        guard let session = getSession(for: network) else {
            self.error?(GaError.GenericError("Invalid session"))
            return
        }
        guard session.logged else {
            // create a new session with current mnemonic
            registerSession(session: session)
                .then { self.createOrUnarchiveSubaccount(session: session, policy: policy)}
                .then { self.wm.subaccounts() }
                .asVoid()
                .done { self.success?() }
                .catch { err in self.error?(err) }
            return

        }
        // use an existing session
        createOrUnarchiveSubaccount(session: session, policy: policy)
            .then { self.wm.subaccounts() }
            .asVoid()
            .done { self.success?() }
            .catch { err in self.error?(err) }
    }

    func registerSession(session: SessionManager) -> Promise<Void> {
        return Guarantee()
            .compactMap { self.wm.prominentSession }
            .then { $0.getCredentials(password: "") }
            .get { session.registerSW($0).asVoid() }
            .then { session.loginWithCredentials($0) }
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

    func createOrUnarchiveSubaccount(session: SessionManager, policy: PolicyCellType) -> Promise<Void> {
        if policy == .TwoFAProtected {
            return session.subaccount(0)
                .then { account in
                    session.getBalance(subaccount: 0, numConfs: 0)
                        .compactMap { account.hidden ?? false && $0.map { $0.value }.reduce(0, +) == 0 }
                }
                .then { $0 ? session.updateSubaccount(subaccount: 0, hidden: false).asVoid() : self.createSubaccount(session: session, policy: policy) }
        }
        return createSubaccount(session: session, policy: policy)
    }

    func createSubaccount(session: SessionManager, policy: PolicyCellType) -> Promise<Void> {
        let cellModel = PolicyCellModel.from(policy: policy)
        let params = CreateSubaccountParams(name: cellModel.name,
                               type: getAccountType(for: policy),
                               recoveryMnemonic: nil,
                               recoveryXpub: nil)
        let data = try? JSONEncoder().encode(params)
        let dict = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
        return session.createSubaccount(details: dict ?? [:]).asVoid()
    }

    func getNetwork(for policy: PolicyCellType, liquid: Bool) -> NetworkSecurityCase {
        switch policy {
        case .Standard:
             // singlesig legacy segwit
            return wm.testnet ? NetworkSecurityCase.testnetSS : NetworkSecurityCase.bitcoinSS
        case .Instant:
            return wm.testnet ? NetworkSecurityCase.testnetSS : NetworkSecurityCase.bitcoinSS
        case .TwoFAProtected:
            return wm.testnet ? NetworkSecurityCase.testnetMS : NetworkSecurityCase.bitcoinMS
        case .TwoOfThreeWith2FA:
            return wm.testnet ? NetworkSecurityCase.testnetMS : NetworkSecurityCase.bitcoinMS
        case .NativeSegwit:
            return wm.testnet ? NetworkSecurityCase.testnetSS : NetworkSecurityCase.bitcoinSS
        case .Taproot:
            return wm.testnet ? NetworkSecurityCase.testnetSS : NetworkSecurityCase.bitcoinSS
        }
    }

    func getSession(for network: NetworkSecurityCase) -> SessionManager? {
        wm.sessions[network.network]
    }

    func getAccountType(for policy: PolicyCellType) -> AccountType {
        switch policy {
        case .Standard:
             // singlesig legacy segwit
            return .legacy
        case .Instant:
            return .legacy
        case .TwoFAProtected:
            return .standard
        case .TwoOfThreeWith2FA:
            return .twoOfThree
        case .NativeSegwit:
            return .segWit
        case .Taproot:
            return .taproot
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
