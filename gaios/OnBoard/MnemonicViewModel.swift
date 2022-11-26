import Foundation
import PromiseKit

class MnemonicViewModel {

    // on success
    var success: (() -> Void)?

    // on errors
    var error: ((Error) -> Void)?

    func validateMnemonic(_ mnemonic: String) throws {
        let validated = try? gaios.validateMnemonic(mnemonic: mnemonic)
        if validated ?? false {
            throw LoginError.invalidMnemonic()
        }
    }

    func restore(mnemonic: String, password: String, testnet: Bool) {
        let name = AccountsManager.shared.getUniqueAccountName(testnet: testnet)
        let account = Account(name: name, network: testnet ? "testnet" : "mainnet", isSingleSig: true)
        let wm = WalletManager.getOrAdd(for: account)
        let credentials = Credentials(mnemonic: mnemonic, password: password)
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .then(on: bgq) { wm.restore(credentials) }
            .compactMap {
                AccountsManager.shared.current = account
                AnalyticsManager.shared.restoreWallet(account: account)
                return ()
            }
            .then(on: bgq) { wm.login(credentials) }
            .done { self.success?() }
            .catch { err in self.error?(err) }
    }

    func create(mnemonic: String, network: NetworkSecurityCase) {
        guard let wm = WalletManager.current else { return }
        let credentials = Credentials(mnemonic: mnemonic)
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .then(on: bgq) { wm.restore(credentials) }
            .done {
                AnalyticsManager.shared.createWallet(account: AccountsManager.shared.current)
                self.success?()
            }.catch { err in self.error?(err) }
    }
}
