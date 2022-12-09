import Foundation

enum Constants {
    static let mnemonicSizeDefault = MnemonicSize._12.rawValue
    static let wordsPerPage = 6
    static let wordsPerQuiz = 4
    static let electrumPrefix = "electrum-"
    static let trxPerPage: UInt32 = 30

    // SPV Settings
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
    static let countlyRemoteConfigAppReview = "app_review"
    static let countlyRemoteConfigBanners = "banners"
    static let countlyRemoteConfigAssets = "liquid_assets"
}

enum AppStorage {
    static let dontShowTorAlert = "dont_show_tor_alert"
    static let defaultTransactionPriority = "default_transaction_priority"
    static let testnetIsVisible = "testnet_is_visible"
    static let userAnalyticsPreference = "user_analytics_preference"
    static let analyticsUUID = "analytics_uuid"
    static let countlyOffset = "countly_offset"
    static let alwaysAskPassphrase = "always_ask_passphrase"
    static let storeReviewDate = "store_review_date"
    static let hideBalance = "hide_balance"
}

enum ExternalUrls {
    static let otaReadMore = URL(string: "https://blockstream.zendesk.com/hc/en-us/articles/4408030503577")!
    static let receiveTransactionHelp = URL(string: "https://help.blockstream.com/hc/en-us/articles/900004651103-How-do-I-receive-assets-on-Blockstream-Green-")!
    static let jadeNeedHelp = URL(string: "https://help.blockstream.com/hc/en-us/articles/4406185830041")!
    static let jadeMoreInfo = URL(string: "https://blockstream.zendesk.com/hc/en-us/articles/4412006238617")!
    static let mnemonicNotWorking = URL(string: "https://help.blockstream.com/hc/en-us/articles/900001388566-Why-is-my-mnemonic-backup-not-working-")!
    static let analyticsReadMore = URL(string: "https://blockstream.zendesk.com/hc/en-us/articles/5988514431897")!
    static let passphraseReadMore = URL(string: "https://help.blockstream.com/hc/en-us/articles/8712301763737")!

    static let aboutBlockstreamGreenWebSite = URL(string: "https://blockstream.com/green/")!
    static let aboutBlockstreamTwitter = URL(string: "https://twitter.com/Blockstream")!
    static let aboutBlockstreamLinkedIn = URL(string: "https://www.linkedin.com/company/blockstream")!
    static let aboutBlockstreamFacebook = URL(string: "https://www.facebook.com/Blockstream")!
    static let aboutBlockstreamTelegram = URL(string: "https://t.me/blockstream_green")!
    static let aboutBlockstreamGitHub = URL(string: "https://github.com/Blockstream")!
    static let aboutBlockstreamYouTube = URL(string: "https://www.youtube.com/channel/UCZNt3fZazX9cwWcC9vjDJ4Q")!

    static let aboutHelpCenter = URL(string: "https://help.blockstream.com/hc/en-us/categories/900000056183-Blockstream-Green/")!
    static let aboutTermsOfService = URL(string: "https://blockstream.com/green/terms/")!
    static let aboutPrivacyPolicy = URL(string: "https://blockstream.com/green/privacy/")!
}
