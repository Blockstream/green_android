package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.arkivanov.essenty.statekeeper.stateKeeper
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.phone.EnterRecoveryPhraseViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.onboarding.phone.EnterRecoveryPhraseScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class EnterRecoveryPhraseFragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view,
    menuRes = R.menu.menu_help
) {

    val args: EnterRecoveryPhraseFragmentArgs by navArgs()

    val viewModel: EnterRecoveryPhraseViewModel by viewModel {
        parametersOf(args.setupArgs, stateKeeper())
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        (sideEffect as? SideEffects.NavigateTo)?.also {
            (it.destination as? NavigateDestinations.SetPin)?.also {
                navigate(
                    EnterRecoveryPhraseFragmentDirections.actionEnterRecoveryPhraseFragmentToPinFragment(
                        setupArgs = it.setupArgs,
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
                    EnterRecoveryPhraseScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.help -> {
                viewModel.postEvent(EnterRecoveryPhraseViewModel.LocalEvents.LaunchHelp)
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }
}