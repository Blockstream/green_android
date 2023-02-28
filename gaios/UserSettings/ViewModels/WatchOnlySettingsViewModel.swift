import Foundation
import UIKit
import PromiseKit

class WatchOnlySettingsViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }
    let bgq = DispatchQueue.global(qos: .background)

    // reload all contents
    var reloadTableView: (() -> Void)?

    // show errors
    var error: ((String) -> Void)?

    // settings cell models
    var sections = WOSection.allCases
    var items = [WOSection: [UserSettingsItem]]()
    var multisigCellModels = [WatchOnlySettingsCellModel]() {
        didSet {
            reloadTableView?()
        }
    }
    var singlesigCellModels = [WatchOnlySettingsCellModel]() {
        didSet {
            reloadTableView?()
        }
    }

    func getCellModel(at indexPath: IndexPath) -> WatchOnlySettingsCellModel? {
        let section = sections[indexPath.section]
        return (section == .Multisig ? multisigCellModels : singlesigCellModels)[indexPath.row]
    }

    func getCellModelsForSection(at indexSection: Int) -> [WatchOnlySettingsCellModel]? {
        let section = sections[indexSection]
        return section == .Multisig ? multisigCellModels : singlesigCellModels
    }

    func load() {
        self.multisigCellModels = []
        let bitcoinSession = wm.sessions
            .filter { ["mainnet", "testnet"].contains($0.key) }
            .first?.value
        let liquidSession = wm.sessions
            .filter { ["liquid", "testnet-liquid"].contains($0.key) }
            .first?.value
        if let session = bitcoinSession, session.logged {
            self.loadWOSession(session)
                .done { self.multisigCellModels += [$0] }
                .catch { err in self.error?(err.localizedDescription) }
        }
        if let session = liquidSession, session.logged {
            self.loadWOSession(session)
                .done { self.multisigCellModels += [$0] }
                .catch { err in self.error?(err.localizedDescription) }
        }
        let cellHeaderPubKeys = WatchOnlySettingsCellModel(
            title: "Extended Public Keys",
            subtitle: "Tip: You can use the xPub/yPub/zPub to view your watch-only wallet",
            network: nil)
        self.singlesigCellModels = [cellHeaderPubKeys]
        Promise()
            .then { self.loadWOExtendedPubKeys() }
            .done { self.singlesigCellModels += $0 }
            .catch { err in self.error?(err.localizedDescription) }
    }

    func loadWOSession(_ session: SessionManager) -> Promise<WatchOnlySettingsCellModel> {
        return Guarantee().compactMap { session }
            .then { $0.getWatchOnlyUsername() }
            .map { WatchOnlySettingsCellModel(
                title: session.gdkNetwork.name,
                subtitle: $0.isEmpty ? "id_set_up_watchonly_credentials".localized : String(format: "id_enabled_1s".localized, $0),
                network: session.gdkNetwork.network) }
    }

    func loadWOExtendedPubKeys() -> Promise<[WatchOnlySettingsCellModel]> {
        let promises = WalletManager.current!.subaccounts
            .filter { $0.gdkNetwork.electrum && !$0.gdkNetwork.liquid }
            .compactMap { $0.session?.subaccount($0.pointer) }
        return when(fulfilled: promises).compactMap { subaccounts in
            return subaccounts.map {
                WatchOnlySettingsCellModel(
                    title: $0.localizedName,
                    subtitle: $0.extendedPubkey ?? "",
                    network: $0.gdkNetwork.network,
                    isExtended: true)
            }
        }
    }

    func loadWOOutputDescriptors() -> [WatchOnlySettingsCellModel] {
        return WalletManager.current!.subaccounts
            .filter { $0.coreDescriptors != nil }
            .compactMap {
                WatchOnlySettingsCellModel(
                    title: $0.localizedName,
                    subtitle: $0.coreDescriptors?.joined(separator: "\n") ?? "",
                    network: $0.gdkNetwork.network)
            }
    }
}
