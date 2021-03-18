import Foundation

class AccountsManager {
    let attrAccount = "AccountsManager_Account"
    let attrService = "AccountsManager_Service"
    static let shared = AccountsManager()

    var list: [Account] {
        get {
            if !UserDefaults.standard.bool(forKey: "wallets_keychain") {
                // Handle wallet migration
                let accounts = migrationAccounts()
                try? write(accounts)
                UserDefaults.standard.setValue(true, forKey: "wallets_keychain")
                return accounts
            }
            return (try? read()) ?? []
        }
        set {
            try? write(newValue)
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

    private func migrationAccounts() -> [Account] {
        var accounts = [Account]()
        for network in ["mainnet", "testnet", "liquid"] {
            let bioData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
            let pinData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
            if pinData || bioData {
                var account = Account(name: nameLabel(network), network: network)
                account.attempts = UserDefaults.standard.integer(forKey: network + "_pin_attempts")
                if bioData {
                    try? AuthenticationTypeHandler.generateBiometricPrivateKey(network: account.id)
                    let data = try? AuthenticationTypeHandler.getAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
                    try? AuthenticationTypeHandler.addBiometryType(data: data!, extraData: String.random(length: 14), forNetwork: account.id)
                }
                if pinData {
                    let data = try? AuthenticationTypeHandler.getAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
                    _ = try? AuthenticationTypeHandler.addPIN(data: data!, forNetwork: account.id)
                }
                accounts.append(account)
            }
        }
        return accounts
    }

    private func nameLabel(_ network: String) -> String {
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
}
