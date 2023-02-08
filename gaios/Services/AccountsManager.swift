import Foundation

class AccountsManager {
    let attrAccount = "AccountsManager_Account"
    let attrService = "AccountsManager_Service"
    static let shared = AccountsManager()

    // List of saved accounts with cache
    private var accountsCached: [Account]?
    var accounts: [Account] {
        get {
            if let cached = accountsCached {
                return cached
            }
            accountsCached = try? read()
            return accountsCached ?? []
        }
        set {
            try? write(newValue)
            accountsCached = newValue
        }
    }

    // Filtered account list of software wallets
    var swAccounts: [Account] { accounts.filter { !$0.isHW } }

    // Filtered account list of software ephemeral wallets
    var ephAccounts: [Account] = [Account]()

    // Filtered account list of hardware wallets
    var hwAccounts: [Account] { accounts.filter { account in
        account.isHW && !WalletManager.wallets.filter {$0.key == account.id }.isEmpty } }

    // Hardware wallets accounts are store in temporary memory
    var devices = [ Account(name: "Blockstream Jade", network: "mainnet", isJade: true, isSingleSig: false),
                       Account(name: "Ledger Nano X", network: "mainnet", isLedger: true, isSingleSig: false) ]

    // Current Account
    private var currentId = ""
    var current: Account? {
        get {
            ephAccounts.filter({ $0.id == currentId }).first ??
            accounts.filter({ $0.id == currentId }).first
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

    func onFirstInitialization() {
        for network in ["mainnet", "testnet", "liquid"] {
            if !UserDefaults.standard.bool(forKey: network + "FirstInitialization") {
                _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyBiometric, forNetwork: network)
                _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyPIN, forNetwork: network)
                UserDefaults.standard.set(true, forKey: network + "FirstInitialization")
            }
        }
        if !UserDefaults.standard.bool(forKey: "FirstInitialization") {
            try? remove()
            accounts = []
            UserDefaults.standard.set(true, forKey: "FirstInitialization")

            // Handle wallet migration
            accounts = migratedAccounts()
        }
    }

    private func remove() throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                    kSecAttrAccount as String: attrAccount,
                                    kSecAttrService as String: attrService,
                                    kSecMatchLimit as String: kSecMatchLimitOne,
                                    kSecReturnData as String: kCFBooleanTrue ?? true]
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess else {
            throw GaError.GenericError()
        }
    }

    private func write(_ accounts: [Account]) throws {
        let data = try JSONEncoder().encode(accounts)
        let list = try JSONSerialization.jsonObject(with: data, options: []) as? [[String: Any]]
        let serializedAccounts = try? JSONSerialization.data(withJSONObject: list!, options: [])
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                    kSecAttrAccount as String: attrAccount,
                                    kSecAttrService as String: attrService,
                                    kSecValueData as String: serializedAccounts!]
        var status = SecItemAdd(query as CFDictionary, nil)
        if status == errSecDuplicateItem {
            status = SecItemDelete(query as CFDictionary)
            status = SecItemAdd(query as CFDictionary, nil)
        }
        guard status == errSecSuccess else {
            if #available(iOS 11.3, *) {
                let text = SecCopyErrorMessageString(status, nil) ?? "" as CFString
                print("Operation failed: \(status) \(text))")
            }
            throw GaError.GenericError()
        }
    }

    private func read() throws -> [Account] {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                    kSecAttrAccount as String: attrAccount,
                                    kSecAttrService as String: attrService,
                                    kSecMatchLimit as String: kSecMatchLimitOne,
                                    kSecReturnData as String: kCFBooleanTrue ?? true]
        var retrivedData: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &retrivedData)
        guard status == errSecSuccess else {
            if #available(iOS 11.3, *) {
                let text = SecCopyErrorMessageString(status, nil) ?? "" as CFString
                print("Operation failed: \(status) \(text))")
            }
            throw GaError.GenericError()
        }
        guard let data = retrivedData as? Data else { throw GaError.GenericError() }
        return try JSONDecoder().decode([Account].self, from: data)
    }

    private func migratedAccounts() -> [Account] {
        var accounts = [Account]()
        for network in ["mainnet", "testnet", "liquid"] {
            let bioData = AuthenticationTypeHandler.findAuth(method: .AuthKeyBiometric, forNetwork: network)
            let pinData = AuthenticationTypeHandler.findAuth(method: .AuthKeyPIN, forNetwork: network)
            if pinData || bioData {
                var account = Account(name: nameLabel(network), network: network, keychain: network, isSingleSig: false)
                account.attempts = UserDefaults.standard.integer(forKey: network + "_pin_attempts")
                accounts.append(account)
            }
        }
        return accounts
    }

    func nameLabel(_ network: String) -> String {
        switch network {
        case "mainnet":
            return "Bitcoin"
        case "testnet":
            return "Testnet"
        case "liquid":
            return "Liquid"
        default:
            return "Account"
        }
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
        var currentList = accounts
        if let index = currentList.firstIndex(where: { $0 == account }) {
            currentList.remove(at: index)
        }
        accounts = currentList
    }

    func getUniqueAccountName(testnet: Bool, watchonly: Bool? = false) -> String {
        let baseName = "\(testnet ? "Testnet ": "")\(watchonly ?? false ? "Watchonly ": "")Wallet"
        for num in 0...999 {
            let name = num == 0 ? baseName : "\(baseName) #\(num + 1)"
            if (AccountsManager.shared.swAccounts.filter { $0.name.lowercased().hasPrefix(name.lowercased()) }.count) > 0 {
            } else {
                return name
            }
        }
        return baseName
    }

    func getUniqueSubAccountName(subaccounts: [WalletItem], name: String) -> String {
        let baseName = name
        for num in 0...999 {
            let name = num == 0 ? baseName : "\(baseName) \(num + 1)"
            if (subaccounts.filter { $0.localizedName().lowercased().hasPrefix(name.lowercased()) }.count) > 0 {
            } else {
                return name
            }
        }
        return baseName
    }
}
