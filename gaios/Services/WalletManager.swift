import Foundation
import UIKit
import PromiseKit

class WalletManager {

    // Current account
    var account: Account

    // Hashmap of available networks with open session
    var sessions = [String: SessionManager]()

    // Cached subaccounts list
    var subaccounts = [WalletItem]()

    // Store active subaccount
    private var activeWalletHash: Int?
    var currentSubaccount: WalletItem? {
        get {
            if activeWalletHash == nil {
                return subaccounts.first { $0.hidden == false }
            }
            return subaccounts.first { $0.hashValue == activeWalletHash}
        }
        set {
            if let newValue = newValue {
                activeWalletHash = newValue.hashValue
                if let index = subaccounts.firstIndex(where: { $0.pointer == newValue.pointer && $0.network == newValue.network}) {
                    subaccounts[index] = newValue
                }
            }
        }
    }

    // Get active session of the active subaccount
    var currentSession: SessionManager? {
        let network = currentSubaccount?.network
        return sessions[network ?? ""]
    }

    // Static store all the Wallet available in the app for each account
    static var shared = [String: WalletManager]()

    // Serial reconnect queue for network events
    static let reconnectionQueue = DispatchQueue(label: "reconnection_queue")

    // Static store the current WalletManager used in the active user session
    static var current: WalletManager? {
        let account = AccountsManager.shared.current
        return WalletManager.shared[account?.id ?? ""]
    }

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
                    .map { session in
                        session.loginWithCredentials(credentials)
                        .asVoid()
                        .recover { _ in Promise().asVoid() }
                        .map { session.registry?.cache(session: session) }
                        .map { session.registry?.loadAsync(session: session) }
                    }
                )
            }.map {
                self.account.isEphemeral = ![nil, ""].contains(bip39passphrase)
            }
    }

    func loginWatchOnly(username: String, password: String) -> Guarantee<Void> {
        return when(guarantees: self.sessions.values
                .filter { !$0.logged }
                .map { session in
                    session.loginWatchOnly(username, password)
                    .asVoid()
                    .recover { _ in Promise().asVoid() }
                    .map { session.registry?.cache(session: session) }
                    .map { session.registry?.loadAsync(session: session) }
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
            self.subaccounts = Array(txt.joined())
            return self.subaccounts
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

    func pause() {
        activeSessions.forEach { (_, session) in
            if session.connected {
                WalletManager.reconnectionQueue.async {
                    try? session.session?.reconnectHint(hint: ["tor_hint": "disconnect", "hint": "disconnect"])
                }
            }
        }
    }

    func resume() {
        activeSessions.forEach { (_, session) in
            if session.connected {
                WalletManager.reconnectionQueue.async {
                    try? session.session?.reconnectHint(hint: ["tor_hint": "connect", "hint": "connect"])
                }
            }
        }
    }
}
