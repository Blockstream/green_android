package com.blockstream.common

const val BTC_POLICY_ASSET = "btc"
const val LN_BTC_POLICY_ASSET = "lnbtc"
const val LBTC_POLICY_ASSET = "lbtc" // Used in preview mostly

const val BTC_UNIT = "BTC"
const val MBTC_UNIT = "mBTC"
const val UBTC_UNIT = "\u00B5BTC"
const val BITS_UNIT = "bits"
const val SATOSHI_UNIT = "sats"

val BitcoinUnits = listOf(BTC_UNIT, MBTC_UNIT, UBTC_UNIT, BITS_UNIT, SATOSHI_UNIT)
val TestnetUnits = listOf("TEST", "mTEST", "\u00B5TEST", "bTEST", "sTEST")

object Urls {
    const val BLOCKSTREAM_GREEN_WEBSITE = "https://blockstream.com/green/"
    const val BLOCKSTREAM_TWITTER = "https://twitter.com/Blockstream"
    const val BLOCKSTREAM_LINKEDIN = "https://www.linkedin.com/company/blockstream"
    const val BLOCKSTREAM_FACEBOOK = "https://www.facebook.com/Blockstream"
    const val BLOCKSTREAM_TELEGRAM = "https://t.me/blockstream_green"
    const val BLOCKSTREAM_GITHUB = "https://github.com/Blockstream"
    const val BLOCKSTREAM_YOUTUBE = "https://www.youtube.com/channel/UCZNt3fZazX9cwWcC9vjDJ4Q"
    const val BLOCKSTREAM_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=com.greenaddress.greenbits_android_wallet"

    const val TERMS_OF_SERVICE = "https://blockstream.com/green/terms/"
    const val PRIVACY_POLICY = "https://blockstream.com/green/privacy/"
    const val BLOCKSTREAM_HELP_NEW_REQUEST =
        "https://help.blockstream.com/hc/en-us/requests/new?tf_900008231623=%s&tf_subject=%s&tf_900003758323=%s&tf_900006375926=%s&tf_900009625166=%s&tf_6167739898649=%s"
    const val HELP_MNEMONIC_NOT_WORKING =
        "https://help.blockstream.com/hc/en-us/articles/900001388566-Why-is-my-mnemonic-backup-not-working-"
    const val HELP_RECEIVE_ASSETS =
        "https://help.blockstream.com/hc/en-us/articles/46683253935513-Receive-bitcoin"
    const val HELP_MNEMONIC_BACKUP = "https://help.blockstream.com/hc/en-us/articles/900001392563-What-is-a-mnemonic-backup-"
    const val HELP_NLOCKTIMES =
        "https://help.blockstream.com/hc/en-us/articles/900004249546-The-upgrade-from-nLockTime-to-CheckSequenceVerify"
    const val HELP_FEES = "https://help.blockstream.com/hc/en-us/articles/4412578550809"
    const val HELP_FUNDING_FEES = "https://help.blockstream.com/hc/en-us/articles/18788578831897"
    const val HELP_RECEIVE_CAPACITY = "https://help.blockstream.com/hc/en-us/articles/18788499177753"
    const val HELP_GET_LIQUID = "https://help.blockstream.com/hc/en-us/articles/900000630846-How-do-I-get-Liquid-Bitcoin-L-BTC-"
    const val HELP_JADE_USB_UPGRADE = "https://help.blockstream.com/hc/en-us/articles/4408030503577"
    const val HELP_WHATS_COLLECTED = "https://help.blockstream.com/hc/en-us/articles/5988514431897"
    const val HELP_BIP39_PASSPHRASE = "https://help.blockstream.com/hc/en-us/articles/8712301763737"
    const val HELP_LIGHTNING_SHORTCUT = "https://help.blockstream.com/hc/en-us/articles/23020279153177"
    const val HELP_JADE_AIRGAPPED = "https://help.blockstream.com/hc/en-us/sections/10426339090713-Air-gapped-Usage"
    const val HELP_AMP_ASSETS = "https://help.blockstream.com/hc/en-us/articles/5301732614169-How-do-I-receive-AMP-assets-"
    const val HELP_QR_PIN_UNLOCK = "https://blockstream.zendesk.com/hc/en-us/articles/28264425434265"
    const val HELP_MASTER_BLINDING_KEY =
        "https://help.blockstream.com/hc/en-us/articles/4403675941017-What-are-the-implications-of-exporting-the-master-blinding-key"
    const val HELP_2FA_PROTECTION =
        "https://help.blockstream.com/hc/en-us/articles/900001391763-How-does-Blockstream-Green-s-2FA-multisig-protection-work#h_01HRYKB9YRHWX02REXYY34VPV9"
    const val HELP_JADE_EXPORT_XPUB = "https://help.blockstream.com/hc/en-us/articles/20272957303577-Add-Jade-to-a-QR-supported-app"
    const val HELP_HW_EXPORT_XPUB = "https://help.blockstream.com/hc/en-us/articles/33140904892057"
    const val HELP_CENTER = "https://help.blockstream.com/hc/en-us/categories/900000056183-Blockstream-Green/"
    const val RECOVERY_TOOL = "https://github.com/greenaddress/garecovery"
    const val JADE_STORE = "https://store.blockstream.com/products/jade-plus"
    const val JADE_TROUBLESHOOT =
        "https://help.blockstream.com/hc/en-us/articles/4406185830041-Why-is-my-Blockstream-Jade-not-connecting-over-Bluetooth-"
    const val LEDGER_SUPPORTED_ASSETS = "https://docs.blockstream.com/green/hww/hww-index.html#ledger-supported-assets"
    const val BLUETOOTH_PERMISSIONS = "https://developer.android.com/guide/topics/connectivity/bluetooth/permissions"
}