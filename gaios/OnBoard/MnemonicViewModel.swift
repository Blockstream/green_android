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

    func restore(mnemonic: String, password: String, testnet: Bool) -> Promise<Void> {
        let name = AccountsManager.shared.getUniqueAccountName(testnet: testnet)
        let account = Account(name: name, network: testnet ? "testnet" : "mainnet", isSingleSig: true)
        let wm = WalletManager.getOrAdd(for: account)
        let credentials = Credentials(mnemonic: mnemonic, password: password)
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { wm.restore(credentials) }
            .compactMap {
                AccountsManager.shared.current = account
                AnalyticsManager.shared.restoreWallet(account: account)
                return ()
            }
            .then(on: bgq) { wm.login(credentials) }
            .asVoid()
    }
}
