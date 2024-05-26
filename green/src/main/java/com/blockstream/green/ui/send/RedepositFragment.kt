package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.send.RedepositViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.send.RedepositScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RedepositFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    val args: RedepositFragmentArgs by navArgs()

    val viewModel: RedepositViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.accountAsset,
            args.isRedeposit2FA
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    RedepositScreen(viewModel = viewModel)
                }
            }
        }
    }
}
