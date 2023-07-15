package com.blockstream.green.ui.about

import android.os.Bundle
import android.view.View
import com.blockstream.common.models.about.*
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.AboutFragmentBinding
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.utils.AppReviewHelper
import org.koin.androidx.viewmodel.ext.android.viewModel

class AboutFragment : AppFragment<AboutFragmentBinding>(R.layout.about_fragment, menuRes = 0) {

    val viewModel: AboutViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buildVersion = viewModel.version
        binding.year = viewModel.year

        binding.buttonTermsOfService.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickTermsOfService())
        }

        binding.buttonPrivacyPolicy.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickPrivacyPolicy())
        }

        binding.website.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickWebsite())
        }

        binding.twitter.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickTwitter())
        }

        binding.linkedin.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickLinkedIn())
        }

        binding.facebook.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickFacebook())
        }

        binding.telegram.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickTelegram())
        }

        binding.github.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickGitHub())
        }

        binding.youtube.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickYouTube())
        }

        binding.buttonHelp.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickHelp())
        }

        binding.buttonFeedback.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickFeedback())
        }

        binding.logo.setOnClickListener {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickLogo())
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.OpenMenu) {
            openMenu()
        } else if (sideEffect is SideEffects.OpenDialog) {
            AppReviewHelper.showFeedback(this)
        }
    }

    private fun openMenu() {
        showPopupMenu(binding.logo, R.menu.menu_about) { item ->
            when (item.itemId) {
                R.id.copy_device_id -> {
                    viewModel.postEvent(AboutViewModel.LocalEvents.CountlyCopyDeviceId())
                }

                R.id.reset_device_id -> {
                    viewModel.postEvent(AboutViewModel.LocalEvents.CountlyResetDeviceId())
                }

                R.id.zero_offset -> {
                    viewModel.postEvent(AboutViewModel.LocalEvents.CountlyZeroOffset())
                }
            }
            true
        }
    }
}