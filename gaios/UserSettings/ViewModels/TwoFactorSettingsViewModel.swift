import Foundation
import UIKit

import gdk

class TwoFactorSettingsViewModel {
    
    // load wallet manager for current logged session
    var wm: WalletManager { WalletManager.current! }
    
    // current multisig session
    var sessionBitcoin: SessionManager? { wm.sessions["mainnet"] }
    var sessionLiquid: SessionManager? { wm.sessions["liquid"] }
    var networks: [String] { wm.testnet ? ["testnet", "testnet-liquid"] : ["mainnet", "liquid"] }
    var sessions: [SessionManager] { networks.compactMap { wm.sessions[$0] }}

    private var csvTypes = [Settings.CsvTime]()
    private var csvValues = [Int]()
    private var newCsv: Int?
    private var currentCsv: Int?
    var twoFactorConfig: TwoFactorConfig?
    
    func getTwoFactors(session: SessionManager) async throws -> [TwoFactorItem] {
        if !session.logged {
            return []
        }
        let twoFactorConfig = try await session.loadTwoFactorConfig()
        self.twoFactorConfig = twoFactorConfig
        guard let twoFactorConfig = twoFactorConfig else {
            return []
        }
        return [ TwoFactorItem(name: NSLocalizedString("id_email", comment: ""), enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, maskedData: twoFactorConfig.email.data, type: TwoFactorType.email),
                 TwoFactorItem(name: NSLocalizedString("id_sms", comment: ""), enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, maskedData: twoFactorConfig.sms.data, type: TwoFactorType.sms),
                 TwoFactorItem(name: NSLocalizedString("id_call", comment: ""), enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, maskedData: twoFactorConfig.phone.data, type: TwoFactorType.phone),
                 TwoFactorItem(name: NSLocalizedString("id_authenticator_app", comment: ""), enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: TwoFactorType.gauth) ]
    }


    func disable(session: SessionManager, type: TwoFactorType) async throws {
        let config = TwoFactorConfigItem(enabled: false, confirmed: false, data: "")
        try await session.changeSettingsTwoFactor(method: type, config: config)
        try await session.loadTwoFactorConfig()
    }

    func setCsvTimeLock(session: SessionManager, csv: Settings.CsvTime) async throws {
        try await session.setCSVTime(value: csv.value(for: session.gdkNetwork)!)
        try await session.loadSettings()
        newCsv = nil
        currentCsv = csv.value(for: session.gdkNetwork)
    }

    func resetTwoFactor(session: SessionManager, email: String) async throws {
        try await session.resetTwoFactor(email: email, isDispute: false)
        try await session.loadTwoFactorConfig()
    }

    func setEmail(session: SessionManager, email: String, isSetRecovery: Bool) async throws {
        let config = TwoFactorConfigItem(enabled: isSetRecovery ? false : true, confirmed: true, data: email)
        try await session.changeSettingsTwoFactor(method: .email, config: config)
        try await session.loadTwoFactorConfig()
    }

    func setPhoneSms(session: SessionManager, countryCode: String, phone: String, sms: Bool) async throws {
        let config = TwoFactorConfigItem(enabled: true, confirmed: true, data: countryCode + phone)
        try await session.changeSettingsTwoFactor(method: sms ? .sms : .phone, config: config)
        try await session.loadTwoFactorConfig()
    }

    func setGauth(session: SessionManager, gauth: String) async throws {
        let config = TwoFactorConfigItem(enabled: true, confirmed: true, data: gauth)
        try await session.changeSettingsTwoFactor(method: .gauth, config: config)
        try await session.loadTwoFactorConfig()
    }
}
