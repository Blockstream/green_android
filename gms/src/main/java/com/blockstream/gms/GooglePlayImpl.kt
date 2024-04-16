package com.blockstream.gms

import android.app.Activity
import com.blockstream.base.GooglePlay
import com.google.android.play.core.review.ReviewManager

class GooglePlayImpl(private val reviewManager: ReviewManager) : GooglePlay() {

    override fun showInAppReviewDialog(activity: Activity, openBrowser: () -> Unit) {
        val startTime = System.currentTimeMillis()

        // In some circumstances the review flow will not be shown to the user, e.g. they have already seen it recently,
        // so do not assume that calling this method will always display the review dialog.
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(activity, task.result!!)
                    .addOnCompleteListener { _ ->
                        val endTime = System.currentTimeMillis()

                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown. Thus, no
                        // matter the result, we continue our app flow.

                        // As the task is always successful, we can identify if the dialog was shown if the callback is fired very quickly
                        if(endTime - startTime < 500){
                            openBrowser.invoke()
                        }
                    }
            } else {
                // There was some problem, log or handle the error code.
                openBrowser.invoke()
            }
        }
    }
}