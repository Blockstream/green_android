package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.settings.WatchOnlyViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.settings.WatchOnlyScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WatchOnlyFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: WatchOnlyFragmentArgs by navArgs()

    val viewModel: WatchOnlyViewModel by viewModel {
        parametersOf(args.wallet)
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
                    WatchOnlyScreen(viewModel = viewModel)
                }
            }
        }
    }
}
