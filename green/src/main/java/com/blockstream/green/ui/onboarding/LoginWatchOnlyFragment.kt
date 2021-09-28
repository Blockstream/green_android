package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.LoginWatchOnlyFragmentBinding
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.gdk.getGDKErrorCode
import com.blockstream.green.ui.wallet.LoginFragmentDirections
import com.blockstream.green.utils.hideKeyboard
import com.blockstream.libgreenaddress.KotlinGDK
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginWatchOnlyFragment :
    AbstractOnboardingFragment<LoginWatchOnlyFragmentBinding>(
        R.layout.login_watch_only_fragment,
        menuRes = 0
    ) {

    override val isAdjustResize: Boolean = true

    val viewModel: LoginWatchOnlyViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsDialogFragment())
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