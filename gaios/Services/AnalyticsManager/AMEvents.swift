import Foundation
import gdk
import hw

enum AnalyticsEventName: String {
    case debugEvent = "debug_event"
    case walletActive = "wallet_active"
    case walletLogin = "wallet_login"
    case walletCreate = "wallet_create"
    case walletRestore = "wallet_restore"
    case renameWallet = "wallet_rename"
    case deleteWallet = "wallet_delete"
    case renameAccount = "account_rename"
    case createAccount = "account_create"
    case sendTransaction = "send_transaction"
    case receiveAddress = "receive_address"
    case shareTransaction = "share_transaction"
    case failedWalletLogin = "failed_wallet_login"
    case failedRecoveryPhraseCheck = "failed_recovery_phrase_check"
    case failedTransaction = "failed_transaction"
    case appReview = "app_review"

    case walletAdd = "wallet_add"
    case walletNew = "wallet_new"
    case walletHWW = "wallet_hww"
    case accountFirst = "account_first"
    case balanceConvert = "balance_convert"
    case assetChange = "asset_change"
    case assetSelect = "asset_select"
    case accountSelect = "account_select"
    case accountNew = "account_new"
    case connectHWW = "hww_connect"
    case connectedHWW = "hww_connected"

    case jadeInitialize = "jade_initialize"
    case jadeVerifyAddress = "verify_address"
    case jadeOtaStart = "ota_start"
    case jadeOtaComplete = "ota_complete"

    case qrScan = "qr_scan"
}

extension AnalyticsManager {

    func activeWalletStart() {
        startTrace(.walletActive)
        cancelEvent(.walletActive)
        startEvent(.walletActive)
    }

    func activeWalletEnd(account: Account?, walletData: WalletData) {
        endTrace(.walletActive)
        var s = sessSgmt(account)
        s?[AnalyticsManager.strWalletFunded] = walletData.walletFunded ? "true" : "false"
        s?[AnalyticsManager.strAccountsFunded] = "\(walletData.accountsFunded)"
        s?[AnalyticsManager.strAccounts] = "\(walletData.accounts)"
        s?[AnalyticsManager.strAccountsTypes] = walletData.accountsTypes
        endEvent(.walletActive, sgmt: s ?? [:])
    }

    func loginWalletStart() {
        startTrace(.walletLogin)
        cancelEvent(.walletLogin)
        startEvent(.walletLogin)
    }
    
    func loginWalletEnd(account: Account, loginType: AnalyticsManager.LoginType) {
        endTrace(.walletLogin)
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strMethod] = loginType.rawValue
            s[AnalyticsManager.strEphemeralBip39] = "\(account.isEphemeral)"
            endEvent(.walletLogin, sgmt: s)
        }
    }

    func renameWallet() {
        recordEvent(.renameWallet)
    }

    func deleteWallet() {
        AnalyticsManager.shared.userPropertiesDidChange()
        recordEvent(.deleteWallet)
    }

    func renameAccount(account: Account?, walletItem: WalletItem?) {
        if let s = subAccSeg(account, walletItem: walletItem) {
            recordEvent(.renameAccount, sgmt: s)
        }
    }

    func startSendTransaction() {
        startTrace(.sendTransaction)
        cancelEvent(.sendTransaction)
        startEvent(.sendTransaction)
    }

    func endSendTransaction(account: Account?, walletItem: WalletItem?, transactionSgmt: AnalyticsManager.TransactionSegmentation, withMemo: Bool) {
        endTrace(.sendTransaction)

        if var s = subAccSeg(account, walletItem: walletItem) {

            switch transactionSgmt.transactionType {
            case .transaction:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.send.rawValue
            case .sweep:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.sweep.rawValue
            case .bumpFee:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.bump.rawValue
            case .bolt11:
                break
            case .lnurl:
                break
            }

            s[AnalyticsManager.strAddressInput] = transactionSgmt.addressInputType.rawValue
            // s[AnalyticsManager.strSendAll] = transactionSgmt.sendAll ? "true" : "false"
            s[AnalyticsManager.strWithMemo] = withMemo ? "true" : "false"

            endEvent(.sendTransaction, sgmt: s)
        }

    }

    func createWallet(account: Account?) {
        if let s = sessSgmt(account) {
            AnalyticsManager.shared.userPropertiesDidChange()
            recordEvent(.walletCreate, sgmt: s)
        }
    }

    func restoreWallet(account: Account?) {
        if let s = sessSgmt(account) {
            AnalyticsManager.shared.userPropertiesDidChange()
            recordEvent(.walletRestore, sgmt: s)
        }
    }

    func createAccount(account: Account?, walletItem: WalletItem?) {
        if let s = subAccSeg(account, walletItem: walletItem) {
            recordEvent(.createAccount, sgmt: s)
        }
    }

    func receiveAddress(account: Account?, walletItem: WalletItem?, data: ReceiveAddressData) {
        if var s = subAccSeg(account, walletItem: walletItem) {
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
    func failedTransaction(account: Account?, walletItem: WalletItem?, transactionSgmt: AnalyticsManager.TransactionSegmentation, withMemo: Bool, error: Error, prettyError: String?) {
        if var s = subAccSeg(account, walletItem: walletItem) {
            switch transactionSgmt.transactionType {
            case .transaction:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.send.rawValue
            case .sweep:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.sweep.rawValue
            case .bumpFee:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.bump.rawValue
            case .bolt11:
                break
            case .lnurl:
                break
            }
            s[AnalyticsManager.strAddressInput] = transactionSgmt.addressInputType.rawValue
            // s[AnalyticsManager.strSendAll] = transactionSgmt.sendAll ? "true" : "false"
            s[AnalyticsManager.strWithMemo] = withMemo ? "true" : "false"
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

    func appReview(account: Account?, walletItem: WalletItem?) {
        if let s = subAccSeg(account, walletItem: walletItem) {
            recordEvent(.appReview, sgmt: s)
        }
    }

    func addWallet() {
        recordEvent(.walletAdd)
    }

    func newWallet() {
        recordEvent(.walletNew)
    }

    func hwwWallet() {
        recordEvent(.walletHWW)
    }

    func onAccountFirst(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.accountFirst, sgmt: s)
        }
    }

    func convertBalance(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.balanceConvert, sgmt: s)
        }
    }

    func changeAsset(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.assetChange, sgmt: s)
        }
    }

    func selectAsset(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.assetSelect, sgmt: s)
        }
    }

    func selectAccount(account: Account?, walletItem: WalletItem?) {
        if let s = subAccSeg(account, walletItem: walletItem) {
            recordEvent(.accountSelect, sgmt: s)
        }
    }

    func newAccount(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.accountNew, sgmt: s)
        }
    }

    func hwwConnect(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.connectHWW, sgmt: s)
        }
    }

    func hwwConnected(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.connectedHWW, sgmt: s)
        }
    }

    func initializeJade(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.jadeInitialize, sgmt: s)
        }
    }

    func verifyAddressJade(account: Account?, walletItem: WalletItem?) {
        if let s = subAccSeg(account, walletItem: walletItem) {
            recordEvent(.jadeVerifyAddress, sgmt: s)
        }
    }

    func otaStartJade(account: Account?, firmware: Firmware) {
        if let s = firmwareSgmt(account, firmware: firmware) {
            recordEvent(.jadeOtaStart, sgmt: s)
            cancelEvent(.jadeOtaComplete)
            startEvent(.jadeOtaComplete)
        }
    }

    func otaCompleteJade(account: Account?, firmware: Firmware) {
        if let s = firmwareSgmt(account, firmware: firmware) {
            endEvent(.jadeOtaComplete, sgmt: s)
        }
    }

    func scanQr(account: Account?, screen: QrScanScreen) {
        switch screen {
        case .addAccountPK, .send, .walletOverview:
            if var s = sessSgmt(account) {
                s[AnalyticsManager.strScreen] = screen.rawValue
                recordEvent(.qrScan, sgmt: s)
            }
        case .onBoardRecovery:
            if var s = onBoardSgmtUnified(flow: .strRestore) {
                s[AnalyticsManager.strScreen] = screen.rawValue
                recordEvent(.qrScan, sgmt: s)
            }
        case .onBoardWOCredentials:
            if var s = onBoardSgmtUnified(flow: .watchOnly) {
                s[AnalyticsManager.strScreen] = screen.rawValue
                recordEvent(.qrScan, sgmt: s)
            }
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
        let transactionType: TxType
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
