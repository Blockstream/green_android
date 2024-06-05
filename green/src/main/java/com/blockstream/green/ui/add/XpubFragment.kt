package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.add.XpubViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.add.XpubScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class XpubFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    val args: XpubFragmentArgs by navArgs()

    override val isAdjustResize = true

    private val viewModel: XpubViewModel by viewModel {
        parametersOf(args.setupArgs)
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
                    XpubScreen(viewModel = viewModel)
                }
            }
        }
    }
}