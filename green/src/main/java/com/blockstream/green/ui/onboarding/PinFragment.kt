package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Event
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.phone.PinViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.onboarding.phone.PinScreen
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.dialogs.EnableLightningShortcut
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PinFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view){
    private val args: PinFragmentArgs by navArgs()
    private val viewModel: PinViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        ((sideEffect as? SideEffects.NavigateTo)?.destination as? NavigateDestinations.WalletOverview)?.also {
            navigate(NavGraphDirections.actionGlobalWalletOverviewFragment(it.greenWallet))
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
                    PinScreen(viewModel = viewModel)
                }
            }
        }

        viewModel.navData.onEach {
            setToolbarVisibility(it.isVisible)
            onBackCallback.isEnabled = !it.isVisible
            (requireActivity() as MainActivity).lockDrawer(!it.isVisible)
        }.launchIn(lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }
}