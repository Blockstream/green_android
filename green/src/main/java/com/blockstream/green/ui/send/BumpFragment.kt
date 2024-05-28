package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.send.BumpViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.send.BumpScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class BumpFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    private val args: BumpFragmentArgs by navArgs()

    override val useCompose: Boolean = true

    private val viewModel: BumpViewModel by viewModel {
        parametersOf(args.wallet, args.accountAsset, args.transaction)
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
                    BumpScreen(viewModel = viewModel)
                }
            }
        }
    }
}