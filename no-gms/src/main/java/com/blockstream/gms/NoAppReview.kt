package com.blockstream.gms

import androidx.fragment.app.Fragment
import com.blockstream.base.IAppReview

class NoAppReview: IAppReview {
    override fun showGooglePlayInAppReviewDialog(fragment: Fragment, openBrowser: () -> Unit) { }
}