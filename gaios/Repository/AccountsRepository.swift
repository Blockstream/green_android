import Foundation

class AccountsRepository {

    let attrAccount = "AccountsManager_Account"
    let attrService = "AccountsManager_Service"

    static let shared = AccountsRepository()
    let storage: KeychainStorage

    init() {
        storage = KeychainStorage(account: attrAccount, service: attrService)
    }

    // List of saved accounts with cache
    private var accountsCached: [Account]?
    var accounts: [Account] {
        get {
            if let cached = accountsCached {
                return cached
            }
            let data = try? storage.read()
            accountsCached = try? JSONDecoder().decode([Account].self, from: data ?? Data())
            return accountsCached ?? []
        }
        set {
            try? storage.write(newValue.encoded())
            accountsCached = newValue
        }
    }

    // Current Account
    private var currentId = ""
    var current: Account? {
        get {
            get(for: currentId)
        }
        set {
            currentId = newValue?.id ?? ""
            if let account = newValue {
                if account.isEphemeral {
                    if !ephAccounts.contains(where: { $0.id == account.id }) {
                        ephAccounts += [account]
                    }
                } else {
                    upsert(account)
                }
            }
        }
    }

    // Filtered account list of software wallets
    var swAccounts: [Account] { accounts.filter { !$0.isHW } }

    // Filtered account list of software ephemeral wallets
    var ephAccounts: [Account] = [Account]()

    // Filtered account list of hardware wallets
    var hwAccounts: [Account] { accounts.filter { $0.isHW } }

    func get(for id: String) -> Account? {
        ephAccounts.filter({ $0.id == id }).first ??
        accounts.filter({ $0.id == id }).first
    }

    func find(xpubHashId: String) -> [Account]? {
        ephAccounts.filter({ $0.xpubHashId == xpubHashId }) +
        accounts.filter({ $0.xpubHashId == xpubHashId })
    }

    func upsert(_ account: Account) {
        var currentList = accounts
        if let index = currentList.firstIndex(where: { $0.id == account.id }) {
            currentList.replaceSubrange(index...index, with: [account])
        } else {
            currentList.append(account)
        }
        accounts = currentList
    }

    func remove(_ account: Account) {
        accounts.removeAll(where: { $0.id == account.id})
        account.removePinKeychainData()
        account.removeBioKeychainData()
        account.removeLightningCredentials()
    }

    func removeAll() {
        accounts.forEach { remove($0) }
        accounts = []
        try? storage.removeAll()
    }

    func getUniqueAccountName(testnet: Bool, watchonly: Bool? = false) -> String {
        let baseName = "\(testnet ? "Testnet ": "")\(watchonly ?? false ? "Watchonly ": "")Wallet"
        for num in 0...999 {
            let name = num == 0 ? baseName : "\(baseName) #\(num + 1)"
            if (AccountsRepository.shared.swAccounts.filter { $0.name.lowercased().hasPrefix(name.lowercased()) }.count) > 0 {
            } else {
                return name
            }
        }
        return baseName
    }
}
