import Foundation
import UIKit

import gdk

class MultisigSettingsViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    // current multisig session
    var session: SessionManager

    // load wallet manager for current logged session
    var settings: Settings! { session.settings }

    // reload all contents
    var reloadTableView: (() -> Void)?

    // show errors
    var error: ((String) -> Void)?

    // settings cell models
    var cellModels = [MultisigSettingsCellModel]() {
        didSet {
            reloadTableView?()
        }
    }

    let bgq = DispatchQueue.global(qos: .background)

    init(session: SessionManager) {
        self.session = session
    }

    func getCellModel(at indexPath: IndexPath) -> MultisigSettingsCellModel? {
        return cellModels[indexPath.row]
    }

    func getCellModels() -> [MultisigSettingsCellModel] {
        return cellModels
    }

    func getItems(username: String) -> [MultisigSettingsItem] {
        let watchOnlyStatus = String(format: NSLocalizedString(username.isEmpty ? "id_disabled" : "id_enabled_1s", comment: ""), username)
        let watchOnly = MultisigSettingsItem(
            title: MSItem.WatchOnly.string,
            subtitle: watchOnlyStatus,
            type: .WatchOnly)
        let twoFactorAuthentication = MultisigSettingsItem(
            title: MSItem.TwoFactorAuthentication.string,
            subtitle: "",
            type: .TwoFactorAuthentication)
        let pgp = MultisigSettingsItem(
            title: MSItem.Pgp.string,
            subtitle: "",
            type: .Pgp)
        return [watchOnly, twoFactorAuthentication, pgp]
    }

    func load() async throws {
        if let username = try await session.getWatchOnlyUsername() {
            let items = getItems(username: username)
            cellModels = items.map { MultisigSettingsCellModel($0) }
            
        }
    }

    func enableRecoveryTransactions(_ enable: Bool) async throws {
        let settings = session.settings!
        settings.notifications = SettingsNotifications(emailIncoming: enable, emailOutgoing: enable)
        try await session.changeSettings(settings: settings)
        try await load()
    }
}
