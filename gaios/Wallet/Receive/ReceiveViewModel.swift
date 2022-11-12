import Foundation
import UIKit
import PromiseKit

class ReceiveViewModel {

    var accounts: [WalletItem]
    var asset: String
    var account: WalletItem
    var address: Address?
    var reload: (() -> Void)?
    var error: ((String) -> Void)?
    var wm: WalletManager { WalletManager.current! }

    init(account: WalletItem, accounts: [WalletItem]) {
        self.account = account
        self.accounts = accounts
        self.asset = account.gdkNetwork.getFeeAsset()
    }

    func assetIcon() -> UIImage {
        return WalletManager.current?.registry.image(for: asset) ?? UIImage()
    }
    func assetName() -> String {
        return WalletManager.current?.registry.info(for: asset).name ?? "--"
    }
    func accountType() -> String {
        return account.localizedName()
    }

    func newAddress() {
        let session = wm.sessions[account.gdkNetwork.network]
        session?.getReceiveAddress(subaccount: account.pointer)
            .done { [weak self] addr in
                self?.address = addr
                self?.reload?()
            }.catch { [weak self] _ in
                self?.error?("id_connection_failed")
            }
    }

    func isBipAddress(_ addr: String) -> Bool {
        let session = wm.sessions[account.gdkNetwork.network]
        return session?.validBip21Uri(uri: addr) ?? false
    }

    func validateHw() -> Promise<Bool> {
        let hw: HWProtocol = AccountsManager.shared.current?.isLedger ?? false ? Ledger.shared : Jade.shared
        let chain = account.gdkNetwork.chain
        guard let addr = address else {
            return Promise() { $0.reject(GaError.GenericError()) }
        }
        return Address.validate(with: self.account, hw: hw, addr: addr, network: chain)
            .compactMap { return self.address?.address == $0 }
    }

    func addressToUri(address: String, satoshi: Int64) -> String {
        var ntwPrefix = "bitcoin"
        if account.gdkNetwork.liquid {
            ntwPrefix = account.gdkNetwork.mainnet ? "liquidnetwork" :  "liquidtestnet"
        }
        if satoshi == 0 {
            return address
        }
        return String(format: "%@:%@?amount=%.8f", ntwPrefix, address, toBTC(satoshi))
    }

    func toBTC(_ satoshi: Int64) -> Double {
        return Double(satoshi) / 100000000
    }
}
