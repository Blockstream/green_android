package com.blockstream.gms

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.blockstream.base.IAppReview
import com.google.android.play.core.review.ReviewManager

class AppReview(private val reviewManager: ReviewManager) : IAppReview {

    override fun showGooglePlayInAppReviewDialog(fragment: Fragment, openBrowser: () -> Unit) {
        Toast.makeText(fragment.requireContext(), "App Review", Toast.LENGTH_SHORT).show()
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
                            openBrowser.invoke()
                            // Temp fix until AppFragment moves to :base
//                            fragment.openBrowser(
//                                Urls.BLOCKSTREAM_GOOGLE_PLAY
//                            )
                        }
                    }
            } else {
                // There was some problem, log or handle the error code.

                openBrowser.invoke()
//                fragment.openBrowser(
//                    Urls.BLOCKSTREAM_GOOGLE_PLAY
//                )
            }
        }
    }
}