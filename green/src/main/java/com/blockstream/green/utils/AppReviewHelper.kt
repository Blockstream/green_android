package com.blockstream.green.utils

import android.view.LayoutInflater
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.Countly
import com.blockstream.green.databinding.DialogFeedbackBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.AppFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManager
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.monthsUntil


object AppReviewHelper {
    fun shouldAskForReview(
        settingsManager: SettingsManager,
        countly: Countly
    ): Boolean {
        // Feature is not enabled
        if(!countly.rateGooglePlayEnabled){
            return false
        }

        // Get value from Countly
        val askEveryMonths = countly.getRemoteConfigValueAsLong("app_review") ?: 0

        // Feature is not enabled in Countly
        if(askEveryMonths == 0L){
            return false
        }

        // Not an exception free session
        if(countly.exceptionCounter > 0L){
            return false
        }

        val lastShown = settingsManager.whenIsAskedAboutAppReview()

        // Calculate diff in months
        val diffInMonths = lastShown.monthsUntil(Clock.System.now(), TimeZone.currentSystemDefault())

        return diffInMonths >= askEveryMonths
    }

    fun showFeedback(fragment: AppFragment<*>) {
        val dialogBinding =
            DialogFeedbackBinding.inflate(LayoutInflater.from(fragment.requireContext()))

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.id_give_us_your_feedback)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.id_send) { _, _ ->
                if (dialogBinding.toggleRate.checkedButtonId > 0 || !dialogBinding.feedbackText.text.isNullOrBlank()) {
                    fragment.countly.recordFeedback(
                        when (dialogBinding.toggleRate.checkedButtonId) {
                            R.id.button1 -> 1
                            R.id.button2 -> 2
                            R.id.button3 -> 3
                            R.id.button4 -> 4
                            R.id.button5 -> 5
                            else -> 0
                        },
                        dialogBinding.emailText.text.toString().trim(),
                        dialogBinding.feedbackText.text.toString()
                    )

                    fragment.snackbar(R.string.id_thank_you_for_your_feedback)
                }
            }
            .setNegativeButton(R.string.id_cancel) { _, _ ->

            }
            .show()

        dialogBinding.emailText.setOnFocusChangeListener { v, hasFocus ->
            val email = dialogBinding.emailText.text?.trim()
            dialogBinding.emailLayout.error = if(hasFocus || email.isNullOrBlank() || email.isEmailValid()){
                null
            }else{
                fragment.getString(R.string.id_not_a_valid_email_address)
            }
        }
    }

    fun showGooglePlayInAppReviewDialog(
        fragment: AppFragment<*>,
        reviewManager: ReviewManager
    ) {
        val startTime = System.currentTimeMillis()

        // In some circumstances the review flow will not be shown to the user, e.g. they have already seen it recently,
        // so do not assume that calling this method will always display the review dialog.
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(fragment.requireActivity(), task.result!!)
                    .addOnCompleteListener { _ ->
                        val endTime = System.currentTimeMillis()

                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown. Thus, no
                        // matter the result, we continue our app flow.

                        // As the task is always successful, we can identify if the dialog was shown if the callback is fired very quickly
                        if(endTime - startTime < 500){
                            fragment.openBrowser(
                                Urls.BLOCKSTREAM_GOOGLE_PLAY
                            )
                        }
                    }
            } else {
                // There was some problem, log or handle the error code.
                fragment.openBrowser(
                    Urls.BLOCKSTREAM_GOOGLE_PLAY
                )
            }
        }
    }
}