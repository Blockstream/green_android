import Foundation
import UIKit
import PromiseKit

class WalletManager {

    var account: Account
    var sessions = [String: SessionManager]()

    static var shared = [String: WalletManager]()

    init(account: Account, testnet: Bool) {
        self.account = account
        if testnet {
            addSession(for: .testnetSS)
            addSession(for: .testnetLiquidSS)
            addSession(for: .testnetMS)
            addSession(for: .testnetLiquidMS)
        } else {
            addSession(for: .bitcoinSS)
            addSession(for: .liquidSS)
            addSession(for: .bitcoinMS)
            addSession(for: .liquidMS)
        }
        WalletManager.shared[account.id] = self
    }

    func addSession(for network: NetworkSecurityCase) {
        let networkName = network.network
        let gdknetwork = getGdkNetwork(networkName)
        sessions[networkName] = SessionManager(gdknetwork)
    }

    var activeSessions: [String: SessionManager] {
        self.sessions.filter { $0.1.logged }
    }

    func login(pin: String, pinData: PinData, bip39passphrase: String?) -> Promise<Void> {
        guard let mainSession = sessions[account.networkName] else {
            fatalError()
        }
        return Guarantee()
            .then { mainSession.connect() }
            .then { _ in mainSession.decryptWithPin(pin: pin, pinData: pinData) }
            .compactMap { Credentials(mnemonic: $0.mnemonic, password: nil, bip39Passphrase: bip39passphrase) }
            .then { credentials in
                when(guarantees: self.sessions.values
                    .filter { !$0.logged }
                    .map { $0.loginWithCredentials(credentials)
                        .asVoid()
                        .recover { _ in Promise().asVoid() }
                    }
                )
            }.map {
                self.account.isEphemeral = ![nil, ""].contains(bip39passphrase)
                //self.registry?.cache(session: self)
                //self.registry?.loadAsync(session: self)
            }
    }

    func loginWatchOnly(username: String, password: String) -> Guarantee<Void> {
        return when(guarantees: self.sessions.values
                .filter { !$0.logged }
                .map { $0.loginWatchOnly(username, password)
                    .asVoid()
                    .recover { _ in Promise().asVoid() }
                }
        )
    }

    func subaccounts(_ refresh: Bool = false) -> Promise<[WalletItem]> {
        let promises: [Promise<[WalletItem]>] = self.activeSessions.values
            .compactMap { session in
                session
                    .subaccounts(refresh)
                    .get { $0.forEach { $0.network = session.gdkNetwork.network }}
            }
        return when(resolved: promises).compactMap { (subaccounts: [Result<[WalletItem]>]) -> [WalletItem] in
            let txt: [[WalletItem]] = subaccounts.compactMap { res in
                switch res {
                case .fulfilled(let sub):
                    return sub
                case .rejected(_):
                    return nil
                }
            }
            return Array(txt.joined())
        }
    }

    func balances(subaccounts: [WalletItem]) -> Promise<[String: UInt64]> {
        let promises = subaccounts
            .map { sub in
                sessions[sub.network ?? ""]!
                    .getBalance(subaccount: sub.pointer, numConfs: 0)
                    .compactMap { sub.satoshi = $0 }
                    .asVoid()
            }
        return when(fulfilled: promises)
            .compactMap { _ in
                var balance = [String: UInt64]()
                subaccounts.forEach { subaccount in
                    let satoshi = subaccount.satoshi ?? [:]
                    satoshi.forEach {
                        if let amount = balance[$0.0] {
                            balance[$0.0] = amount + $0.1
                        } else {
                            balance[$0.0] = $0.1
                        }
                    }
                }
                return balance
            }
    }

    func subaccountsFilteredByAsset(subaccounts: [WalletItem], asset: String) -> [WalletItem] {
        return subaccounts.filter { $0.satoshi?.keys.contains(asset) ?? false }
    }
}
