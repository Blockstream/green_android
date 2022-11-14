import Foundation
import UIKit
import PromiseKit

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
        var locktimeRecoveryEnable = false
        if let notifications = self.settings?.notifications {
            locktimeRecoveryEnable = notifications.emailOutgoing == true
        }
        let locktimeRecovery = MultisigSettingsItem(
            title: MSItem.RecoveryTransactions.string,
            subtitle: locktimeRecoveryEnable ? NSLocalizedString("id_enabled", comment: "") : NSLocalizedString("id_disabled", comment: ""),
            type: .RecoveryTransactions)
        let pgp = MultisigSettingsItem(
            title: MSItem.Pgp.string,
            subtitle: "",
            type: .Pgp)
        return [watchOnly, twoFactorAuthentication, locktimeRecovery, pgp]
    }

    func load() {
        Guarantee().then { self.session.getWatchOnlyUsername() }
            .map { self.getItems(username: $0) }
            .done { self.cellModels = $0.map { MultisigSettingsCellModel($0) } }
            .catch { err in self.error?(err.localizedDescription) }
    }

    func enableRecoveryTransactions(_ enable: Bool) {
        var settings = session.settings!
        settings.notifications = SettingsNotifications(emailIncoming: enable, emailOutgoing: enable)
        Guarantee()
            .then(on: bgq) { self.session.changeSettings(settings: settings) }
            .done { _ in self.load() }
            .catch { err in self.error?(err.localizedDescription) }
    }
}
