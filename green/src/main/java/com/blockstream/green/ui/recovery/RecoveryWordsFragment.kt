package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryWordsFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    private val args: RecoveryWordsFragmentArgs by navArgs()

    private val viewModel: RecoveryWordsViewModel by viewModel {
        parametersOf(args.args)
    }

    override val useCompose: Boolean = true

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    RecoveryWordsScreen(viewModel = viewModel)
                }
            }
        }
    }
}