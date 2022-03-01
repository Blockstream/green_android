import Foundation

enum Constants {
    static let mnemonicSizeDefault = MnemonicSize._12.rawValue
    static let wordsPerPage = 6
    static let wordsPerQuiz = 4
    static let electrumPrefix = "electrum-"
    static let trxPerPage: UInt32 = 30

    //SPV Settings
    static let spvEnabled = "spv_enabled"
    static let personalNodeEnabled = "personal_node_enabled"
    static let btcElectrumSrv = "btc_electrum_srv"
    static let liquidElectrumSrv = "liquid_electrum_srv"
    static let testnetElectrumSrv = "testnet_electrum_srv"
    static let liquidTestnetElectrumSrv = "liquid_testnet_electrum_srv"

    static let btcElectrumSrvDefaultEndPoint = "blockstream.info:700"
    static let liquidElectrumSrvDefaultEndPoint = "blockstream.info:995"
    static let testnetElectrumSrvDefaultEndPoint = "blockstream.info:993"
    static let liquidTestnetElectrumSrvDefaultEndPoint = "blockstream.info:465"
}

enum AppStorage {
    static let dontShowTorAlert = "dont_show_tor_alert"
    static let defaultTransactionPriority = "default_transaction_priority"
    static let testnetIsVisible = "testnet_is_visible"
}

enum ExternalUrls {
    static let otaReadMore = URL(string: "https://blockstream.zendesk.com/hc/en-us/articles/4408030503577")!
    static let receiveTransactionHelp = URL(string: "https://help.blockstream.com/hc/en-us/articles/900004651103-How-do-I-receive-assets-on-Blockstream-Green-")!
    static let jadeNeedHelp = URL(string: "https://help.blockstream.com/hc/en-us/articles/4406185830041")!
    static let jadeMoreInfo = URL(string: "https://blockstream.zendesk.com/hc/en-us/articles/4412006238617")!
    static let mnemonicNotWorking = URL(string: "https://help.blockstream.com/hc/en-us/articles/900001388566-Why-is-my-mnemonic-backup-not-working-")!
}
