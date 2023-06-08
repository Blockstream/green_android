import Foundation

import UIKit
import gdk
import greenaddress

struct WOCellModel {
    let img: UIImage
    let title: String
    let hint: String
}

class WOViewModel {

    let types: [WOCellModel] = [
        WOCellModel(img: UIImage(named: "ic_key_ss")!,
                    title: "id_singlesig".localized,
                    hint: "id_enter_your_xpub_to_add_a".localized),
        WOCellModel(img: UIImage(named: "ic_key_ms")!,
                    title: "id_multisig_shield".localized,
                    hint: "id_log_in_to_your_multisig_shield".localized)
    ]

    func newAccountMultisig(for gdkNetwork: GdkNetwork, username: String, password: String, remember: Bool ) -> Account {
        let name = AccountsRepository.shared.getUniqueAccountName(
            testnet: !gdkNetwork.mainnet,
            watchonly: true)
        let network = NetworkSecurityCase(rawValue: gdkNetwork.network) ?? .bitcoinSS
        return Account(name: name, network: network, username: username, password: remember ? password : nil)
    }

    func newAccountSinglesig(for gdkNetwork: GdkNetwork) -> Account {
        let name = AccountsRepository.shared.getUniqueAccountName(
            testnet: !gdkNetwork.mainnet,
            watchonly: true)
        let network = NetworkSecurityCase(rawValue: gdkNetwork.network) ?? .bitcoinSS
        return Account(name: name, network: network, username: "")
    }

    func loginMultisig(for account: Account, password: String?) async throws {
        guard let username = account.username,
              let password = !password.isNilOrEmpty ? password : account.password else {
            throw GaError.GenericError("Invalid credentials")
        }
        AnalyticsManager.shared.loginWalletStart()
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        let credentials = Credentials.watchonlyMultisig(username: username, password: password)
        try await wm.loginWatchonly(credentials: credentials)
        AnalyticsManager.shared.loginWalletEnd(account: account, loginType: .watchOnly)
    }

    func setupSinglesig(for account: Account, enableBio: Bool, credentials: Credentials) async throws {
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        let session = wm.prominentSession!
        let password = enableBio ? String.random(length: 14) : ""
        try await session.connect()
        let encrypt = EncryptWithPinParams(pin: password, credentials: credentials)
        let encrypted = try await session.encryptWithPin(encrypt)
        if enableBio {
            try AuthenticationTypeHandler.addBiometry(pinData: encrypted.pinData, extraData: password, forNetwork: account.keychain)
        } else {
            try AuthenticationTypeHandler.addPIN(pinData: encrypted.pinData, forNetwork: account.keychain)
        }
    }

    func loginSinglesig(for account: Account) async throws {
        let wm = WalletsRepository.shared.getOrAdd(for: account)
        let session = wm.prominentSession!
        let bioEnabled = AuthenticationTypeHandler.findAuth(method: .AuthKeyBiometric, forNetwork: account.keychain)
        AnalyticsManager.shared.loginWalletStart()
        try await session.connect()
        let data = try account.auth(bioEnabled ? .AuthKeyBiometric : .AuthKeyPIN)
        let decrypt = DecryptWithPinParams(pin: data.plaintextBiometric ?? "", pinData: data)
        let credentials = try await session.decryptWithPin(decrypt)
        try await wm.loginWatchonly(credentials: credentials)
        AnalyticsManager.shared.loginWalletEnd(account: account, loginType: .watchOnly)
    }
}
