import Foundation
import PromiseKit
import gdk

class SetPinViewModel {

    var credentials: Credentials
    var testnet: Bool
    var restoredAccount: Account?

    init(credentials: Credentials, testnet: Bool, restoredAccount: Account? = nil) {
        self.credentials = credentials
        self.testnet = testnet
        self.restoredAccount = restoredAccount
    }

    func getXpubHashId(session: SessionManager) -> Promise<String> {
        return Guarantee()
            .then { session.connect() }
            .compactMap { session.walletIdentifier(session.gdkNetwork.network, credentials: self.credentials) }
            .compactMap { $0.xpubHashId }
    }

    func restore(pin: String) -> Promise<Void> {
        let name = AccountsRepository.shared.getUniqueAccountName(testnet: testnet)
        let mainNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let account = restoredAccount ?? Account(name: name, network: mainNetwork.network)
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        return Promise()
            .compactMap { wm.prominentSession }
            .then { $0.connect() }
            .then { self.getXpubHashId(session: wm.prominentSession!) }
            .map { xpub in
                // Avoid to restore an different wallet if restoredAccount is defined
                if let restoredAccount = self.restoredAccount,
                   let xpubHashId = restoredAccount.xpubHashId, xpubHashId != xpub {
                    throw LoginError.walletMismatch()
                }
                // Avoid to restore an existing wallets
                if let prevAccount = AccountsRepository.shared.find(xpubHashId: xpub),
                   prevAccount.gdkNetwork == account.gdkNetwork && !account.isHW {
                   throw LoginError.walletsJustRestored()
                }
            }.then { wm.login(credentials: self.credentials) }
            .map { _ in
                if let multisig = wm.activeNetworks.first(where: { !$0.singleSig }) {
                    wm.account.network = multisig.chain
                    wm.account.isSingleSig = multisig.singleSig
                    wm.prominentNetwork = multisig
                }
                wm.account.attempts = 0
                AnalyticsManager.shared.restoreWallet(account: account)
            }.then { wm.account.addPin(session: wm.prominentSession!, pin: pin, mnemonic: self.credentials.mnemonic!) }
            .asVoid()
    }

    func create(pin: String) -> Promise<Void> {
        let name = AccountsRepository.shared.getUniqueAccountName(testnet: testnet)
        let mainNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let account = Account(name: name, network: mainNetwork.network)
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        return Guarantee()
            .then { [self] in wm.create(credentials) }
            .then { [self] in wm.login(credentials: credentials) }
            .then { wm.account.addPin(session: wm.prominentSession!, pin: pin, mnemonic: self.credentials.mnemonic!) }
    }

    func setup(pin: String) -> Promise<Void> {
        guard let wm = WalletManager.current,
            let session = wm.prominentSession,
              let mnemonic = credentials.mnemonic
        else { return Promise() { $0.reject(LoginError.failed()) } }
        return Guarantee()
            .then { session.connect() }
            .then { wm.account.addPin(session: session, pin: pin, mnemonic: mnemonic) }
            .map { wm.account.attempts = 0 }
    }

}
