package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.onboarding.phone.AddWalletScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddWalletFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view) {
    val viewModel: AddWalletViewModel by viewModel()

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    AddWalletScreen(viewModel = viewModel)
                }
            }
        }
    }
}