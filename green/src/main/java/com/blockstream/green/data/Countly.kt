package com.blockstream.green.data

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Parcelable
import com.blockstream.gdk.WalletBalances
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.ApplicationScope
import com.blockstream.green.R
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.settings.ApplicationSettings
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.AppActivity
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.isProductionFlavor
import com.blockstream.green.utils.toList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig
import mu.KLogging
import kotlin.properties.Delegates



class Countly constructor(
    private val context: Context,
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    private val sessionManager: SessionManager,
    private val walletRepository: WalletRepository,
) {
    val analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics)

    private val countly = Countly.sharedInstance().also { countly ->
        val config = CountlyConfig(
            context as Application,
            if (context.isProductionFlavor()) PRODUCTION_APP_KEY else DEVELOPMENT_APP_KEY,
            SERVER_URL,
            SERVER_URL_ONION
        ).also {
            if (context.isDevelopmentOrDebug()) {
                it.setEventQueueSizeToSend(1)
            }
            it.setLoggingEnabled(context.isDevelopmentOrDebug())
            // Disable automatic view tracking
            it.setViewTracking(false)
            // Enable crash reporting
            it.enableCrashReporting()
            // APM
            it.setRecordAppStartTime(true)
            // Disable Location
            //it.setDisableLocation()
            // Require user consent
            it.setRequiresConsent(true)
            // Set Device ID
            it.setDeviceId(settingsManager.getCountlyDeviceId())
            // Add initial enabled features
            it.setConsentEnabled(
                if (settingsManager.getApplicationSettings().analytics) {
                    noConsentRequiredGroup + consentRequiredGroup
                } else {
                    noConsentRequiredGroup
                }
            )
            it.setProxy(countlyProxy)
        }

        updateOffset()

        countly.init(config)
    }

    private val events = countly.events()
    private val views = countly.views()
    private val crashes = countly.crashes()
    private val consent = countly.consent()
    private val userProfile = countly.userProfile()

    private var analyticsConsent : Boolean by Delegates.observable(settingsManager.getApplicationSettings().analytics) { _, oldValue, newValue ->
        if(oldValue != newValue){
            consent.setConsentFeatureGroup(ANALYTICS_GROUP, newValue)

            if(!newValue){
                resetDeviceId()
            }
        }
    }

    val deviceId: String
        get() = countly.deviceId().id

    private var appSettingsAsString: String? = null

    private val countlyProxy: String?
        get() {
            val appSettings = settingsManager.getApplicationSettings()
            return if(appSettings.tor){
                sessionManager.torProxy.value ?: "socks5://tor_not_initialized"
            }else if(!appSettings.proxyUrl.isNullOrBlank()){
                appSettings.proxyUrl
            }else{
                null
            }
        }

    init {
        // Create Feature groups
        consent.createFeatureGroup(ANALYTICS_GROUP, consentRequiredGroup)

        settingsManager.getApplicationSettingsLiveData().observeForever {
            analyticsConsent = it.analytics
            appSettingsAsString = appSettingsToString(it)

            updateProxy()
        }

        sessionManager.torProxy.onEach {
            // Proxy endpoint was updated
            updateProxy()
        }.launchIn(applicationScope)

        // Set number of user software wallets
        walletRepository.getWalletsLiveData().observeForever {
            updateUserProfile(it)
        }
    }

    private fun updateUserProfile(wallets: List<Wallet>) {
        // All wallets
        userProfile.setProperty(USER_PROPERTY_TOTAL_WALLETS, wallets.size.toString())
        userProfile.save()
    }

    fun updateOffset(){
        Countly.sharedInstance().setOffset(settingsManager.getCountlyOffset(if (context.isDevelopmentFlavor()) MAX_OFFSET_DEVELOPMENT else MAX_OFFSET_PRODUCTION))
    }

    private fun updateProxy(){
        countly.requestQueue().proxy = countlyProxy
    }

    fun applicationOnCreate(){
        Countly.applicationOnCreate()
    }

    fun resetDeviceId() {
        logger.info { "Reset Device ID" }
        settingsManager.resetCountlyDeviceId()
        settingsManager.resetCountlyOffset()

        countly.deviceId().changeWithoutMerge(settingsManager.getCountlyDeviceId()) {
            // Update offset after the DeviceId is changed in the sdk
            updateOffset()
        }

        // Changing device ID without merging will now clear all consent. It has to be given again after this operation.
        consent.setConsent(noConsentRequiredGroup, true)

        // The following block is required only if you initiate a reset from the ConcentBottomSheetDialog
        if(analyticsConsent){
            consent.setConsentFeatureGroup(ANALYTICS_GROUP, true)
        }

        walletRepository.getWalletsLiveData().value?.let {
            updateUserProfile(it)
        }
    }

    fun recordException(throwable: Throwable) {
        crashes.recordHandledException(throwable)
    }

    fun onStart(activity: AppActivity) {
        countly.onStart(activity)
    }

    fun onStop() {
        countly.onStop()
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        countly.onConfigurationChanged(newConfig)
    }

    fun screenView(view: ScreenView) {
        if (!view.screenIsRecorded && !view.screenName.isNullOrBlank()) {
            view.screenIsRecorded = true
            views.recordView(view.screenName, view.segmentation)
        }
    }

    fun activeWallet(
        session: GreenSession,
        walletBalances: WalletBalances,
        subAccounts: List<SubAccount>
    ) {
        events.recordEvent(
            Events.WALLET_ACTIVE.toString(),
            sessionSegmentation(session).also { segmentation ->

                // Total account balance
                // Note: Balance amount is not tracked, only used to identify a funded wallet
                val accountsTotalBalance = walletBalances.toList().map { it.values.sum() }

                segmentation[PARAM_WALLET_FUNDED] = (accountsTotalBalance.sum() > 0) // Boolean if wallet is funded with any asset
                segmentation[PARAM_ACCOUNTS_FUNDED] = accountsTotalBalance.filter { it > 0 }.count() // number of funded accounts

                segmentation[PARAM_ACCOUNTS] = subAccounts.size
                segmentation[PARAM_ACCOUNTS_TYPES] = subAccounts.map { it.type.gdkType }.toSet().sorted().joinToString(",")
            }
        )
    }

    fun loginWallet(
        wallet: Wallet,
        session: GreenSession,
        loginCredentials: LoginCredentials? = null
    ) {
        events
            .recordEvent(
                Events.WALLET_LOGIN.toString(),
                sessionSegmentation(session).also { segmentation ->
                    when {
                        loginCredentials?.credentialType == CredentialType.PIN -> {
                            LOGIN_TYPE_PIN
                        }
                        loginCredentials?.credentialType == CredentialType.BIOMETRICS -> {
                            LOGIN_TYPE_BIOMETRICS
                        }
                        wallet.isWatchOnly -> {
                            LOGIN_TYPE_WATCH_ONLY
                        }
                        wallet.isHardware -> {
                            LOGIN_TYPE_HARDWARE
                        }
                        else -> null
                    }?.let { method ->
                        segmentation[PARAM_METHOD] = method
                    }
                }
            )
    }

    fun startCreateWallet() {
        // Cancel any previous event
        events.cancelEvent(Events.WALLET_CREATE.toString())
        events.startEvent(Events.WALLET_CREATE.toString())
    }

    fun createWallet(session: GreenSession) {
        events
            .endEvent(
                Events.WALLET_CREATE.toString(),
                sessionSegmentation(session), 1, 0.0
            )
    }

    fun startRestoreWatchOnlyWallet() {
        // Cancel any previous event
        events.cancelEvent(Events.WALLET_RESTORE_WATCH_ONLY.toString())
        events.startEvent(Events.WALLET_RESTORE_WATCH_ONLY.toString())
    }

    fun restoreWatchOnlyWallet(session: GreenSession) {
        events
            .endEvent(
                Events.WALLET_RESTORE_WATCH_ONLY.toString(),
                sessionSegmentation(session), 1, 0.0
            )
    }

    fun startRestoreWallet() {
        // Cancel any previous event
        events.cancelEvent(Events.WALLET_RESTORE.toString())
        events
            .startEvent(
                Events.WALLET_RESTORE.toString()
            )
    }

    fun restoreWallet(session: GreenSession) {
        events
            .endEvent(
                Events.WALLET_RESTORE.toString(),
                sessionSegmentation(session), 1, 0.0
            )
    }

    fun renameWallet() {
        events.recordEvent(Events.WALLET_RENAME.toString())
    }

    fun deleteWallet() {
        events.recordEvent(Events.WALLET_DELETE.toString())
    }

    fun renameAccount(session: GreenSession, subAccount: SubAccount?) {
        events.recordEvent(Events.ACCOUNT_RENAME.toString(), subAccountSegmentation(session, subAccount))
    }

    fun createAccount(session: GreenSession, subAccount: SubAccount) {
        events.recordEvent(
            Events.ACCOUNT_CREATE.toString(),
            subAccountSegmentation(session, subAccount = subAccount)
        )
    }

    fun startSendTransaction(){
        // Cancel any previous event
        events.cancelEvent(Events.SEND_TRANSACTION.toString())
        events.startEvent(Events.SEND_TRANSACTION.toString())
    }

    fun sendTransaction(
        session: GreenSession,
        subAccount: SubAccount?,
        transactionSegmentation: TransactionSegmentation,
        withMemo: Boolean
    ) {
        events
            .endEvent(
                Events.SEND_TRANSACTION.toString(),
                subAccountSegmentation(session, subAccount).also {
                    it[PARAM_TRANSACTION_TYPE] = transactionSegmentation.transactionType.toString()
                    it[PARAM_ADDRESS_INPUT] = transactionSegmentation.addressInputType.toString()
                    it[PARAM_WITH_MEMO] = withMemo
                }
                , 1, 0.0)
    }

    fun receiveAddress(
        addressType: AddressType,
        mediaType: MediaType,
        isShare: Boolean = false,
        subAccount: SubAccount?,
        session: GreenSession
    ) {
        events.recordEvent(
            Events.RECEIVE_ADDRESS.toString(),
            subAccountSegmentation(session, subAccount).also {
                it[PARAM_TYPE] = addressType.toString()
                it[PARAM_MEDIA] = mediaType.toString()
                it[PARAM_METHOD] = SHARE.takeIf { isShare } ?: COPY
            }
        )
    }

    fun shareTransaction(session: GreenSession, isShare: Boolean = false) {
        events.recordEvent(
            Events.SHARE_TRANSACTION.toString(),
            sessionSegmentation(session).also {
                it[PARAM_METHOD] = SHARE.takeIf { isShare } ?: COPY
            }
        )
    }

    fun failedWalletLogin(session: GreenSession, error: Throwable) {
        events
            .recordEvent(
                Events.FAILED_WALLET_LOGIN.toString(),
                sessionSegmentation(session)
                    .also {
                        it[PARAM_ERROR] = error.message ?: "error"
                    }
            )
    }

    fun recoveryPhraseCheckFailed(networkId: String, page: Int) {
        events
            .recordEvent(
                Events.FAILED_RECOVERY_PHRASE_CHECK.toString(),
                networkSegmentation(networkId).also {
                    it[PARAM_PAGE] = page
                }
            )
    }

    fun failedTransaction(session: GreenSession, error: Throwable) {
        events
            .recordEvent(
                Events.FAILED_TRANSACTION.toString(),
                sessionSegmentation(session).also {
                    it[PARAM_ERROR] = error.message ?: "error"
                }
            )
    }

    fun networkSegmentation(networkId: String): HashMap<String, Any> =
        hashMapOf(
            PARAM_NETWORK to Network.canonicalNetworkId(networkId),
            PARAM_SECURITY to if (Network.isSinglesig(networkId)) SINGLESIG else MULTISIG
        )

    fun onBoardingSegmentation(onboardingOptions: OnboardingOptions): HashMap<String, Any> {
        return hashMapOf(
            PARAM_FLOW to when {
                onboardingOptions.isRestoreFlow -> RESTORE
                onboardingOptions.isWatchOnly -> WATCH_ONLY
                else -> CREATE
            },
        ).also {
            onboardingOptions.isSinglesig?.let { isSinglesig ->
                it[PARAM_SECURITY] = if(isSinglesig) SINGLESIG else MULTISIG
            }

            onboardingOptions.networkType?.let { networkType ->
                it[PARAM_NETWORK] = networkType
            }
        } as HashMap<String, Any>
    }

    fun sessionSegmentation(session: GreenSession): HashMap<String, Any> =
        networkSegmentation(session.network.network)
            .also { segmentation ->
                session.device?.let { device ->
                    device.deviceBrand.brand.let { segmentation[PARAM_BRAND] = it }
                    device.hwWallet?.firmwareVersion?.let { segmentation[PARAM_FIRMWARE] = it }
                    device.hwWallet?.model?.let { segmentation[PARAM_MODEL] = it }
                    segmentation[PARAM_CONNECTION] = if (device.isUsb) USB else BLE
                }

                appSettingsAsString?.let {
                    segmentation[USER_PROPERTY_APP_SETTINGS] = it
                }
            }

    fun subAccountSegmentation(session: GreenSession, subAccount: SubAccount?): HashMap<String, Any> =
        sessionSegmentation(session)
            .also { segmentation ->
                subAccount?.let { subAccount ->
                    segmentation[PARAM_ACCOUNT_TYPE] = subAccount.type.gdkType
                }
            }

    fun twoFactorSegmentation(session: GreenSession, subAccount: SubAccount?, twoFactorMethod: TwoFactorMethod): HashMap<String, Any> =
        subAccountSegmentation(session, subAccount)
            .also { segmentation ->
                segmentation[PARAM_2FA] = twoFactorMethod.gdkType
            }

    private fun appSettingsToString(appSettings: ApplicationSettings): String {
        val settingsAsSet = mutableSetOf<String>()

        if (appSettings.enhancedPrivacy) { settingsAsSet.add(ENCHANCED_PRIVACY) }
        if (appSettings.tor) { settingsAsSet.add(TOR) }
        if (!appSettings.proxyUrl.isNullOrBlank()) { settingsAsSet.add(PROXY) }
        if (appSettings.testnet) { settingsAsSet.add(TESTNET) }
        if (appSettings.electrumNode){ settingsAsSet.add(ELECTRUM_SERVER) }
        if (appSettings.spv) { settingsAsSet.add(SPV) }

        return settingsAsSet.sorted().joinToString(",")
    }

    enum class Events(val event: String) {
        WALLET_LOGIN("wallet_login"),
        WALLET_RESTORE("wallet_restore"),
        WALLET_CREATE("wallet_create"),
        WALLET_RESTORE_WATCH_ONLY("wallet_restore_watch_only"),

        WALLET_RENAME("wallet_rename"),
        WALLET_DELETE("wallet_delete"),

        WALLET_ACTIVE("wallet_active"),

        FAILED_WALLET_LOGIN("failed_wallet_login"),

        ACCOUNT_CREATE("account_create"),
        ACCOUNT_RENAME("account_rename"),

        RECEIVE_ADDRESS("receive_address"),

        SHARE_TRANSACTION("share_transaction"),

        SEND_TRANSACTION("send_transaction"),
        FAILED_TRANSACTION("failed_transaction"),
        FAILED_RECOVERY_PHRASE_CHECK("failed_recovery_phrase_check");

        override fun toString(): String = event
    }


    companion object : KLogging() {
        const val SERVER_URL = "https://countly.blockstream.com"
        const val SERVER_URL_ONION = "http://greciphd2z3eo6bpnvd6mctxgfs4sslx4hyvgoiew4suoxgoquzl72yd.onion/"

        const val PRODUCTION_APP_KEY = "351d316234a4a83169fecd7e760ef64bfd638d21"
        const val DEVELOPMENT_APP_KEY = "cb8e449057253add71d2f9b65e5f66f73c073e63"

        const val MAX_OFFSET_PRODUCTION     = 12 * 60 * 60 * 1000L // 12 hours
        const val MAX_OFFSET_DEVELOPMENT    =      30 * 60 * 1000L // 30 mins

        const val PARAM_NETWORK = "network"
        const val PARAM_SECURITY = "security"
        const val PARAM_ACCOUNT_TYPE = "account_type"
        const val PARAM_2FA = "2fa"
        const val PARAM_TYPE = "type"
        const val PARAM_MEDIA = "media"
        const val PARAM_METHOD = "method"
        const val PARAM_PAGE = "page"
        const val PARAM_BRAND = "brand"
        const val PARAM_MODEL = "model"
        const val PARAM_FIRMWARE = "firmware"
        const val PARAM_CONNECTION = "connection"
        const val PARAM_ERROR = "error"
        const val PARAM_FLOW = "flow"

        const val PARAM_TRANSACTION_TYPE = "tx_type"
        const val PARAM_ADDRESS_INPUT = "address_input"
        const val PARAM_WITH_MEMO = "with_memo"

        const val PARAM_WALLET_FUNDED = "wallet_funded"
        const val PARAM_ACCOUNTS = "accounts"
        const val PARAM_ACCOUNTS_TYPES = "accounts_types"
        const val PARAM_ACCOUNTS_FUNDED = "accounts_funded"

        const val LOGIN_TYPE_PIN = "pin"
        const val LOGIN_TYPE_BIOMETRICS = "biometrics"
        const val LOGIN_TYPE_WATCH_ONLY = "watch_only"
        const val LOGIN_TYPE_HARDWARE = "hardware"

        const val USER_PROPERTY_APP_SETTINGS = "app_settings"

        const val USER_PROPERTY_TOTAL_WALLETS = "total_wallets"

        const val TOR = "tor"
        const val PROXY = "proxy"
        const val TESTNET = "testnet"
        const val ELECTRUM_SERVER = "electrum_server"
        const val SPV = "spv"
        const val ENCHANCED_PRIVACY = "enhanced_privacy"

        const val BLE = "ble"
        const val USB = "usb"

        const val CREATE = "create"
        const val RESTORE = "restore"
        const val WATCH_ONLY = "watch_only"

        const val SHARE = "share"
        const val COPY = "copy"

        const val SINGLESIG = "singlesig"
        const val MULTISIG = "multisig"

        const val ANALYTICS_GROUP = "analytics"

        val consentRequiredGroup = arrayOf(
            Countly.CountlyFeatureNames.sessions,
            Countly.CountlyFeatureNames.events,
            Countly.CountlyFeatureNames.views,
            Countly.CountlyFeatureNames.location,
            Countly.CountlyFeatureNames.scrolls,
            Countly.CountlyFeatureNames.clicks,
            Countly.CountlyFeatureNames.crashes,
            Countly.CountlyFeatureNames.apm
        )

        val noConsentRequiredGroup = arrayOf(
            Countly.CountlyFeatureNames.users,
            Countly.CountlyFeatureNames.push,
            Countly.CountlyFeatureNames.starRating,
            Countly.CountlyFeatureNames.feedback,
            Countly.CountlyFeatureNames.remoteConfig,
            Countly.CountlyFeatureNames.attribution,
        )
    }
}

enum class AddressType(val string: String) {
    ADDRESS("address"),
    URI("uri");

    override fun toString(): String = string
}

enum class MediaType(val string: String) {
    TEXT("text"),
    IMAGE("image");

    override fun toString(): String = string
}

enum class TransactionType(val string: String) {
    SEND("send"),
    SWEEP("sweep"),
    BUMP("bump");

    override fun toString(): String = string
}

enum class AddressInputType(val string: String) {
    PASTE("paste"),
    SCAN("scan"),
    BIP21("bip21");

    override fun toString(): String = string
}

@Parcelize
data class TransactionSegmentation constructor(
    val transactionType: TransactionType,
    val addressInputType: AddressInputType?,
    val sendAll: Boolean
) : Parcelable

interface ScreenView{
    var screenIsRecorded: Boolean
    val screenName: String?
    val segmentation: HashMap<String, Any>?
}