package com.blockstream.green.ui.intro

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.HomeFragmentBinding
import com.blockstream.green.ui.dialogs.LightningShortcutDialogFragment
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.views.GreenAlertView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment : AbstractWalletsFragment<HomeFragmentBinding>(R.layout.home_fragment, menuRes = 0) {
    val viewModel: HomeViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override fun getAppViewModel() = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        init(binding.common, viewModel)

        combine(viewModel.softwareWallets, viewModel.ephemeralWallets, viewModel.hardwareWallets) { w1, w2, w3 ->
            w1?.isEmpty() == true && w2?.isEmpty() == true && w3?.isEmpty() == true
        }.onEach {
            if(it){
                navigate(NavGraphDirections.actionGlobalIntroSetupNewWalletFragment())
            }
        }.launchIn(lifecycleScope)

        binding.buttonSetupWallet.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.buttonAbout.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAboutFragment())
        }
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner
}