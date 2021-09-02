enum Constants {
    static let wordsCount = 12
    static let wordsPerPage = 6
    static let wordsPerQuiz = 4
    static let electrumPrefix = "electrum-"
    static let trxPerPage: UInt32 = 30

    //SPV Settings
    static let personalNodeEnabled = "personal_node_enabled"
    static let btcElectrumSrv = "btc_electrum_srv"
    static let liquidElectrumSrv = "liquid_electrum_srv"
    static let testnetElectrumSrv = "testnet_electrum_srv"
}

enum AppStorage {
    static let dontShowTorAlert = "dont_show_tor_alert"
    static let defaultTransactionPriority = "default_transaction_priority"
    static let testnetIsVisible = "testnet_is_visible"
}
