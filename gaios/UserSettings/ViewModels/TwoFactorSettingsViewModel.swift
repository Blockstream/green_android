import Foundation
import UIKit
import PromiseKit

class TwoFactorSettingsViewModel {

    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }

    // current multisig session
    var sessionBitcoin: SessionManager? { wm.sessions["mainnet"] }
    var sessionLiquid: SessionManager? { wm.sessions["liquid"] }
    var networks: [String] { wm.testnet ? ["testnet", "testnet-liquid"] : ["mainnet", "liquid"] }
    var sessions: [SessionManager] { networks.compactMap { wm.sessions[$0] }}

    private let bgq = DispatchQueue.global(qos: .background)
    private var csvTypes = [Settings.CsvTime]()
    private var csvValues = [Int]()
    private var newCsv: Int?
    private var currentCsv: Int?
    var twoFactorConfig: TwoFactorConfig?

    func getTwoFactors(session: SessionManager) -> Promise<[TwoFactorItem]> {
        if !session.logged {
            return Promise.value([])
        }
        return session.loadTwoFactorConfig()
            .map { twoFactorConfig in
                self.twoFactorConfig = twoFactorConfig
                return [ TwoFactorItem(name: NSLocalizedString("id_email", comment: ""), enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, maskedData: twoFactorConfig.email.data, type: .email),
                        TwoFactorItem(name: NSLocalizedString("id_sms", comment: ""), enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, maskedData: twoFactorConfig.sms.data, type: .sms),
                        TwoFactorItem(name: NSLocalizedString("id_call", comment: ""), enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, maskedData: twoFactorConfig.phone.data, type: .phone),
                        TwoFactorItem(name: NSLocalizedString("id_authenticator_app", comment: ""), enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: .gauth) ]
            }
    }

    func disable(session: SessionManager, type: TwoFactorType) -> Promise<Void> {
        return Guarantee()
            .compactMap { TwoFactorConfigItem(enabled: false, confirmed: false, data: "") }
            .then(on: bgq) { session.changeSettingsTwoFactor(method: type, config: $0) }
            .then(on: bgq) { _ in session.loadTwoFactorConfig() }
            .asVoid()
    }

    func setCsvTimeLock(session: SessionManager, csv: Settings.CsvTime) -> Promise<Void> {
        return Guarantee()
            .then(on: bgq) { session.setCSVTime(value: csv.value(for: session.gdkNetwork)!) }
            .then(on: bgq) { _ in session.loadSettings() }
            .map {_ in
                self.newCsv = nil
                self.currentCsv = csv.value(for: session.gdkNetwork)
            }.asVoid()
    }

    func resetTwoFactor(session: SessionManager, email: String) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { session.resetTwoFactor(email: email, isDispute: false) }
            .then(on: bgq) { _ in session.loadTwoFactorConfig() }
            .asVoid()
    }

    func setEmail(session: SessionManager, email: String, isSetRecovery: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { TwoFactorConfigItem(enabled: isSetRecovery ? false : true, confirmed: true, data: email) }
            .then(on: bgq) { config in session.changeSettingsTwoFactor(method: .email, config: config) }
            .then(on: bgq) { _ in session.loadTwoFactorConfig() }
            .asVoid()
    }

    func setPhoneSms(session: SessionManager, countryCode: String, phone: String, sms: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { TwoFactorConfigItem(enabled: true, confirmed: true, data: countryCode + phone) }
            .then(on: bgq) { session.changeSettingsTwoFactor(method: sms ? .sms : .phone, config: $0) }
            .then(on: bgq) { _ in session.loadTwoFactorConfig() }
            .asVoid()
    }

    func setGauth(session: SessionManager, gauth: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { TwoFactorConfigItem(enabled: true, confirmed: true, data: gauth) }
            .then(on: bgq) { session.changeSettingsTwoFactor(method: .gauth, config: $0) }
            .then(on: bgq) { _ in session.loadTwoFactorConfig() }
            .asVoid()
    }
}
