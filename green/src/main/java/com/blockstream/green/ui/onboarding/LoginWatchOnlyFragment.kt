package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.LoginWatchOnlyFragmentBinding
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.settings.AppSettingsDialogFragment
import com.blockstream.green.ui.wallet.LoginFragmentDirections
import com.blockstream.green.utils.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginWatchOnlyFragment :
    AbstractOnboardingFragment<LoginWatchOnlyFragmentBinding>(
        R.layout.login_watch_only_fragment,
        menuRes = 0
    ) {

    override val screenName = "OnBoardWatchOnlyCredentials"

    override val isAdjustResize: Boolean = true

    val args: LoginWatchOnlyFragmentArgs by navArgs()

    @Inject
    lateinit var assistedFactory: LoginWatchOnlyViewModel.AssistedFactory

    val viewModel: LoginWatchOnlyViewModel by viewModels{
        LoginWatchOnlyViewModel.provideFactory(
            assistedFactory = assistedFactory, onboardingOptions = args.onboardingOptions
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.isSinglesig = args.onboardingOptions.isSinglesig

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.extenedPublicKey.postValue(it)
            }
        }

        binding.extendedPublicKeyTextInputLayout.endIconCopyMode()

        binding.buttonAppSettings.setOnClickListener {
            AppSettingsDialogFragment.show(childFragmentManager)
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(childFragmentManager)
        }

        binding.buttonLogin.setOnClickListener {
            viewModel.createNewWatchOnlyWallet()
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                hideKeyboard()
                navigate(LoginFragmentDirections.actionGlobalOverviewFragment(navigate.data as Wallet))
            }
        }
    }
}