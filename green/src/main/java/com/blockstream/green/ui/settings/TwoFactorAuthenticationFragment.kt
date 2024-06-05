package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.settings.TwoFactorAuthenticationScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TwoFactorAuthenticationFragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view,
    0
) {
    val args: TwoFactorAuthenticationFragmentArgs by navArgs()

    val viewModel: TwoFactorAuthenticationViewModel by viewModel {
        parametersOf(
            args.wallet
        )
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val networkViewModels = viewModel.networks.map {
            WalletSettingsViewModel(
                greenWallet = viewModel.greenWallet,
                section = WalletSettingsSection.TwoFactor,
                network = it
            ).also {
                it.parentViewModel = viewModel
            }
        }

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {

                    TwoFactorAuthenticationScreen(
                        viewModel = viewModel,
                        networkViewModels = networkViewModels,
                        network = args.network
                    )
                }
            }
        }
    }
}
