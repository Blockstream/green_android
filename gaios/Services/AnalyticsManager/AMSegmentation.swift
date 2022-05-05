extension AMan {

    typealias Sgmt = [String: String]

    func ntwSgmt(_ onBoardParams: OnBoardParams?) -> Sgmt? {
        if let network = onBoardParams?.network {
            var s = Sgmt()
            s[AMan.strNetwork] = network
            s[AMan.strSecurity] = onBoardParams?.singleSig == true ? AMan.strSinglesig : AMan.strMultisig
            return s
        }
        return nil
    }

    func ntwSgmt(_ account: Account?) -> Sgmt? {
        if let network = account?.network {
            var s = Sgmt()
            s[AMan.strNetwork] = network
            s[AMan.strSecurity] = account?.isSingleSig == true ? AMan.strSinglesig : AMan.strMultisig
            return s
        }
        return nil
    }

    func onBoardSgmt(onBoardParams: OnBoardParams?, flow: AMan.OnBoardFlow) -> Sgmt? {
        if var s = ntwSgmt(onBoardParams) {
            s[AMan.strFlow] = flow.rawValue
            return s
        }
        return nil
    }

    func sessSgmt(_ account: Account?) -> Sgmt? {
        if var s = ntwSgmt(account) {

            if account?.isJade ?? false {
                s[AMan.strBrand] = "Blockstream"
                s[AMan.strFirmware] = BLEManager.shared.fmwVersion ?? ""
                s[AMan.strModel] = BLEManager.shared.boardType ?? ""
                s[AMan.strConnection] = "BLE"
            }
            if account?.isLedger ?? false {
                s[AMan.strBrand] = "Ledger"
                s[AMan.strFirmware] = BLEManager.shared.fmwVersion ?? ""
                s[AMan.strModel] = "Ledger Nano X"
                s[AMan.strConnection] = "BLE"
            }

            return s
        }
        return nil
    }

    func subAccSeg(_ account: Account?, walletType: String?) -> Sgmt? {
        if var s = sessSgmt(account), let walletType = walletType {
            s[AMan.strAccountType] = walletType
            return s
        }
        return nil
    }

    func twoFacSgmt(_ account: Account?, walletType: String?, twoFactorType: TwoFactorType?) -> Sgmt? {
        if var s = subAccSeg(account, walletType: walletType) {
            if let twoFactorType = twoFactorType {
                s[AMan.str2fa] = twoFactorType.rawValue
            }
            return s
        }
        return nil
    }
}

extension AMan {
    // these need a custom dictionary
    func chooseNtwSgmt(flow: AMan.OnBoardFlow) -> Sgmt? {
        var s = Sgmt()
        s[AMan.strFlow] = flow.rawValue
        return s
    }

    func chooseSecuritySgmt(onBoardParams: OnBoardParams?, flow: AMan.OnBoardFlow) -> Sgmt? {
        if let network = onBoardParams?.network {
            var s = Sgmt()
            s[AMan.strNetwork] = network
            s[AMan.strFlow] = flow.rawValue
            return s
        }
        return nil
    }

    func chooseRecoverySgmt(onBoardParams: OnBoardParams?, flow: AMan.OnBoardFlow) -> Sgmt? {
        if let network = onBoardParams?.network {
            var s = Sgmt()
            s[AMan.strNetwork] = network
            s[AMan.strFlow] = flow.rawValue
            return s
        }
        return nil
    }
}
