import Foundation

extension AnalyticsManager {

    typealias Sgmt = [String: String]

    func ntwSgmt(_ onBoardParams: OnBoardParams?) -> Sgmt? {
        if let network = onBoardParams?.network {
            var s = Sgmt()
            s[AnalyticsManager.strNetwork] = network
            s[AnalyticsManager.strSecurity] = onBoardParams?.singleSig == true ? AnalyticsManager.strSinglesig : AnalyticsManager.strMultisig
            return s
        }
        return nil
    }

    func ntwSgmt(_ account: Account?) -> Sgmt? {
        if let network = account?.network {
            var s = Sgmt()
            s[AnalyticsManager.strNetwork] = network
            s[AnalyticsManager.strSecurity] = account?.isSingleSig == true ? AnalyticsManager.strSinglesig : AnalyticsManager.strMultisig
            return s
        }
        return nil
    }

    func onBoardSgmt(onBoardParams: OnBoardParams?, flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
        if var s = ntwSgmt(onBoardParams) {
            s[AnalyticsManager.strFlow] = flow.rawValue
            return s
        }
        return nil
    }

    func sessSgmt(_ account: Account?) -> Sgmt? {
        if var s = ntwSgmt(account) {

            if account?.isJade ?? false {
                s[AnalyticsManager.strBrand] = "Blockstream"
                s[AnalyticsManager.strFirmware] = BLEManager.shared.fmwVersion ?? ""
                s[AnalyticsManager.strModel] = BLEManager.shared.boardType ?? ""
                s[AnalyticsManager.strConnection] = "BLE"
            }
            if account?.isLedger ?? false {
                s[AnalyticsManager.strBrand] = "Ledger"
                s[AnalyticsManager.strFirmware] = BLEManager.shared.fmwVersion ?? ""
                s[AnalyticsManager.strModel] = "Ledger Nano X"
                s[AnalyticsManager.strConnection] = "BLE"
            }

            s[AnalyticsManager.strAppSettings] = appSettings()

            return s
        }
        return nil
    }

    func subAccSeg(_ account: Account?, walletType: AccountType?) -> Sgmt? {
        if var s = sessSgmt(account), let walletType = walletType {
            s[AnalyticsManager.strAccountType] = walletType.rawValue
            return s
        }
        return nil
    }

    func twoFacSgmt(_ account: Account?, walletType: AccountType?, twoFactorType: TwoFactorType?) -> Sgmt? {
        if var s = subAccSeg(account, walletType: walletType) {
            if let twoFactorType = twoFactorType {
                s[AnalyticsManager.str2fa] = twoFactorType.rawValue
            }
            return s
        }
        return nil
    }

    func appSettings() -> String {
        let settings = UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] ?? [:]
        var settingsProps: [String] = []
        if settings["proxy"] as? Bool ?? false == true { settingsProps.append(AnalyticsManager.strProxy) }
        if settings["tor"] as? Bool ?? false == true { settingsProps.append(AnalyticsManager.strTor) }
        if settings[Constants.spvEnabled] as? Bool ?? false == true { settingsProps.append(AnalyticsManager.strSpv) }
        if UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true { settingsProps.append(AnalyticsManager.strTestnet) }
        if settings[Constants.personalNodeEnabled] as? Bool ?? false == true { settingsProps.append(AnalyticsManager.strElectrumServer) }
        if settings.count == 0 {
            return ""
        }
        return settingsProps.sorted().joined(separator: ",")
    }
}

extension AnalyticsManager {
    // these need a custom dictionary
    func chooseNtwSgmt(flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
        var s = Sgmt()
        s[AnalyticsManager.strFlow] = flow.rawValue
        return s
    }

    func chooseSecuritySgmt(onBoardParams: OnBoardParams?, flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
        if let network = onBoardParams?.network {
            var s = Sgmt()
            s[AnalyticsManager.strNetwork] = network
            s[AnalyticsManager.strFlow] = flow.rawValue
            return s
        }
        return nil
    }

    func chooseRecoverySgmt(onBoardParams: OnBoardParams?, flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
        if let network = onBoardParams?.network {
            var s = Sgmt()
            s[AnalyticsManager.strNetwork] = network
            s[AnalyticsManager.strFlow] = flow.rawValue
            return s
        }
        return nil
    }
}
