package com.blockstream.green.ui.overview


import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.overview.WalletAssetsViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.overview.WalletAssetsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AssetsFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    val args: AssetsFragmentArgs by navArgs()

    val viewModel: WalletAssetsViewModel by viewModel {
        parametersOf(args.wallet)
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
                    WalletAssetsScreen(viewModel = viewModel)
                }
            }
        }
    }
}