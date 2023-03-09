import Foundation
import PromiseKit

class SetPinViewModel {

    var credentials: Credentials
    var testnet: Bool
    var xpubHashId: String?

    init(credentials: Credentials, testnet: Bool, xpubHashId: String? = nil) {
        self.credentials = credentials
        self.testnet = testnet
        self.xpubHashId = xpubHashId
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
        let account = Account(name: name, network: mainNetwork.network)
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        return Guarantee()
            .compactMap { wm.prominentSession }
            .then { $0.connect() }
            .then { self.getXpubHashId(session: wm.prominentSession!) }
            .map {
                if let xpubHashId = self.xpubHashId, xpubHashId != $0 {
                    throw LoginError.walletMismatch()
                }
            }.then { wm.restore(self.credentials, forceJustRestored: self.xpubHashId != nil) }
            .map { AnalyticsManager.shared.restoreWallet(account: account) }
            .then { wm.login(self.credentials) }
            .map { wm.account.attempts = 0}
            .then { wm.account.addPin(session: wm.prominentSession!, pin: pin, mnemonic: self.credentials.mnemonic!) }
            .asVoid()
    }

    func create(pin: String) -> Promise<Void> {
        let name = AccountsRepository.shared.getUniqueAccountName(testnet: testnet)
        let mainNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let account = Account(name: name, network: mainNetwork.network)
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        return Guarantee()
            .then { [self] in wm.create(credentials) }
            .then { [self] in wm.login(credentials) }
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
    }

}
