import Foundation
import PromiseKit

class MnemonicViewModel {

    func validateMnemonic(_ mnemonic: String) -> Promise<Void> {
        if let validated = try? gaios.validateMnemonic(mnemonic: mnemonic),
           validated {
            return Promise().asVoid()
        }
        return Promise(error: LoginError.invalidMnemonic())
    }

    func getXpubHashId(credentials: Credentials, wm: WalletManager) -> Promise<String> {
        let session = wm.prominentSession!
        return Guarantee()
            .then { session.connect() }
            .compactMap { session.walletIdentifier(session.gdkNetwork.network, credentials: credentials) }
            .compactMap { $0.xpubHashId }
    }

    func restore(credentials: Credentials, testnet: Bool, xpubHashId: String? = nil) -> Promise<Void> {
        let name = AccountsManager.shared.getUniqueAccountName(testnet: testnet)
        let account = Account(name: name, network: testnet ? "testnet" : "mainnet")
        let wm = WalletManager.getOrAdd(for: account)
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.getXpubHashId(credentials: credentials, wm: wm) }
            .map {
                if let xpubHashId = xpubHashId, xpubHashId != $0 {
                    throw LoginError.walletMismatch()
                }
            }.then(on: bgq) { wm.restore(credentials) }
            .compactMap {
                AccountsManager.shared.current = account
                AnalyticsManager.shared.restoreWallet(account: account)
                return ()
            }
            .then(on: bgq) { wm.login(credentials) }
            .asVoid()
    }
}
