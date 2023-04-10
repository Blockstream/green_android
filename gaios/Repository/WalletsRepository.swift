import Foundation
import gdk

class WalletsRepository {

    static let shared = WalletsRepository()

    // Store all the Wallet available for each account id
    var wallets = [String: WalletManager]()

    func add(for account: Account, wm: WalletManager? = nil) {
        if let wm = wm {
            wallets[account.id] = wm
            return
        }
        let network = NetworkSecurityCase(rawValue: account.networkName)
        let wm = WalletManager(account: account, prominentNetwork: network)
        wallets[account.id] = wm
    }

    func get(for accountId: String) -> WalletManager? {
        return wallets[accountId]
    }

    func get(for account: Account) -> WalletManager? {
        get(for: account.id)
    }

    func getOrAdd(for account: Account) -> WalletManager {
        if !wallets.keys.contains(account.id) {
            add(for: account)
        }
        return get(for: account)!
    }

    func delete(for accountId: String) {
        wallets.removeValue(forKey: accountId)
    }

    func delete(for account: Account?) {
        if let account = account {
            delete(for: account.id)
        }
    }

    func delete(for wm: WalletManager) {
        if let index = wallets.firstIndex(where: { $0.value === wm }) {
            wallets.remove(at: index)
        }
    }

    func change(wm: WalletManager, for account: Account) {
        delete(for: wm)
        add(for: account, wm: wm)
    }
}
