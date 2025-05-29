package com.blockstream.green.data

import android.app.Activity
import android.content.res.Configuration
import androidx.fragment.app.FragmentManager
import com.blockstream.common.data.CountlyWidget
import com.blockstream.common.database.Database
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.utils.Loggable
import ly.count.android.sdk.ModuleFeedback

class CountlyNoOp constructor(
    appInfo: AppInfo,
    applicationScope: ApplicationScope,
    settingsManager: SettingsManager,
    database: Database,
) : CountlyAndroid(appInfo, applicationScope, settingsManager, database) {
    init {
        logger.i { "CountlyNoOp init. A No-Op version of Countly class guarantees your privacy." }
    }

    override fun showFeedbackWidget(supportFragmentManager: FragmentManager) {

    }

    override fun onStart(activity: Activity) {

    }

    override fun onStop() {
    }

    override fun onConfigurationChanged(newConfig: Configuration) {

    }

    override fun sendFeedbackWidgetData(
        widget: ModuleFeedback.CountlyFeedbackWidget,
        data: Map<String, Any>?
    ) {

    }

    override fun getFeedbackWidgetData(
        widget: ModuleFeedback.CountlyFeedbackWidget,
        callback: (CountlyWidget?) -> Unit
    ) {

    }

    override fun updateRemoteConfig(force: Boolean) {

    }

    override fun updateOffset() {

    }

    override fun updateDeviceId() {

    }

    override fun updateConsent(withUserConsent: Boolean) {

    }

    override fun viewRecord(viewName: String, segmentation: Map<String, Any>?) {

    }

    override fun eventRecord(key: String, segmentation: Map<String, Any>?) {

    }

    override fun eventStart(key: String) {

    }

    override fun eventCancel(key: String) {

    }

    override fun eventEnd(key: String, segmentation: Map<String, Any>?) {

    }

    override fun traceStart(key: String) {

    }

    override fun traceEnd(key: String) {

    }

    override fun setProxy(proxyUrl: String?) {

    }

    override fun updateUserWallets(wallets: Int) {

    }

    override fun getRemoteConfigValueAsString(key: String): String? {
        return null
    }

    override fun getRemoteConfigValueAsBoolean(key: String): Boolean? {
        return null
    }

    override fun getRemoteConfigValueAsNumber(key: String): Long? {
        return null
    }

    override fun recordExceptionImpl(throwable: Throwable) {

    }

    override fun recordFeedback(rating: Int, email: String?, comment: String) {

    }

    companion object : Loggable()
}
