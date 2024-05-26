package com.blockstream.green.ui.twofactor

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.about.*
import com.blockstream.common.models.twofactor.ReEnable2FAViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.twofactor.ReEnable2FAScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ReEnable2FAFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {
    val args: ReEnable2FAFragmentArgs by navArgs()

    val viewModel: ReEnable2FAViewModel by viewModel {
        parametersOf(
            args.wallet
        )
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
                    ReEnable2FAScreen(viewModel = viewModel)
                }
            }
        }
    }
}