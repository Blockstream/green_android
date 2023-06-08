import Foundation
import UIKit

import gdk
import greenaddress

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

    func load() async {
        self.multisigCellModels = []
        let bitcoinSession = wm.sessions
            .filter { ["mainnet", "testnet"].contains($0.key) }
            .first?.value
        let liquidSession = wm.sessions
            .filter { ["liquid", "testnet-liquid"].contains($0.key) }
            .first?.value
        if let session = bitcoinSession, session.logged {
            if let model = try? await self.loadWOSession(session) {
                multisigCellModels += [model]
            }
        }
        if let session = liquidSession, session.logged {
            if let model = try? await self.loadWOSession(session) {
                multisigCellModels += [model]
            }
        }
        let cellHeaderPubKeys = WatchOnlySettingsCellModel(
            title: "id_extended_public_keys".localized,
            subtitle: "id_tip_you_can_use_the".localized,
            network: nil)
        singlesigCellModels = [cellHeaderPubKeys]
        let models = self.loadWOExtendedPubKeys()
        singlesigCellModels += models
    }

    func loadWOSession(_ session: SessionManager) async throws -> WatchOnlySettingsCellModel {
        let username = try await session.getWatchOnlyUsername()
        guard let username = username else { throw GaError.GenericError()}
        return WatchOnlySettingsCellModel(
            title: session.gdkNetwork.name,
            subtitle: username.isEmpty ? "id_set_up_watchonly_credentials".localized : String(format: "id_enabled_1s".localized, username),
            network: session.gdkNetwork.network)
    }

    func loadWOExtendedPubKeys() -> [WatchOnlySettingsCellModel] {
        return WalletManager.current!.subaccounts
            .filter { $0.gdkNetwork.electrum && !$0.gdkNetwork.liquid && !$0.hidden }
            .compactMap {
                WatchOnlySettingsCellModel(
                    title: $0.localizedName,
                    subtitle: $0.extendedPubkey ?? "",
                    network: $0.gdkNetwork.network,
                    isExtended: true)
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
