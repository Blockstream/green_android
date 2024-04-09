package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
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

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        (sideEffect as? SideEffects.NavigateTo)?.also { to ->
            (to.destination as? NavigateDestinations.RecoveryIntro)?.also {
                navigate(AddWalletFragmentDirections.actionAddWalletFragmentToRecoveryIntroFragment(
                    setupArgs = it.args
                ))
            }

            (to.destination as? NavigateDestinations.EnterRecoveryPhrase)?.also {
                navigate(AddWalletFragmentDirections.actionAddWalletFragmentToEnterRecoveryPhraseFragment(
                    setupArgs = it.args
                ))
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
                    AddWalletScreen(viewModel = viewModel)
                }
            }
        }
    }
}