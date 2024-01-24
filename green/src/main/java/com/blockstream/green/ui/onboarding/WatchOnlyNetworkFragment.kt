package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyNetworkViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyNetworkScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WatchOnlyNetworkFragment :
    AppFragment<ComposeViewBinding>(
        R.layout.compose_view
    ) {

    private val args: WatchOnlyNetworkFragmentArgs by navArgs()

    val viewModel: WatchOnlyNetworkViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if (sideEffect is SideEffects.Navigate) {
            (sideEffect.data as? SetupArgs)?.also {
                navigate(
                    WatchOnlyNetworkFragmentDirections.actionChooseNetworkFragmentToWatchOnlyCredentialsFragment(
                        it
                    )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    WatchOnlyNetworkScreen(viewModel = viewModel)
                }
            }
        }
    }
}
