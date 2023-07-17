import Foundation
import gdk
import hw

extension AnalyticsManager {

    typealias Sgmt = [String: String]

    func ntwSgmt(_ onBoardParams: OnBoardParams?) -> Sgmt? {
//        if let network = onBoardParams?.network {
//            var s = Sgmt()
//            s[AnalyticsManager.strNetwork] = network
//            s[AnalyticsManager.strSecurity] = onBoardParams?.singleSig == true ? AnalyticsManager.strSinglesig : AnalyticsManager.strMultisig
//            return s
//        }
        return nil
    }

//    func ntwSgmt(_ account: Account?) -> Sgmt? {
//        if let network = account?.network {
//            var s = Sgmt()
//            s[AnalyticsManager.strNetwork] = network
//            s[AnalyticsManager.strSecurity] = account?.isSingleSig == true ? AnalyticsManager.strSinglesig : AnalyticsManager.strMultisig
//            return s
//        }
//        return nil
//    }

    func ntwSgmtUnified() -> Sgmt? {
        if let analyticsNtw = analyticsNtw, let analyticsSec = analyticsSec {
            var s = Sgmt()
            s[AnalyticsManager.strNetworks] = analyticsNtw.rawValue
            s[AnalyticsManager.strSecurity] = analyticsSec.map { $0.rawValue }.joined(separator: "-")
            return s
        }
        return nil
    }

    func onBoardSgmtUnified(flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
        var s = Sgmt()
        s[AnalyticsManager.strFlow] = flow.rawValue
        return s
    }

    func sessSgmt(_ account: Account?) -> Sgmt? {
        if var s = ntwSgmtUnified() {

            if account?.isJade ?? false {
                s[AnalyticsManager.strBrand] = "Blockstream"
                s[AnalyticsManager.strFirmware] = BLEManager.shared.fmwVersion ?? ""
                s[AnalyticsManager.strModel] = BLEManager.shared.boardType ?? ""
                s[AnalyticsManager.strConnection] = AnalyticsManager.strBle
            }
            if account?.isLedger ?? false {
                s[AnalyticsManager.strBrand] = "Ledger"
                s[AnalyticsManager.strFirmware] = BLEManager.shared.fmwVersion ?? ""
                s[AnalyticsManager.strModel] = "Ledger Nano X"
                s[AnalyticsManager.strConnection] = AnalyticsManager.strBle
            }

            s[AnalyticsManager.strAppSettings] = appSettings()

            return s
        }
        return nil
    }

    func accountNetworkLabel(_ gdkNetwork: GdkNetwork) -> String {
        let server: String? = {
            if gdkNetwork.lightning { return "greenlight" }
            if gdkNetwork.multisig { return "legacy" }
            if gdkNetwork.singlesig { return "electrum" }
            return nil
        }()
        let liquid = gdkNetwork.liquid ? "liquid" : nil
        let mainnet = gdkNetwork.liquid && gdkNetwork.mainnet ? nil : gdkNetwork.mainnet ? "mainnet" : "testnet"
        return [server, liquid, mainnet].compactMap { $0 }.joined(separator: "-")
    }

    func subAccSeg(_ account: Account?, walletItem: WalletItem?) -> Sgmt? {
        if var s = sessSgmt(account), let walletItem = walletItem {
            s[AnalyticsManager.strAccountType] = walletItem.type.rawValue
            s[AnalyticsManager.strNetwork] = accountNetworkLabel(walletItem.gdkNetwork)
            return s
        }
        return nil
    }

    func twoFacSgmt(_ account: Account?, walletItem: WalletItem?, twoFactorType: TwoFactorType?) -> Sgmt? {
        if var s = subAccSeg(account, walletItem: walletItem) {
            if let twoFactorType = twoFactorType, let walletItem = walletItem {
                s[AnalyticsManager.str2fa] = twoFactorType.rawValue
                s[AnalyticsManager.strNetwork] = accountNetworkLabel(walletItem.gdkNetwork)
            }
            return s
        }
        return nil
    }

    func firmwareSgmt(_ account: Account?, firmware: Firmware) -> Sgmt? {
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strSelectedConfig] = firmware.config.lowercased()
            s[AnalyticsManager.strSelectedDelta] = firmware.isDelta == true ? "true" : "false"
            s[AnalyticsManager.strSelectedVersion] = firmware.version
            return s
        }
        return nil
    }

    func appSettings() -> String {
        let settings = GdkSettings.read()
        var settingsProps: [String] = []
        if settings?.proxy ?? false == true {
            settingsProps.append(AnalyticsManager.strProxy)
        }
        if settings?.tor ?? false == true {
            settingsProps.append(AnalyticsManager.strTor)
        }
        if settings?.spvEnabled ?? false == true {
            settingsProps.append(AnalyticsManager.strSpv)
        }
        if AppSettings.shared.testnet {
            settingsProps.append(AnalyticsManager.strTestnet)
        }
        if settings?.personalNodeEnabled ?? false == true {
            settingsProps.append(AnalyticsManager.strElectrumServer)
        }
        if settingsProps.count == 0 {
            return ""
        }
        return settingsProps.sorted().joined(separator: ",")
    }
}

extension AnalyticsManager {
    // these need a custom dictionary
//    func chooseNtwSgmt(flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
//        var s = Sgmt()
//        s[AnalyticsManager.strFlow] = flow.rawValue
//        return s
//    }

//    func chooseSecuritySgmt(onBoardParams: OnBoardParams?, flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
//        if let network = onBoardParams?.network {
//            var s = Sgmt()
//            s[AnalyticsManager.strNetwork] = network
//            s[AnalyticsManager.strFlow] = flow.rawValue
//            return s
//        }
//        return nil
//    }
//
//    func chooseRecoverySgmt(onBoardParams: OnBoardParams?, flow: AnalyticsManager.OnBoardFlow) -> Sgmt? {
//        if let network = onBoardParams?.network {
//            var s = Sgmt()
//            s[AnalyticsManager.strNetwork] = network
//            s[AnalyticsManager.strFlow] = flow.rawValue
//            return s
//        }
//        return nil
//    }
}
