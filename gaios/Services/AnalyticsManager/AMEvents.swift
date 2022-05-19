enum AnalyticsEventName: String {
    case debugEvent = "debug_event"
    case walletActive = "wallet_active"
    case walletLogin = "wallet_login"
    case walletCreate = "wallet_create"
    case walletRestore = "wallet_restore"
    case renameWallet = "rename_wallet"
    case deleteWallet = "delete_wallet"
    case renameAccount = "rename_account"
    case createAccount = "create_account"
    case sendTransaction = "send_transaction"
    case receiveAddress = "receive_address"
    case shareTransaction = "share_transaction"
    case failedWalletLogin = "failed_wallet_login"
    case failedRecoveryPhraseCheck = "failed_recovery_phrase_check"
    case failedTransaction = "failed_transaction"
}

extension AMan {

    func activeWallet(account: Account?, walletData: WalletData) {
        if var s = sessSgmt(account) {
            s[AMan.strWalletFunded] = walletData.walletFunded ? "true" : "false"
            s[AMan.strAccountsFunded] = "\(walletData.accountsFunded)"
            s[AMan.strAccounts] = "\(walletData.accounts)"
            s[AMan.strAccountsTypes] = walletData.accountsTypes
            AMan.S.recordEvent(.walletLogin, sgmt: s)

            recordEvent(.walletActive, sgmt: s)
        }
    }

    func loginWallet(loginType: AMan.LoginType, account: Account?) {
        if var s = sessSgmt(account) {
            s[AMan.strMethod] = loginType.rawValue
            AMan.S.recordEvent(.walletLogin, sgmt: s)
        }
    }

    func renameWallet() {
        AMan.S.recordEvent(.renameWallet)
    }

    func deleteWallet() {
        AMan.S.userPropertiesDidChange()
        AMan.S.recordEvent(.deleteWallet)
    }

    func renameAccount(account: Account?) {
        if let s = sessSgmt(account) {
            AMan.S.recordEvent(.renameAccount, sgmt: s)
        }
    }

    func startSendTransaction() {
//        cancelEvent(.sendTransaction)
//        startEvent(.sendTransaction)
    }

    func sendTransaction(account: Account?, walletItem: WalletItem?, transactionSgmt: AMan.TransactionSegmentation, withMemo: Bool) {

//        if var s = subAccSeg(account, walletType: walletItem?.type) {
//
//            switch transactionSgmt.transactionType {
//            case .transaction:
//                s[AMan.strTtransactionType] = AMan.TransactionType.send.rawValue
//            case .sweep:
//                s[AMan.strTtransactionType] = AMan.TransactionType.sweep.rawValue
//            case .bumpFee:
//                s[AMan.strTtransactionType] = AMan.TransactionType.bump.rawValue
//            }
//
//            s[AMan.strAddressInput] = transactionSgmt.addressInputType.rawValue
//            s[AMan.strSendAll] = transactionSgmt.sendAll ? "true" : "false"
//            s[AMan.strWithMemo] = withMemo ? "true" : "false"
//
//            endEvent(.sendTransaction, sgmt: s)
//        }
    }

    func startCreateWallet() {
        cancelEvent(.walletCreate)
        startEvent(.walletCreate)
    }

    func createWallet(account: Account?) {
        if let s = sessSgmt(account) {
            AMan.S.userPropertiesDidChange()
            endEvent(.walletCreate, sgmt: s)
        }
    }

    func startRestoreWallet() {
        cancelEvent(.walletRestore)
        startEvent(.walletRestore)
    }

    func restoreWallet(account: Account?) {
        if let s = sessSgmt(account) {
            AMan.S.userPropertiesDidChange()
            endEvent(.walletRestore, sgmt: s)
        }
    }

    func createAccount(account: Account?, walletType: String?) {
        if let s = subAccSeg(account, walletType: walletType) {
            recordEvent(.createAccount, sgmt: s)
        }
    }

    func receiveAddress(account: Account?, walletType: String?, data: ReceiveAddressData) {
        if var s = subAccSeg(account, walletType: walletType) {
            s[AMan.strType] = data.type.rawValue
            s[AMan.strMedia] = data.media.rawValue
            s[AMan.strMethod] = data.method.rawValue
            recordEvent(.receiveAddress, sgmt: s)
        }
    }

    func shareTransaction(account: Account?, isShare: Bool) {
        if var s = sessSgmt(account) {
            s[AMan.strMethod] = isShare ? AMan.strShare : AMan.strCopy
            recordEvent(.shareTransaction, sgmt: s)
        }
    }

    func failedWalletLogin(account: Account?, error: Error, prettyError: String?) {
        if var s = sessSgmt(account) {
            if let prettyError = prettyError {
                s[AMan.strError] = prettyError
            } else {
                s[AMan.strError] = error.localizedDescription
            }
            recordEvent(.failedWalletLogin, sgmt: s)
        }
    }

    func failedTransaction(account: Account?, error: Error, prettyError: String?) {
        if var s = sessSgmt(account) {
            if let prettyError = prettyError {
                s[AMan.strError] = prettyError
            } else {
                s[AMan.strError] = error.localizedDescription
            }
            recordEvent(.failedTransaction, sgmt: s)
        }
    }

    func recoveryPhraseCheckFailed(onBoardParams: OnBoardParams?, page: Int) {
        if var s = ntwSgmt(onBoardParams) {
            s[AMan.strPage] = "\(page)"
            recordEvent(.failedRecoveryPhraseCheck, sgmt: s)
        }
    }
}

extension AMan {

    enum TransactionType: String {
        case send
        case sweep
        case bump
    }

    enum AddressInputType: String {
        case paste
        case scan
        case bip21
    }

    enum ReceiveAddressType: String {
        case address
        case uri
    }

    enum ReceiveAddressMedia: String {
        case text
        case image
    }

    enum ReceiveAddressMethod: String {
        case share
        case copy
    }

    struct TransactionSegmentation {
        let transactionType: InputType
        let addressInputType: AddressInputType
        let sendAll: Bool
    }

    struct WalletData {
        let walletFunded: Bool
        let accountsFunded: Int
        let accounts: Int
        let accountsTypes: String
    }

    struct ReceiveAddressData {
        let type: ReceiveAddressType
        let media: ReceiveAddressMedia
        let method: ReceiveAddressMethod
    }
}
