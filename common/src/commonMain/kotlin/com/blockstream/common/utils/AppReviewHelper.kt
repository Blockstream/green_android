package com.blockstream.common.utils

import com.blockstream.common.CountlyBase
import com.blockstream.common.managers.SettingsManager
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.monthsUntil

object AppReviewHelper {
    fun shouldAskForReview(
        settingsManager: SettingsManager,
        countly: CountlyBase
    ): Boolean {
        // Feature is not enabled
        if (!settingsManager.storeRateEnabled) {
            return false
        }

        // Get value from Countly
        val askEveryMonths = countly.getRemoteConfigValueAsNumber("app_review") ?: 0

        // Feature is not enabled in Countly
        if (askEveryMonths == 0L) {
            return false
        }

        // Not an exception free session
        if (countly.exceptionCounter > 0L) {
            return false
        }

        val lastShown = settingsManager.whenIsAskedAboutAppReview()

        // Calculate diff in months
        val diffInMonths = lastShown.monthsUntil(Clock.System.now(), TimeZone.currentSystemDefault())

        return diffInMonths >= askEveryMonths
    }
}