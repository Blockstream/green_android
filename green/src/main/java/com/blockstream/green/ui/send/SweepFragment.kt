package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.send.SweepViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.send.SweepScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SweepFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    private val args: SweepFragmentArgs by navArgs()

    override val useCompose: Boolean = true

    private val viewModel: SweepViewModel by viewModel {
        parametersOf(args.wallet, args.privateKey, args.accountAsset)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    SweepScreen(viewModel = viewModel)
                }
            }
        }
    }
}