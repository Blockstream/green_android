import Foundation
import gdk
import hw

extension AnalyticsManager {
    
    typealias Sgmt = [String: String]
    
    func ntwSgmtUnified() -> Sgmt {
        var s = Sgmt()
        if let analyticsNtw = analyticsNetworks {
            s[AnalyticsManager.strNetworks] = analyticsNtw.rawValue
        }
        if let analyticsSec = analyticsSecurity {
            s[AnalyticsManager.strSecurity] = analyticsSec.map { $0.rawValue }.joined(separator: "-")
        }
        return s
    }
    
    func onBoardSgmtUnified(flow: AnalyticsManager.OnBoardFlow) -> Sgmt {
        var s = Sgmt()
        s[AnalyticsManager.strFlow] = flow.rawValue
        return s
    }
    
    func sessSgmt(_ account: Account?) -> Sgmt {
        var s = ntwSgmtUnified()
        if account?.isJade ?? false {
            s[AnalyticsManager.strBrand] = "Blockstream"
            s[AnalyticsManager.strFirmware] = BleViewModel.shared.jade?.version?.jadeVersion ?? ""
            s[AnalyticsManager.strModel] = BleViewModel.shared.jade?.version?.boardType ?? ""
            s[AnalyticsManager.strConnection] = AnalyticsManager.strBle
        }
        if account?.isLedger ?? false {
            s[AnalyticsManager.strBrand] = "Ledger"
            s[AnalyticsManager.strFirmware] = ""
            s[AnalyticsManager.strModel] = "Ledger Nano X"
            s[AnalyticsManager.strConnection] = AnalyticsManager.strBle
        }
        s[AnalyticsManager.strAppSettings] = appSettings()
        return s
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

    func subAccSeg(_ account: Account?, walletItem: WalletItem?) -> Sgmt {
        var s = sessSgmt(account)
        if let walletItem = walletItem {
            s[AnalyticsManager.strAccountType] = walletItem.type.rawValue
            s[AnalyticsManager.strNetwork] = accountNetworkLabel(walletItem.gdkNetwork)
        }
        return s
    }

    func twoFacSgmt(_ account: Account?, walletItem: WalletItem?, twoFactorType: TwoFactorType?) -> Sgmt {
        var s = subAccSeg(account, walletItem: walletItem)
        if let twoFactorType = twoFactorType, let walletItem = walletItem {
            s[AnalyticsManager.str2fa] = twoFactorType.rawValue
            s[AnalyticsManager.strNetwork] = accountNetworkLabel(walletItem.gdkNetwork)
        }
        return s
    }

    func firmwareSgmt(_ account: Account?, firmware: Firmware) -> Sgmt {
        var s = sessSgmt(account)
        s[AnalyticsManager.strSelectedConfig] = firmware.config.lowercased()
        s[AnalyticsManager.strSelectedDelta] = firmware.isDelta == true ? "true" : "false"
        s[AnalyticsManager.strSelectedVersion] = firmware.version
        return s
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
