extension AnalyticsManager {

    static let strNetwork = "network"
    static let strSecurity = "security"
    static let strAccountType = "account_type"
    static let str2fa = "2fa"
    static let strMethod = "method"
    static let strEphemeralBip39 = "ephemeral_bip39"
    static let strPage = "page"
    static let strBrand = "brand"
    static let strModel = "model"
    static let strFirmware = "firmware"
    static let strConnection = "connection"
    static let strError = "error"
    static let strFlow = "flow"

    static let strIsUri = "is_uri"
    static let strIsQR = "is_qr"

    static let strTtransactionType = "tx_type"
    static let strAddressInput = "address_input"
    static let strSendAll = "send_all"
    static let strWithMemo = "with_memo"

    static let strWalletFunded = "wallet_funded"
    static let strAccounts = "accounts"
    static let strAccountsTypes = "accounts_types"
    static let strAccountsFunded = "accounts_funded"

    static let strAppSettings = "app_settings"

    static let strUserPropertyTotalWallets = "total_wallets"

    static let strUserPropertyBitcoinWallets = "bitcoin_wallets"
    static let strUserPropertyBitcoinSinglesigWallets = "bitcoin_singlesig_wallets"
    static let strUserPropertyBitcoinMultisigWallets = "bitcoin_multisig_wallets"

    static let strUserPropertyLiquidWallets = "liquid_wallets"
    static let strUserPropertyLiquidSinglesigWallets = "liquid_singlesig_wallets"
    static let strUserPropertyLiquidMultisigWallets = "liquid_multisig_wallets"

    static let strTor = "tor"
    static let strProxy = "proxy"
    static let strTestnet = "testnet"
    static let strElectrumServer = "electrum_server"
    static let strSpv = "spv"

    static let strBle = "ble"
    static let strUsb = "usb"

    static let strShare = "share"
    static let strCopy = "copy"

    static let strSinglesig = "singlesig"
    static let strMultisig = "multisig"

    static let strAnalyticsGroup = "analytics"
    static let strCountlyGroup = "countly"

    static let strType = "type"
    static let strMedia = "media"

    enum OnBoardFlow: String {
        case strCreate = "create"
        case strRestore = "restore"
        case watchOnly = "watchOnly"
    }

    enum LoginType: String {
        case pin = "pin"
        case biometrics = "biometrics"
        case watchOnly = "watch_only"
        case hardware = "hardware"
    }
    static let maxOffsetProduction = 12 * 60 * 60 * 1000 // 12 hours
    static let maxOffsetDevelopment = 30 * 60 * 1000 // 30 mins

    static let ratingWidgetId = "5f15c01425f83c169c33cb65"
}
