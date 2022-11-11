import Foundation
import UIKit
import PromiseKit

class UserSettingsViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    // load wallet manager for current logged session
    var settings: Settings? { wm.prominentSession?.settings }

    // reload all contents
    var reloadTableView: (() -> Void)?

    // settings cell models
    var sections = [USSection]()
    var items = [USSection: [UserSettingsItem]]()
    var cellModels = [USSection: [UserSettingsCellModel]]() {
        didSet {
            reloadTableView?()
        }
    }

    func getCellModel(at indexPath: IndexPath) -> UserSettingsCellModel? {
        let section = sections[indexPath.section]
        return cellModels[section]?[indexPath.row]
    }

    func getCellModelsForSection(at indexSection: Int) -> [UserSettingsCellModel]? {
        let section = sections[indexSection]
        return cellModels[section]
    }

    func getAbout() -> [UserSettingsItem] {
        let versionSubtitle = String(format: NSLocalizedString("id_version_1s", comment: ""), Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? "")
        let version = UserSettingsItem(
            title: USItem.Version.string,
            subtitle: versionSubtitle,
            section: .About,
            type: .Version)
        let support = UserSettingsItem(
            title: USItem.SupportID.string,
            subtitle: "Copy support ID",
            section: .About,
            type: .SupportID)
        return [version, support]
    }

    func getSecurity() -> [UserSettingsItem] {
        let changePin = UserSettingsItem(
            title: USItem.ChangePin.string,
            subtitle: "",
            section: .Security,
            type: .ChangePin)
        let bioTitle = AuthenticationTypeHandler.supportsBiometricAuthentication() ? NSLocalizedString(AuthenticationTypeHandler.biometryType == .faceID ? "id_face_id" : "id_touch_id", comment: "") : NSLocalizedString("id_touchface_id_not_available", comment: "")
        let loginWithBiometrics = UserSettingsItem(
            title: bioTitle,
            subtitle: "",
            section: .Security,
            type: .LoginWithBiometrics)
        let autolock = UserSettingsItem(
            title: USItem.AutoLogout.string,
            subtitle: (settings?.autolock ?? .fiveMinutes).string,
            section: .Security,
            type: .AutoLogout)
        return [changePin, loginWithBiometrics, autolock]
    }

    func getGeneral() -> [UserSettingsItem] {
        guard let settings = settings else { return [] }
        let bitcoinDenomination = UserSettingsItem(
            title: USItem.BitcoinDenomination.string,
            subtitle: settings.denomination.string,
            section: .General,
            type: .BitcoinDenomination)
        let referenceExchangeRate = UserSettingsItem(
            title: USItem.ReferenceExchangeRate.string,
            subtitle: "\(settings.pricing["currency"]!)/\(settings.pricing["exchange"]!.capitalized)",
            section: .General,
            type: .ReferenceExchangeRate)
        let archievedAccounts = UserSettingsItem(
            title: USItem.ArchievedAccounts.string,
            subtitle: "",
            section: .General,
            type: .ArchievedAccounts)
        return [bitcoinDenomination, referenceExchangeRate, archievedAccounts]
    }

    func getRecovery() -> [UserSettingsItem] {
        let recovery = UserSettingsItem(
            title: String(format: USItem.BackUpRecoveryPhrase.string, getNetwork()).localizedCapitalized,
            subtitle: "",
            section: .Recovery,
            type: .BackUpRecoveryPhrase)
        return [recovery]
    }

    func getMultisig() -> [UserSettingsItem] {
        let bitcoin = UserSettingsItem(
            title: String(format: USItem.Bitcoin.string, getNetwork()).localizedCapitalized,
            subtitle: "",
            section: .Multisig,
            type: .Bitcoin)
        let liquid = UserSettingsItem(
            title: String(format: USItem.Liquid.string, getNetwork()).localizedCapitalized,
            subtitle: "",
            section: .Multisig,
            type: .Liquid)
        return [bitcoin, liquid]
    }

    func getLogout() -> [UserSettingsItem] {
        let wallet = AccountsManager.shared.current
        let logout = UserSettingsItem(
            title: (wallet?.name ?? "").localizedCapitalized,
            subtitle: NSLocalizedString("id_log_out", comment: ""),
            section: .Logout,
            type: .Logout)
        return [logout]
    }

    func load() {
        sections = [ .Logout, .General, .Security, .Recovery, .Multisig, .About ]
        items = [
            .Logout: getLogout(),
            .General: getGeneral(),
            .Security: getSecurity(),
            .Recovery: getRecovery(),
            .Multisig: getMultisig(),
            .About: getAbout()]
        cellModels = items.mapValues { $0.map { UserSettingsCellModel($0) } }
    }
}
