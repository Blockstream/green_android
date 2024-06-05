package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.settings.TwoFactorSetupViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.settings.TwoFactorSetupScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class TwoFactorSetupFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: TwoFactorSetupFragmentArgs by navArgs()

    val viewModel: TwoFactorSetupViewModel by viewModel {
        parametersOf(args.wallet, args.network, args.method, args.action, args.isSmsBackup)
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    TwoFactorSetupScreen(
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}