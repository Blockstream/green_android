package com.blockstream.green.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Parcelable
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.blockstream.gdk.GreenWallet
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
import com.blockstream.green.ui.dialogs.CountlyNpsDialogFragment
import com.blockstream.green.ui.dialogs.CountlySurveyDialogFragment
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.isProductionFlavor
import com.blockstream.green.utils.toList
import com.blockstream.green.views.GreenAlertView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig
import ly.count.android.sdk.ModuleFeedback
import ly.count.android.sdk.ModuleFeedback.CountlyFeedbackWidget
import ly.count.android.sdk.RemoteConfigCallback
import mu.NamedKLogging
import java.net.URLDecoder
import kotlin.properties.Delegates



class Countly constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    private val sessionManager: SessionManager,
    private val walletRepository: WalletRepository,
) {
    val analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics)
    val rateGooglePlayEnabled = context.resources.getBoolean(R.bool.feature_rate_google_play)

    val remoteConfigUpdateEvent = MutableLiveData(0)

    private val countly = Countly.sharedInstance().also { countly ->
        val config = CountlyConfig(
            context as Application,
            if (isProductionFlavor) PRODUCTION_APP_KEY else DEVELOPMENT_APP_KEY,
            SERVER_URL,
            SERVER_URL_ONION
        ).also {
            if (isDevelopmentOrDebug) {
                it.setEventQueueSizeToSend(1)
            }
            it.setLoggingEnabled(isDevelopmentOrDebug)
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
            // Set automatic remote config download
            it.setRemoteConfigAutomaticDownload(true, RemoteConfigCallback { error ->
                logger.info { if (error.isNullOrBlank()) "Remote Config Completed" else "Remote Config error: $error" }

                if(error.isNullOrBlank()){
                    remoteConfigUpdateEvent.postValue(remoteConfigUpdateEvent.value?.plus(1))
                }
            })
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
    private val remoteConfig = countly.remoteConfig()
    private val attribution = countly.attribution()
    private val feedback = countly.feedback()

    private var analyticsConsent : Boolean by Delegates.observable(settingsManager.getApplicationSettings().analytics) { _, oldValue, newValue ->
        if(oldValue != newValue){
            consent.setConsentFeatureGroup(ANALYTICS_GROUP, newValue)

            if(!newValue){
                resetDeviceId()
            }
        }
    }

    var exceptionCounter = 0L
        private set

    val deviceId: String
        get() = countly.deviceId().id

    private var appSettingsAsString: String? = null

    private val countlyProxy: String?
        get() {
            val appSettings = settingsManager.getApplicationSettings()
            return if(appSettings.tor){
                // Use Orbot
                if(appSettings.proxyUrl?.startsWith("socks5://") == true){
                    appSettings.proxyUrl
                }else {
                    sessionManager.torProxy.value ?: "socks5://tor_not_initialized"
                }
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

        // If no referrer is set, try to get it from the install referrer
        // Empty string is also allowed
        if (!this.sharedPreferences.contains(REFERRER_KEY)) {
            handleReferrer { referrer ->
                // Mark it as complete
                sharedPreferences.edit {
                    putString(REFERRER_KEY, referrer)
                }
            }
        }
        updateFeedbackWidget()
    }

    private val _feedbackWidgetStateFlow = MutableStateFlow<CountlyFeedbackWidget?>(null)
    val feedbackWidgetStateFlow get() = _feedbackWidgetStateFlow.asStateFlow()
    val feedbackWidget get() = _feedbackWidgetStateFlow.value

    fun sendFeedbackWidgetData(widget: CountlyFeedbackWidget, data: Map<String, Any>?){
        feedback.reportFeedbackWidgetManually(widget, null, data)
        // can't use updateFeedback() as the data are sent async
        _feedbackWidgetStateFlow.value = null
    }

    fun getFeedbackWidgetData(widget: CountlyFeedbackWidget, callback: (CountlyWidget?) -> Unit){
        countly.feedback().getFeedbackWidgetData(widget) { data, _ ->
            try{
                callback.invoke(GreenWallet.JsonDeserializer.decodeFromString<CountlyWidget>(data.toString()).also{
                    it.widget = widget
                })

                // Set it to null to hide it from UI, this way user can know that this is a temporary FAB
                _feedbackWidgetStateFlow.value = null
            }catch (e: Exception){
                logger.info { data.toString() }
                e.printStackTrace()
                callback.invoke(null)
            }
        }
    }

    private fun updateFeedbackWidget(){
        countly.feedback().getAvailableFeedbackWidgets { countlyFeedbackWidgets, _ ->
            _feedbackWidgetStateFlow.value = countlyFeedbackWidgets?.firstOrNull()
        }
    }

    fun handleReferrer(onComplete: (referrer: String) -> Unit) {
        InstallReferrerClient.newBuilder(context).build().also { referrerClient ->
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            var cid: String? = null
                            var uid: String? = null
                            var referrer: String? = null

                            try {
                                // The string may be URL Encoded, so decode it just to be sure.
                                // eg. utm_source=google-play&utm_medium=organic
                                // eg. "cly_id=0eabe3eac38ff74556c69ed25a8275b19914ea9d&cly_uid=c27b33b16ac7947fae0ed9e60f3a5ceb96e0e545425dd431b791fe930fabafde4b96c69e0f63396202377a8025f008dfee2a9baf45fa30f7c80958bd5def6056"
                                referrer = URLDecoder.decode(
                                    referrerClient.installReferrer.installReferrer,
                                    "UTF-8"
                                )

                                logger.info { "Referrer: $referrer" }

                                val parts = referrer.split("&")

                                for (part in parts) {
                                    // Countly campaign
                                    if (part.startsWith("cly_id")) {
                                        cid = part.replace("cly_id=", "").trim()
                                    }
                                    if (part.startsWith("cly_uid")) {
                                        uid = part.replace("cly_uid=", "").trim()
                                    }

                                    // Google Play organic
                                    if (part.trim() == "utm_medium=organic") {
                                        cid = if (isProductionFlavor) GOOGLE_PLAY_ORGANIC_PRODUCTION else GOOGLE_PLAY_ORGANIC_DEVELOPMENT
                                    }
                                }
                                
                                attribution.recordDirectAttribution("countly", buildJsonObject {
                                    put("cid", cid)
                                    if (uid != null) {
                                        put("cuid", uid)
                                    }
                                }.toString())

                            } catch (e: Exception) {
                                recordException(e)
                            }

                            onComplete.invoke(referrer ?: "")
                        }
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            // API not available on the current Play Store app.
                            // logger.info { "InstallReferrerService FEATURE_NOT_SUPPORTED" }
                            onComplete.invoke("")
                        }
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            // Connection couldn't be established.
                            // logger.info { "InstallReferrerService SERVICE_UNAVAILABLE" }
                        }
                    }

                    // Disconnect the client
                    referrerClient.endConnection()
                }

                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }

    private fun updateUserProfile(wallets: List<Wallet>) {
        // All wallets
        userProfile.setProperty(USER_PROPERTY_TOTAL_WALLETS, wallets.size.toString())
        userProfile.save()
    }

    fun updateOffset(){
        Countly.sharedInstance().setOffset(settingsManager.getCountlyOffset(if (isDevelopmentFlavor) MAX_OFFSET_DEVELOPMENT else MAX_OFFSET_PRODUCTION))
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
        if(!skipExceptionRecording.contains(throwable.message)) {
            exceptionCounter++
            crashes.recordHandledException(throwable)
        }
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

    fun appReview(session: GreenSession, subAccount: SubAccount?) {
        events.recordEvent(Events.APP_REVIEW.toString(),
            subAccountSegmentation(session, subAccount))
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

                session.ephemeralWallet?.also {
                    if(it.isBip39Ephemeral){
                        segmentation[PARAM_EPHEMERAL_BIP39] = true
                    }
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

    fun recordRating(rating: Int, comment :String){
        countly.ratings().recordRatingWidgetWithID(RATING_WIDGET_ID, rating, null, comment, false)
    }

    fun recordFeedback(rating: Int, email: String?, comment :String){
        countly.ratings().recordRatingWidgetWithID(RATING_WIDGET_ID, rating, email.takeIf { !it.isNullOrBlank() }, comment, !email.isNullOrBlank())
    }

//    fun getRemoteConfigValue(key: String): Any? = remoteConfig.getValueForKey(key)

    fun getRemoteConfigValueAsString(key: String): String? {
        return remoteConfig.getValueForKey(key) as? String
    }

    fun getRemoteConfigValueAsInt(key: String): Int? {
        return remoteConfig.getValueForKey(key) as? Int
    }

    fun getRemoteConfigValueAsLong(key: String): Long? {
        return getRemoteConfigValueAsInt(key)?.toLong() ?: remoteConfig.getValueForKey(key) as? Long
    }

    fun getRemoteConfigValueAsBoolean(key: String): Boolean? {
        return remoteConfig.getValueForKey(key) as? Boolean
    }

    fun getRemoteConfigValueAsJsonElement(key: String): JsonElement? {
        return remoteConfig.getValueForKey(key)?.let {
            Json.parseToJsonElement(it.toString())
        }
    }

    fun getRemoteConfigValueForBanners(key: String): List<Banner>? {
        return try {
            remoteConfig.getValueForKey(key)?.let {
                GreenWallet.JsonDeserializer.decodeFromString<List<Banner>>(it.toString())
            }
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
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

        APP_REVIEW("app_review"),

        SEND_TRANSACTION("send_transaction"),
        FAILED_TRANSACTION("failed_transaction"),
        FAILED_RECOVERY_PHRASE_CHECK("failed_recovery_phrase_check");

        override fun toString(): String = event
    }


    companion object : NamedKLogging("AppCountly") {
        const val SERVER_URL = "https://countly.blockstream.com"
        const val SERVER_URL_ONION = "http://greciphd2z3eo6bpnvd6mctxgfs4sslx4hyvgoiew4suoxgoquzl72yd.onion/"

        const val PRODUCTION_APP_KEY = "351d316234a4a83169fecd7e760ef64bfd638d21"
        const val DEVELOPMENT_APP_KEY = "cb8e449057253add71d2f9b65e5f66f73c073e63"

        const val GOOGLE_PLAY_ORGANIC_PRODUCTION = "95d7943329b90c07d6d7d16b874f97de68fbf67c"
        const val GOOGLE_PLAY_ORGANIC_DEVELOPMENT = "fba90e3e3959c95c18cca2f173bdf31cfb934d47"

        const val REFERRER_KEY = "referrer"

        const val MAX_OFFSET_PRODUCTION     = 12 * 60 * 60 * 1000L // 12 hours
        const val MAX_OFFSET_DEVELOPMENT    =      30 * 60 * 1000L // 30 mins

        const val RATING_WIDGET_ID = "5f15c01425f83c169c33cb65"

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
        const val PARAM_EPHEMERAL_BIP39 = "ephemeral_bip39"

        const val PARAM_TRANSACTION_TYPE = "transaction_type"
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

        const val BLE = "BLE"
        const val USB = "USB"

        const val CREATE = "create"
        const val RESTORE = "restore"
        const val WATCH_ONLY = "watch_only"

        const val SHARE = "share"
        const val COPY = "copy"

        const val SINGLESIG = "singlesig"
        const val MULTISIG = "multisig"

        const val ANALYTICS_GROUP = "analytics"

        val skipExceptionRecording = listOf(
            "id_invalid_amount",
            "id_invalid_address",
            "id_insufficient_funds",
            "id_invalid_private_key",
            "id_action_canceled",
            "id_login_failed",
            "id_error_parsing",
            "id_invalid_address",
            "id_invalid_asset_id"
        )

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
            Countly.CountlyFeatureNames.metrics,
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

interface BannerView {
    fun getBannerAlertView() : GreenAlertView?
}