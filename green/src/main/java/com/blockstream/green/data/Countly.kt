package com.blockstream.green.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import com.blockstream.base.InstallReferrer
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.CountlyWidget
import com.blockstream.common.database.Database
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.Loggable
import com.blockstream.green.ui.AppActivity
import com.blockstream.green.ui.dialogs.CountlyNpsDialogFragment
import com.blockstream.green.ui.dialogs.CountlySurveyDialogFragment
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.isProductionFlavor
import kotlinx.datetime.Clock
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig
import ly.count.android.sdk.ModuleAPM
import ly.count.android.sdk.ModuleAttribution
import ly.count.android.sdk.ModuleConsent
import ly.count.android.sdk.ModuleCrash
import ly.count.android.sdk.ModuleEvents
import ly.count.android.sdk.ModuleFeedback
import ly.count.android.sdk.ModuleFeedback.CountlyFeedbackWidget
import ly.count.android.sdk.ModuleRemoteConfig
import ly.count.android.sdk.ModuleRequestQueue
import ly.count.android.sdk.ModuleUserProfile
import ly.count.android.sdk.ModuleViews
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Countly constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val appInfo: AppInfo,
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    private val database: Database,
    private val installReferrer: InstallReferrer,
): CountlyAndroid(appInfo, applicationScope, settingsManager, database) {

    private val _requestQueue: ModuleRequestQueue.RequestQueue
    private val _feedback: ModuleFeedback.Feedback
    private val _attribution: ModuleAttribution.Attribution
    private val _remoteConfig: ModuleRemoteConfig.RemoteConfig
    private val _userProfile: ModuleUserProfile.UserProfile
    private val _consent: ModuleConsent.Consent
    private val _crashes: ModuleCrash.Crashes
    private val _views: ModuleViews.Views
    private val _events: ModuleEvents.Events
    private val _apm: ModuleAPM.Apm

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
            // it.setLoggingEnabled(isDevelopmentOrDebug)
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
            it.setDeviceId(getDeviceId())
            it.RemoteConfigRegisterGlobalCallback { _, error, _, _ ->
                logger.i { if (error.isNullOrBlank()) "Remote Config Completed" else "Remote Config error: $error" }

                if(error.isNullOrBlank()){
                    remoteConfigUpdated()
                }
            }
            // Set automatic remote config download
            it.enableRemoteConfigAutomaticTriggers()
            // Add initial enabled features
            it.setConsentEnabled(
                if (settingsManager.appSettings.analytics) {
                    noConsentRequiredGroup + consentRequiredGroup
                } else {
                    noConsentRequiredGroup
                }
            )
            it.setProxy(countlyProxy)
        }

        updateOffset()

        countly.init(config)

        _apm = countly.apm()
        _events = countly.events()
        _views = countly.views()
        _crashes = countly.crashes()
        _consent = countly.consent()
        _userProfile = countly.userProfile()
        _remoteConfig = countly.remoteConfig()
        _attribution = countly.attribution()
        _feedback = countly.feedback()
        _requestQueue = countly.requestQueue()

        initBase()
    }

    init {
        logger.i { "Countly init. A privacy-first, user opt-in version of Countly." }
        // Create Feature groups
        _consent.createFeatureGroup(ANALYTICS_GROUP, consentRequiredGroup)

        // If no referrer is set, try to get it from the install referrer
        // Empty string is also allowed
        if (!this.sharedPreferences.contains(REFERRER_KEY)) {
            installReferrer.handleReferrer(_attribution) { referrer ->
                // Mark it as complete
                sharedPreferences.edit {
                    putString(REFERRER_KEY, referrer)
                }
            }
        }

        Countly.applicationOnCreate()

        updateFeedbackWidget()
    }

    override fun onStart(activity: AppActivity) {
        countly.onStart(activity)
    }

    override fun onStop() {
        countly.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        countly.onConfigurationChanged(newConfig)
    }

    override fun updateRemoteConfig(force: Boolean) {
        // Update remote config if required (1 minute distance between calls)
        if (force || _remoteConfigUpdate.plus(30L.toDuration(DurationUnit.SECONDS)) < Clock.System.now()) {
            _remoteConfig.downloadAllKeys(null)
        } else {
            logger.d { "Remote Config skip update: ${Clock.System.now().minus(_remoteConfigUpdate).inWholeSeconds} secs from previous update" }
        }
    }

    override fun updateDeviceId() {
        countly.deviceId().changeWithoutMerge(getDeviceId()) {
            // Update offset after the DeviceId is changed in the sdk
            updateOffset()
        }

        // Changing device ID without merging will now clear all consent. It has to be given again after this operation.
        _consent.setConsent(noConsentRequiredGroup, true)

        // The following block is required only if you initiate a reset from the ConcentBottomSheetDialog
        if(analyticsConsent){
            _consent.setConsentFeatureGroup(ANALYTICS_GROUP, true)
        }

        updateFeedbackWidget()
    }

    override fun updateConsent(withUserConsent: Boolean) {
        _consent.setConsentFeatureGroup(ANALYTICS_GROUP, withUserConsent)
    }

    override fun eventRecord(key: String, segmentation: Map<String, Any>?) {
        _events.recordEvent(key, segmentation, 1, 0.0)
    }
    override fun eventStart(key: String) {
        _events.startEvent(key)
    }

    override fun eventCancel(key: String) {
        _events.cancelEvent(key)
    }

    override fun eventEnd(key: String, segmentation: Map<String, Any>?) {
        _events.endEvent(key, segmentation ,1, 0.0)
    }

    override fun traceStart(key: String) {
        _apm.startTrace(key)
    }

    override fun traceEnd(key: String) {
        _apm.endTrace(key, mutableMapOf())
    }

    override fun sendFeedbackWidgetData(widget: CountlyFeedbackWidget, data: Map<String, Any>?){
        _feedback.reportFeedbackWidgetManually(widget, null, data)
        // can't use updateFeedback() as the data are sent async
        _feedbackWidgetStateFlow.value = null
    }

    override fun getFeedbackWidgetData(widget: CountlyFeedbackWidget, callback: (CountlyWidget?) -> Unit){
        countly.feedback().getFeedbackWidgetData(widget) { data, _ ->
            try{
                callback.invoke(JsonDeserializer.decodeFromString<CountlyWidget>(data.toString()).also {
                    it.widget = widget
                })

                // Set it to null to hide it from UI, this way user can know that this is a temporary FAB
                _feedbackWidgetStateFlow.value = null
            }catch (e: Exception){
                logger.i { data.toString() }
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

    override fun showFeedbackWidget(supportFragmentManager: FragmentManager) {
        feedbackWidget?.type.also { type ->
            if(type == ModuleFeedback.FeedbackWidgetType.nps){
                CountlyNpsDialogFragment.show(supportFragmentManager)
            }else if(type == ModuleFeedback.FeedbackWidgetType.survey){
                CountlySurveyDialogFragment.show(supportFragmentManager)
            }
        }
    }

    override fun updateUserWallets(wallets: Int) {
        _userProfile.setProperty(USER_PROPERTY_TOTAL_WALLETS, wallets.toString())
        _userProfile.save()
    }

    override fun updateOffset(){
        Countly.sharedInstance().setOffset(getOffset())
    }

    override fun setProxy(proxyUrl: String?){
        _requestQueue.proxy = proxyUrl
    }

    override fun recordExceptionImpl(throwable: Throwable) {
        _crashes.recordHandledException(throwable)
    }

    fun recordRating(rating: Int, comment :String){
        countly.ratings().recordRatingWidgetWithID(RATING_WIDGET_ID, rating, null, comment, false)
    }

    override fun recordFeedback(rating: Int, email: String?, comment :String){
        countly.ratings().recordRatingWidgetWithID(RATING_WIDGET_ID, rating, email.takeIf { !it.isNullOrBlank() }, comment, !email.isNullOrBlank())
    }

    override fun viewRecord(viewName: String, segmentation: Map<String, Any>?) {
        _views.recordView(viewName, segmentation)
    }

    override fun getRemoteConfigValueAsString(key: String): String? {
        return _remoteConfig.getValue(key).value?.toString() // convert back to json string if required
    }

    override fun getRemoteConfigValueAsNumber(key: String): Long? {
        return _remoteConfig.getValue(key).value as? Long
    }

    override fun getRemoteConfigValueAsBoolean(key: String): Boolean? {
        return _remoteConfig.getValue(key).value as? Boolean
    }

    companion object : Loggable() {

        val consentRequiredGroup = arrayOf(
            Countly.CountlyFeatureNames.sessions,
            Countly.CountlyFeatureNames.events,
            Countly.CountlyFeatureNames.views,
            Countly.CountlyFeatureNames.location,
            Countly.CountlyFeatureNames.scrolls,
            Countly.CountlyFeatureNames.clicks,
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
            Countly.CountlyFeatureNames.crashes
        )
    }
}