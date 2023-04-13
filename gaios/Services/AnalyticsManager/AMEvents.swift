import Foundation
import gdk

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
    case jadeInitialize = "jade_initialize"
    case connectHWW = "hww_connect"
    case connectedHWW = "hww_connected"
}

extension AnalyticsManager {

    func activeWallet(account: Account?, walletData: WalletData) {
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strWalletFunded] = walletData.walletFunded ? "true" : "false"
            s[AnalyticsManager.strAccountsFunded] = "\(walletData.accountsFunded)"
            s[AnalyticsManager.strAccounts] = "\(walletData.accounts)"
            s[AnalyticsManager.strAccountsTypes] = walletData.accountsTypes

            recordEvent(.walletActive, sgmt: s)
        }
    }

    func loginWallet(loginType: AnalyticsManager.LoginType, ephemeralBip39: Bool, account: Account?) {
        if var s = sessSgmt(account) {
            s[AnalyticsManager.strMethod] = loginType.rawValue
            s[AnalyticsManager.strEphemeralBip39] = "\(ephemeralBip39)"
            recordEvent(.walletLogin, sgmt: s)
        }
    }

    func renameWallet() {
        recordEvent(.renameWallet)
    }

    func deleteWallet() {
        AnalyticsManager.shared.userPropertiesDidChange()
        recordEvent(.deleteWallet)
    }

    func renameAccount(account: Account?, walletType: AccountType?) {
        if let s = subAccSeg(account, walletType: walletType) {
            recordEvent(.renameAccount, sgmt: s)
        }
    }

    func sendTransaction(account: Account?, walletItem: WalletItem?, transactionSgmt: AnalyticsManager.TransactionSegmentation, withMemo: Bool) {

        if var s = subAccSeg(account, walletType: walletItem?.type) {

            switch transactionSgmt.transactionType {
            case .transaction:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.send.rawValue
            case .sweep:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.sweep.rawValue
            case .bumpFee:
                s[AnalyticsManager.strTransactionType] = AnalyticsManager.TransactionType.bump.rawValue
            }

            s[AnalyticsManager.strAddressInput] = transactionSgmt.addressInputType.rawValue
            // s[AnalyticsManager.strSendAll] = transactionSgmt.sendAll ? "true" : "false"
            s[AnalyticsManager.strWithMemo] = withMemo ? "true" : "false"

            recordEvent(.sendTransaction, sgmt: s)
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

    func createAccount(account: Account?, walletType: AccountType?) {
        if let s = subAccSeg(account, walletType: walletType) {
            recordEvent(.createAccount, sgmt: s)
        }
    }

    func receiveAddress(account: Account?, walletType: AccountType?, data: ReceiveAddressData) {
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

    func appReview(account: Account?, walletType: AccountType?) {
        if let s = subAccSeg(account, walletType: walletType) {
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

    func selectAccount(account: Account?, walletType: AccountType?) {
        if let s = subAccSeg(account, walletType: walletType) {
            recordEvent(.accountSelect, sgmt: s)
        }
    }

    func newAccount(account: Account?) {
        if let s = sessSgmt(account) {
            recordEvent(.accountNew, sgmt: s)
        }
    }

    func initJade() {
        recordEvent(.jadeInitialize)
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
