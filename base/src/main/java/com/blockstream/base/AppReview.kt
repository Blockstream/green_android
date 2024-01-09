package com.blockstream.base

import androidx.fragment.app.Fragment

open class AppReview {
    open fun showGooglePlayInAppReviewDialog(fragment: Fragment, openBrowser: () -> Unit) { }
}