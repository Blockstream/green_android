import Foundation
import gdk

enum MigrationFlag: String {
    case appDataVersion = "app_data_version"
    case firstInitialization = "FirstInitialization"
}
class MigratorManager {

    static let shared = MigratorManager()

    func migrate() {
        let appDataVersion = UserDefaults.standard.integer(forKey: MigrationFlag.appDataVersion.rawValue)
        let firstInitialization = !UserDefaults.standard.bool(forKey: MigrationFlag.firstInitialization.rawValue)
        if firstInitialization {
            // first installation or app upgrade from app version < v3.5.5
            clean()
            migrateWallets()
            UserDefaults.standard.set(true, forKey: MigrationFlag.firstInitialization.rawValue)
        }
        if appDataVersion < 1 {
            // upgrade from app < v4.0.0
            migrateDatadir()
        }
        UserDefaults.standard.set(1, forKey: MigrationFlag.appDataVersion.rawValue)
    }

    private func migrateDatadir() { // from "4.0.0"
        // move cache dir to the app support
        let params = GdkInit.defaults()
        let url = try? FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true).appendingPathComponent(Bundle.main.bundleIdentifier!, isDirectory: true)
        if let atPath = url?.path, let toPath = params.datadir,
            FileManager.default.fileExists(atPath: atPath) {
            let files = try? FileManager.default.contentsOfDirectory(atPath: atPath)
            files?.forEach { file in
                try? FileManager.default.moveItem(atPath: "\(atPath)/\(file)", toPath: "\(toPath)/\(file)")
            }
        }
    }

    private func migrateWallets() { // from "3.5.5"
        var accounts = [Account]()
        for network in ["mainnet", "testnet", "liquid"] {
            let bioData = AuthenticationTypeHandler.findAuth(method: .AuthKeyBiometric, forNetwork: network)
            let pinData = AuthenticationTypeHandler.findAuth(method: .AuthKeyPIN, forNetwork: network)
            if pinData || bioData {
                var account = Account(name: network.firstCapitalized, network: network, keychain: network, isSingleSig: false)
                account.attempts = UserDefaults.standard.integer(forKey: network + "_pin_attempts")
                accounts.append(account)
            }
        }
        AccountsRepository.shared.accounts = accounts
    }

    private func clean() {
        for network in ["mainnet", "testnet", "liquid"] {
            if !UserDefaults.standard.bool(forKey: network + "FirstInitialization") {
                _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyBiometric, forNetwork: network)
                _ = AuthenticationTypeHandler.removeAuth(method: .AuthKeyPIN, forNetwork: network)
                UserDefaults.standard.set(true, forKey: network + "FirstInitialization")
            }
        }
        AccountsRepository.shared.removeAll()
    }
}
