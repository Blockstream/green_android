package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.add.Account2of3ViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.add.Account2of3Screen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class Account2of3Fragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view, 0
) {
    val args: Account2of3FragmentArgs by navArgs()

    private val viewModel: Account2of3ViewModel by viewModel {
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
                    Account2of3Screen(viewModel = viewModel)
                }
            }
        }
    }
}