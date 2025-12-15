package com.blockstream.green.data

import android.app.Activity
import android.content.res.Configuration
import androidx.fragment.app.FragmentManager
import com.blockstream.data.CountlyBase
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.CountlyWidget
import com.blockstream.data.database.Database
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.managers.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ly.count.android.sdk.ModuleFeedback
import ly.count.android.sdk.ModuleFeedback.CountlyFeedbackWidget

abstract class CountlyAndroid constructor(
    appInfo: AppInfo,
    applicationScope: ApplicationScope,
    settingsManager: SettingsManager,
    database: Database,
) : CountlyBase(appInfo, applicationScope, settingsManager, database) {
    protected val _feedbackWidgetStateFlow = MutableStateFlow<CountlyFeedbackWidget?>(null)
    val feedbackWidgetStateFlow get() = _feedbackWidgetStateFlow.asStateFlow()
    val feedbackWidget get() = _feedbackWidgetStateFlow.value

    abstract fun showFeedbackWidget(supportFragmentManager: FragmentManager)

    abstract fun onStart(activity: Activity)

    abstract fun onStop()

    abstract fun onConfigurationChanged(newConfig: Configuration)

    abstract fun sendFeedbackWidgetData(
        widget: ModuleFeedback.CountlyFeedbackWidget,
        data: Map<String, Any>?
    )

    abstract fun getFeedbackWidgetData(
        widget: ModuleFeedback.CountlyFeedbackWidget,
        callback: (CountlyWidget?) -> Unit
    )
}
