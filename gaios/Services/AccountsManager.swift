import Foundation

class AccountsManager {
    let attrAccount = "AccountsManager_Account"
    let attrService = "AccountsManager_Service"
    static let shared = AccountsManager()

    var current: Account?
    var list: [Account] {
        get {
            return (try? read()) ?? []
        }
        set {
            try? write(newValue)
        }
    }

    func onFirstInitialization() {
        for network in ["mainnet", "testnet", "liquid"] {
            if !UserDefaults.standard.bool(forKey: network + "FirstInitialization") {
                _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
                _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
                UserDefaults.standard.set(true, forKey: network + "FirstInitialization")
            }
        }
        if !UserDefaults.standard.bool(forKey: "FirstInitialization") {
            try? remove()
            UserDefaults.standard.set(true, forKey: "FirstInitialization")

            // Handle wallet migration
            list = migratedAccounts()
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
            throw GaError.GenericError
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
            throw GaError.GenericError
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
            throw GaError.GenericError
        }
        guard let data = retrivedData as? Data else { throw GaError.GenericError }
        return try JSONDecoder().decode([Account].self, from: data)
    }

    private func migratedAccounts() -> [Account] {
        var accounts = [Account]()
        for network in ["mainnet", "testnet", "liquid"] {
            let bioData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
            let pinData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
            if pinData || bioData {
                var account = Account(name: nameLabel(network), network: network, keychain: network)
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

    func add(_ account: Account) {
        var currentList = list
        currentList.append(account)
        list = currentList
    }

    func remove(_ account: Account) {
        var currentList = list
        if let index = currentList.firstIndex(where: { $0 == account }) {
            currentList.remove(at: index)
        }
        list = currentList
    }

    func update(_ account: Account) {
        var currentList = list
        if let index = currentList.firstIndex(where: { $0.id == account.id }) {
            currentList.replaceSubrange(index...index, with: [account])
        }
        list = currentList
    }
}
