import Foundation
import PromiseKit
import UIKit
import gdk
import greenaddress

struct WOCellModel {
    let img: UIImage
    let title: String
    let hint: String
}

class WOViewModel {

    let bgq = DispatchQueue.global(qos: .background)

    let types: [WOCellModel] = [
        WOCellModel(img: UIImage(named: "ic_key_ss")!,
                    title: "id_singlesig".localized,
                    hint: "id_enter_your_xpub_to_add_a".localized),
        WOCellModel(img: UIImage(named: "ic_key_ms")!,
                    title: "id_multisig_shield".localized,
                    hint: "id_log_in_to_your_multisig_shield".localized)
    ]

    func newAccountMultisig(for network: GdkNetwork, username: String, password: String, remember: Bool ) -> Account {
        let name = AccountsRepository.shared.getUniqueAccountName(
            testnet: !network.mainnet,
            watchonly: true)
        return Account(name: name, network: network.chain, username: username, password: remember ? password : nil, isSingleSig: network.electrum)
    }

    func newAccountSinglesig(for network: GdkNetwork) -> Account {
        let name = AccountsRepository.shared.getUniqueAccountName(
            testnet: !network.mainnet,
            watchonly: true)
        return Account(name: name, network: network.chain, username: "", isSingleSig: network.electrum)
    }

    func loginMultisig(for account: Account, password: String?) -> Promise<Void> {
        guard let username = account.username,
              let password = !password.isNilOrEmpty ? password : account.password else {
            return Promise(error: GaError.GenericError("Invalid credentials"))
        }
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        return Guarantee()
            .compactMap { Credentials.watchonlyMultisig(username: username, password: password) }
            .then(on: bgq) { wm.loginWatchonly(credentials: $0) }
            .map { _ in AnalyticsManager.shared.loginWallet(loginType: .watchOnly, ephemeralBip39: false, account: account) }
    }

    func setupSinglesig(for account: Account, enableBio: Bool, credentials: Credentials) -> Promise<Void> {
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        let session = wm.prominentSession!
        let password = enableBio ? String.random(length: 14) : ""
        return Guarantee()
            .then(on: bgq) { session.connect() }
            .compactMap { EncryptWithPinParams(pin: password, credentials: credentials) }
            .then(on: bgq) { session.encryptWithPin($0) }
            .compactMap(on: bgq) { encrypted in
                if enableBio {
                    try AuthenticationTypeHandler.addBiometry(pinData: encrypted.pinData, extraData: password, forNetwork: account.keychain)
                } else {
                    try AuthenticationTypeHandler.addPIN(pinData: encrypted.pinData, forNetwork: account.keychain)
                }
            }
    }

    func loginSinglesig(for account: Account) -> Promise<Void> {
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        let session = wm.prominentSession!
        let bioEnabled = AuthenticationTypeHandler.findAuth(method: .AuthKeyBiometric, forNetwork: account.keychain)
        return Guarantee()
            .then(on: bgq) { session.connect() }
            .compactMap { try account.auth(bioEnabled ? .AuthKeyBiometric : .AuthKeyPIN) }
            .compactMap { DecryptWithPinParams(pin: $0.plaintextBiometric ?? "", pinData: $0) }
            .then(on: bgq) { session.decryptWithPin($0) }
            .then(on: bgq) { wm.loginWatchonly(credentials: $0) }
            .map { _ in AnalyticsManager.shared.loginWallet(loginType: .watchOnly, ephemeralBip39: false, account: account) }
            .asVoid()
    }
}
