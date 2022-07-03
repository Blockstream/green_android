package com.blockstream.green.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.AboutFragmentBinding
import com.blockstream.green.utils.AppReviewHelper
import com.blockstream.green.utils.getVersionName
import com.blockstream.green.utils.openBrowser
import com.google.android.play.core.review.ReviewManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class AboutFragment : AppFragment<AboutFragmentBinding>(R.layout.about_fragment, menuRes = 0) {
    override val screenName = "About"

    @Inject
    lateinit var reviewManager: ReviewManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rateGooglePlay = countly.rateGooglePlayEnabled
        binding.buildVersion = getVersionName(requireContext())
        binding.year = Calendar.getInstance().get(Calendar.YEAR).toString()

        binding.buttonTermsOfService.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.TERMS_OF_SERVICE)
        }

        binding.buttonPrivacyPolocy.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.PRIVACY_POLICY)
        }

        binding.website.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_GREEN_WEBSITE)
        }

        binding.twitter.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_TWITTER)
        }

        binding.linkedin.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_LINKEDIN)
        }

        binding.facebook.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_FACEBOOK)
        }

        binding.telegram.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_TELEGRAM)
        }

        binding.github.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_GITHUB)
        }

        binding.youtube.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.BLOCKSTREAM_YOUTUBE)
        }

        binding.buttonHelp.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_CENTER)
        }

        binding.buttonFeedback.setOnClickListener {
            AppReviewHelper.showFeedback(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_cache -> {
                // TODO clear cache
            }
        }
        return super.onOptionsItemSelected(item)
    }
}