package com.blockstream.base

import androidx.fragment.app.Fragment

interface IAppReview {
    fun showGooglePlayInAppReviewDialog(fragment: Fragment, openBrowser: () -> Unit)
}