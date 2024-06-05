package com.blockstream.common

import cocoapods.Countly.CLYConsentAttribution
import cocoapods.Countly.CLYConsentEvents
import cocoapods.Countly.CLYConsentFeedback
import cocoapods.Countly.CLYConsentLocation
import cocoapods.Countly.CLYConsentMetrics
import cocoapods.Countly.CLYConsentPerformanceMonitoring
import cocoapods.Countly.CLYConsentPushNotifications
import cocoapods.Countly.CLYConsentRemoteConfig
import cocoapods.Countly.CLYConsentSessions
import cocoapods.Countly.CLYConsentUserDetails
import cocoapods.Countly.CLYConsentViewTracking
import cocoapods.Countly.Countly
import cocoapods.Countly.CountlyConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.database.Database
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.Loggable
import kotlinx.cinterop.convert
import platform.Foundation.NSException

class CountlyIOS(
    appInfo: AppInfo,
    applicationScope: ApplicationScope,
    settingsManager: SettingsManager,
    database: Database
) : CountlyBase(appInfo, applicationScope, settingsManager, database) {

    private val _countly : Countly

    init {
        logger.i { "Countly init. A privacy-first, user opt-in version of Countly." }

        _countly = Countly.sharedInstance()

        val config = CountlyConfig()

        config.appKey = if (appInfo.isDevelopment) DEVELOPMENT_APP_KEY else PRODUCTION_APP_KEY
        config.host = SERVER_URL
        // TODO support ONION ?

        if(appInfo.isDevelopmentOrDebug){
            config.eventSendThreshold = 1u
        }

        config.remoteConfigRegisterGlobalCallback { s, nsError, b, map ->
            logger.i { if (nsError == null) "Remote Config Completed" else "Remote Config error: $nsError" }

//            if(nsError == null){
//            }
            remoteConfigUpdated()
        }

        config.enableRemoteConfigAutomaticTriggers = true
        config.enableRemoteConfigValueCaching = true
        config.remoteConfigCompletionHandler = {
            logger.i { "Countly remoteConfigCompletionHandler." }
        }
        config.requiresConsent = true
        config.consents = if (settingsManager.appSettings.analytics) {
            noConsentRequiredGroup + consentRequiredGroup
        } else {
            noConsentRequiredGroup
        }

        config.deviceID = getDeviceId()
        config.offset = 0u

        logger.i { "Countly starting." }
        _countly.startWithConfig(config)
    }
    override fun updateRemoteConfig() {
        logger.i { "updateRemoteConfig" }
        _countly.remoteConfig().downloadKeys { s, nsError, b, map ->
            logger.d { "Download all keys" }
            remoteConfigUpdated()
        }
    }

    override fun updateOffset() {
        _countly.setNewOffset(getOffset().convert())
    }

    override fun updateDeviceId() {
        _countly.changeDeviceIDWithoutMerge(getDeviceId())
        updateOffset()

        // Changing device ID without merging will now clear all consent. It has to be given again after this operation.
        _countly.giveConsentForFeatures(noConsentRequiredGroup)

        // The following block is required only if you initiate a reset from the ConcentBottomSheetDialog
        if(analyticsConsent){
            _countly.giveConsentForFeatures(consentRequiredGroup)
        }
    }

    override fun updateConsent(withUserConsent: Boolean) {

        if(withUserConsent) {
            _countly.giveConsentForFeatures(consentRequiredGroup)
        } else {
            _countly.cancelConsentForFeatures(consentRequiredGroup)
        }
    }

    override fun viewRecord(viewName: String, segmentation: Map<String, Any>?) {
        if(segmentation == null){
            _countly.recordView(viewName)
        }else{
            _countly.recordView(viewName, segmentation as Map<Any?, *>)
        }
    }

    override fun eventRecord(key: String, segmentation: Map<String, Any>?) {
        if(segmentation == null){
            _countly.recordEvent(key)
        }else{
            _countly.recordEvent(key, segmentation as Map<Any?, *>)
        }
    }

    override fun eventStart(key: String) {
        _countly.startEvent(key)
    }

    override fun eventCancel(key: String) {
        _countly.cancelEvent(key)
    }

    override fun eventEnd(key: String, segmentation: Map<String, Any>?) {
        if(segmentation == null) {
            _countly.endEvent(key)
        }else{
            _countly.endEvent(key, segmentation as Map<Any?, *>, 1u, 0.0)
        }
    }

    override fun traceStart(key: String) {
        _countly.startCustomTrace(key)
    }

    override fun traceEnd(key: String) {
        _countly.endCustomTrace(key, null)
    }

    override fun setProxy(proxyUrl: String?) {
        // TODO

    }

    override fun updateUserWallets(wallets: Int) {
        // TODO investigate
        // val user : CountlyUserDetails = _countly.user()
        // user.custom = mapOf(USER_PROPERTY_TOTAL_WALLETS to wallets.toString()) as? CountlyUserDetailsNullableDictionaryProtocol
        // user.save()
    }

    override fun getRemoteConfigValueAsString(key: String): String? {
        return (_countly.remoteConfigValueForKey(key) as? String).also {
            logger.d { "getRemoteConfigValueAsString: $key: $it" }
        }
    }

    override fun getRemoteConfigValueAsBoolean(key: String): Boolean? {
        return (_countly.remoteConfigValueForKey(key) as? Boolean).also {
            logger.d { "getRemoteConfigValueAsBoolean: $key: $it" }
        }
    }

    override fun getRemoteConfigValueAsNumber(key: String): Long? {
        return (_countly.remoteConfigValueForKey(key) as? Long).also {
            logger.d { "getRemoteConfigValueAsNumber: $key: $it" }
        }
    }

    override fun recordExceptionImpl(throwable: Throwable) {
        NSException.exceptionWithName(name = throwable::class.simpleName, reason = throwable.message ?: "", userInfo = null).also {
            _countly.recordHandledException(it)
        }
    }

    override fun recordFeedback(rating: Int, email: String?, comment: String) {
        _countly.recordRatingWidgetWithID(
            widgetID = RATING_WIDGET_ID,
            rating = rating.convert(),
            email = email.takeIf { !it.isNullOrBlank() },
            comment = comment,
            userCanBeContacted = !email.isNullOrBlank()
        )
    }

    companion object : Loggable() {
        val consentRequiredGroup = listOf(
            CLYConsentSessions,
            CLYConsentEvents,
            CLYConsentViewTracking,
            CLYConsentLocation,
            CLYConsentPerformanceMonitoring
        )

        val noConsentRequiredGroup = listOf(
            CLYConsentMetrics,
            CLYConsentUserDetails,
            CLYConsentPushNotifications,
            CLYConsentFeedback,
            CLYConsentRemoteConfig,
            CLYConsentAttribution,
            CLYConsentAttribution
        )
    }
}