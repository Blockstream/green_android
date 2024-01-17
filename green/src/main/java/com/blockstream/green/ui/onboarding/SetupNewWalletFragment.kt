package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.SetupNewWalletFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.ui.bottomsheets.AbstractBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.ConsentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DismissBottomSheetDialogListener
import com.blockstream.green.utils.linkedText
import org.koin.androidx.viewmodel.ext.android.viewModel


open class SetupNewWalletFragment : AppFragment<SetupNewWalletFragmentBinding>(
    R.layout.setup_new_wallet_fragment,
    menuRes = 0
), DismissBottomSheetDialogListener {
    private var _pendingSideEffect: SetupNewWalletViewModel.LocalSideEffects.ShowConsent? = null

    private val viewModel: SetupNewWalletViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is SetupNewWalletViewModel.LocalSideEffects.NavigateAddWallet -> {
                navigate(NavGraphDirections.actionGlobalAddWalletFragment())
            }

            is SetupNewWalletViewModel.LocalSideEffects.NavigateUseHardwareDevice -> {
                navigate(NavGraphDirections.actionGlobalUseHardwareDeviceFragment())
            }

            is SetupNewWalletViewModel.LocalSideEffects.ShowConsent -> {
                _pendingSideEffect = sideEffect
                ConsentBottomSheetDialogFragment.show(childFragmentManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.termsOfServiceIsChecked.value = isChecked
        }

        binding.buttonAddWallet.setOnClickListener {
            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickAddWallet)
        }

        binding.buttonHardware.setOnClickListener {
            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickUseHardwareDevice)
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.terms.movementMethod = LinkMovementMethod.getInstance()
        binding.terms.linksClickable = true
        binding.terms.isClickable = true
        binding.terms.text = requireContext().linkedText(
            text = R.string.id_i_agree_to_the_terms_of_service,
            links = listOf(
                R.string.id_terms_of_service to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickTermsOfService())
                    }
                },
                R.string.id_privacy_policy to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickPrivacyPolicy())
                    }
                }
            )
        )
    }

    override fun dialogDismissed(dialog: AbstractBottomSheetDialogFragment<*>) {
        _pendingSideEffect?.let {
            viewModel.postEvent(it.event)
        }
    }
}