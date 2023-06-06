package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import com.blockstream.common.Urls
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.SetupNewWalletFragmentBinding
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.bottomsheets.AbstractBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.ConsentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DismissBottomSheetDialogListener
import com.blockstream.green.utils.linkedText
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
open class SetupNewWalletFragment : AbstractOnboardingFragment<SetupNewWalletFragmentBinding>(
        R.layout.setup_new_wallet_fragment,
        menuRes = 0
    ), DismissBottomSheetDialogListener {

    private var pendingNavigation: NavDirections? = null

    override val screenName = "SetupNewWallet"

    private val viewModel: SetupNewWalletViewModel by viewModels()

    override fun getAppViewModel(): AppViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.termsChecked.value = isChecked
        }

        binding.buttonAddWallet.setOnClickListener {
            askForAnalyticsConsentAndNavigate(NavGraphDirections.actionGlobalAddWalletFragment())
            countly.addWallet()
        }

        binding.buttonHardware.setOnClickListener {
            askForAnalyticsConsentAndNavigate(SetupNewWalletFragmentDirections.actionGlobalUseHardwareDeviceFragment())
            settingsManager.setDeviceTermsAccepted()
            countly.hardwareWallet()
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.terms.movementMethod = LinkMovementMethod.getInstance()
        binding.terms.linksClickable = true
        binding.terms.isClickable = true
        binding.terms.text = requireContext().linkedText(
            R.string.id_i_agree_to_the_terms_of_service,
            listOf(
                R.string.id_terms_of_service to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openBrowser(settingsManager.getApplicationSettings(), Urls.TERMS_OF_SERVICE)
                    }
                },
                R.string.id_privacy_policy to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openBrowser(settingsManager.getApplicationSettings(), Urls.PRIVACY_POLICY)
                    }
                }
            )
        )
    }

    private fun askForAnalyticsConsentAndNavigate(directions: NavDirections) {
        if (ConsentBottomSheetDialogFragment.shouldShowConsentDialog(settingsManager)) {
            pendingNavigation = directions
            ConsentBottomSheetDialogFragment.show(childFragmentManager)
        } else {
            navigate(directions)
        }
    }

    override fun dialogDismissed(dialog: AbstractBottomSheetDialogFragment<*>) {
        pendingNavigation?.let { navigate(it) }
    }
}