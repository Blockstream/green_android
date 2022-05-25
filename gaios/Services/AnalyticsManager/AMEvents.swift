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

extension AnalyticsManager {

    func activeWallet(account: Account?, walletData: WalletData) {
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strWalletFunded] = walletData.walletFunded ? "true" : "false"
            s[AnalyticsManager.strAccountsFunded] = "\(walletData.accountsFunded)"
            s[AnalyticsManager.strAccounts] = "\(walletData.accounts)"
            s[AnalyticsManager.strAccountsTypes] = walletData.accountsTypes
            AnalyticsManager.shared.recordEvent(.walletLogin, sgmt: s)

            recordEvent(.walletActive, sgmt: s)
        }
    }

    func loginWallet(loginType: AnalyticsManager.LoginType, account: Account?) {
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strMethod] = loginType.rawValue
            AnalyticsManager.shared.recordEvent(.walletLogin, sgmt: s)
        }
    }

    func renameWallet() {
        AnalyticsManager.shared.recordEvent(.renameWallet)
    }

    func deleteWallet() {
        AnalyticsManager.shared.userPropertiesDidChange()
        AnalyticsManager.shared.recordEvent(.deleteWallet)
    }

    func renameAccount(account: Account?) {
        if let s = sessSgmt(account) {
            AnalyticsManager.shared.recordEvent(.renameAccount, sgmt: s)
        }
    }

    func startSendTransaction() {
//        cancelEvent(.sendTransaction)
//        startEvent(.sendTransaction)
    }

    func sendTransaction(account: Account?, walletItem: WalletItem?, transactionSgmt: AnalyticsManager.TransactionSegmentation, withMemo: Bool) {

//        if var s = subAccSeg(account, walletType: walletItem?.type) {
//
//            switch transactionSgmt.transactionType {
//            case .transaction:
//                s[AnalyticsManager.strTtransactionType] = AnalyticsManager.TransactionType.send.rawValue
//            case .sweep:
//                s[AnalyticsManager.strTtransactionType] = AnalyticsManager.TransactionType.sweep.rawValue
//            case .bumpFee:
//                s[AnalyticsManager.strTtransactionType] = AnalyticsManager.TransactionType.bump.rawValue
//            }
//
//            s[AnalyticsManager.strAddressInput] = transactionSgmt.addressInputType.rawValue
//            s[AnalyticsManager.strSendAll] = transactionSgmt.sendAll ? "true" : "false"
//            s[AnalyticsManager.strWithMemo] = withMemo ? "true" : "false"
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
            AnalyticsManager.shared.userPropertiesDidChange()
            endEvent(.walletCreate, sgmt: s)
        }
    }

    func startRestoreWallet() {
        cancelEvent(.walletRestore)
        startEvent(.walletRestore)
    }

    func restoreWallet(account: Account?) {
        if let s = sessSgmt(account) {
            AnalyticsManager.shared.userPropertiesDidChange()
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
            s[AnalyticsManager.strType] = data.type.rawValue
            s[AnalyticsManager.strMedia] = data.media.rawValue
            s[AnalyticsManager.strMethod] = data.method.rawValue
            recordEvent(.receiveAddress, sgmt: s)
        }
    }

    func shareTransaction(account: Account?, isShare: Bool) {
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strMethod] = isShare ? AnalyticsManager.strShare : AnalyticsManager.strCopy
            recordEvent(.shareTransaction, sgmt: s)
        }
    }

    func failedWalletLogin(account: Account?, error: Error, prettyError: String?) {
        if var s = sessSgmt(account) {
            if let prettyError = prettyError {
                s[AnalyticsManager.strError] = prettyError
            } else {
                s[AnalyticsManager.strError] = error.localizedDescription
            }
            recordEvent(.failedWalletLogin, sgmt: s)
        }
    }

    func failedTransaction(account: Account?, error: Error, prettyError: String?) {
        if var s = sessSgmt(account) {
            if let prettyError = prettyError {
                s[AnalyticsManager.strError] = prettyError
            } else {
                s[AnalyticsManager.strError] = error.localizedDescription
            }
            recordEvent(.failedTransaction, sgmt: s)
        }
    }

    func recoveryPhraseCheckFailed(onBoardParams: OnBoardParams?, page: Int) {
        if var s = ntwSgmt(onBoardParams) {
            s[AnalyticsManager.strPage] = "\(page)"
            recordEvent(.failedRecoveryPhraseCheck, sgmt: s)
        }
    }
}

extension AnalyticsManager {

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
