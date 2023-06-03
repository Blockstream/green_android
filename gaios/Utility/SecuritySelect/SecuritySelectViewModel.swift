import Foundation
import UIKit
import PromiseKit
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

    var unarchiveCreateDialog: (() -> Promise<Bool>)?

    var showAll = false {
        didSet {
            reloadSections?([SecuritySelectSection.policy], false)
        }
    }

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

    func create(policy: PolicyCellType, asset: String, params: CreateSubaccountParams?) -> Promise<WalletItem> {
        let network = policy.getNetwork(testnet: wm.testnet, liquid: asset != "btc")!
        let prominentSession = wm.prominentSession!
        if network.lightning, let session = wm.lightningSession {
            if session.logged {
                return Promise(error: GaError.GenericError("Lightning account already exist"))
            }
            return Guarantee()
                .then { session.connect() }
                .then { prominentSession.getCredentials(password: "")}
                .then { session.loginUser($0) }
                .then { _ in self.wm.subaccounts() }
                .then { _ in session.subaccount(0) }
        }
        if let session = getSession(for: network) {
            let params = params ?? CreateSubaccountParams(name: uniqueName(policy.accountType, liquid: asset != "btc"),
                                                          type: policy.accountType,
                                                          recoveryMnemonic: nil,
                                                          recoveryXpub: nil)
            return Guarantee()
                .then { !session.logged ? self.registerSession(session: session) : Promise().asVoid() }
                .map { self.wm.subaccounts.filter { $0.gdkNetwork == session.gdkNetwork && $0.type == params.type && $0.hidden} }
                .then { accounts in self.wm.transactions(subaccounts: accounts).map { (accounts, $0) } }
                .then { self.createOrUnarchiveSubaccount(session: session, accounts: $0.0, txs: $0.1, params: params) }
                .then { res in self.wm.subaccounts().map { _ in res } }
        }
        return Promise(error: GaError.GenericError("Invalid session"))
    }

    func device() -> HWDevice {
        return wm.account.isJade ? .defaultJade(fmwVersion: nil) : .defaultLedger()
    }

    func registerSession(session: SessionManager) -> Promise<Void> {
        if session.gdkNetwork.liquid && wm.account.isLedger {
            return Promise() { seal in seal.reject(GaError.GenericError("Liquid not supported on Ledger Nano X"))}
        } else if wm.account.isHW {
            let hw = wm.account.isJade ? HWDevice.defaultJade(fmwVersion: nil) : HWDevice.defaultLedger()
            return self.registerSession(session: session, credentials: nil, hw: hw)
        } else if let prominentSession = wm.prominentSession {
            return prominentSession.getCredentials(password: "")
                .then { self.registerSession(session: session, credentials: $0, hw: nil) }
        } else {
            return Promise() { seal in seal.reject(GaError.GenericError("Invalid session"))}
        }
    }

    func registerSession(session: SessionManager, credentials: Credentials? = nil, hw: HWDevice? = nil) -> Promise<Void> {
        return Promise()
            .then { session.register(credentials: credentials, hw: hw) }
            .then { _ in session.loginUser(credentials: credentials, hw: hw) }
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

    func createOrUnarchiveSubaccount(session: SessionManager, accounts: [WalletItem], txs: [Transaction], params: CreateSubaccountParams) -> Promise<WalletItem> {
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
                    return session.createSubaccount(params)
                } else {
                    return session.updateSubaccount(subaccount: funded.pointer, hidden: false)
                        .then { session.subaccount(funded.pointer) }
                }
            }
        }
        // automatically create a new account
        return session.createSubaccount(params)
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
