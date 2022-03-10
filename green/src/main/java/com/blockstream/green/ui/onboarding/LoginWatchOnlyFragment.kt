package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.LoginWatchOnlyFragmentBinding
import com.blockstream.green.gdk.getGDKErrorCode
import com.blockstream.green.ui.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.LoginFragmentDirections
import com.blockstream.green.utils.*
import com.blockstream.libgreenaddress.KotlinGDK
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginWatchOnlyFragment :
    AbstractOnboardingFragment<LoginWatchOnlyFragmentBinding>(
        R.layout.login_watch_only_fragment,
        menuRes = 0
    ) {

    override val isAdjustResize: Boolean = true

    val args: LoginWatchOnlyFragmentArgs by navArgs()

    @Inject
    lateinit var assistedFactory: LoginWatchOnlyViewModel.AssistedFactory

    val viewModel: LoginWatchOnlyViewModel by viewModels{
        LoginWatchOnlyViewModel.provideFactory(
            assistedFactory = assistedFactory, isMultisig = args.isMultisig
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.isMultisig = args.isMultisig

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.extenedPublicKey.postValue(it)
            }
        }

        binding.extendedPublicKeyTextInputLayout.endIconCopyMode()

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsDialogFragment())
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.open(this)
        }

        settingsManager.getApplicationSettingsLiveData().observe(viewLifecycleOwner){
            binding.showTestnet = it.testnet
            viewModel.isTestnet.postValue(false)
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(getString(if (it.getGDKErrorCode() == KotlinGDK.GA_ERROR) R.string.id_user_not_found_or_invalid else R.string.id_connection_failed))
            }
        }

        viewModel.newWallet.observe(viewLifecycleOwner) {
            if (it != null) {
                hideKeyboard()
                navigate(LoginFragmentDirections.actionGlobalOverviewFragment(it))
            }
        }
    }
}