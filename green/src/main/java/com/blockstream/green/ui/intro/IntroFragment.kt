package com.blockstream.green.ui.intro

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.IntroFragmentBinding
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.ui.wallet.WalletsViewModel
import com.blockstream.green.views.GreenAlertView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


@AndroidEntryPoint
class IntroFragment : AbstractWalletsFragment<IntroFragmentBinding>(R.layout.intro_fragment, menuRes = 0) {
    override val screenName = "Home"

    val viewModel: WalletsViewModel by viewModels()

    override fun getAppViewModel(): AppViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        init(binding.common, viewModel)

        walletRepository.getAllWalletsFlow().onEach {
            if(it.isEmpty()){
                navigate(NavGraphDirections.actionGlobalIntroSetupNewWalletFragment())
            }
        }.launchIn(lifecycleScope)

        binding.buttonSetupWallet.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                errorDialog(throwable)
            }
        }

        binding.buttonAbout.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAboutFragment())
        }
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner
}