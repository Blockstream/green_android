enum AnalyticsEventKey: String {
    case walletActive = "wallet_active"
    case walletLogin = "wallet_login"
    case walletCcreate = "wallet_create"
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

    func renameAccount() {
        recordEvent(.renameAccount, segmentation: [:])
    }
//    func renameAccount(session: GreenSession) {
//        events.recordEvent(Events.ACCOUNT_RENAME.toString(), sessionSegmentation(session))
//    }

}
