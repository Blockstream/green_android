package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyPolicyViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyPolicyScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class WatchOnlyPolicyFragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view
) {
    val viewModel: WatchOnlyPolicyViewModel by viewModel()

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
                    WatchOnlyPolicyScreen(viewModel = viewModel)
                }
            }
        }
    }
}