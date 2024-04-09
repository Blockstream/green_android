package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.arkivanov.essenty.statekeeper.stateKeeper
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryIntroFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
) {
    private val args: RecoveryIntroFragmentArgs by navArgs()

    private val viewModel : RecoveryIntroViewModel by viewModel {
        parametersOf(args.setupArgs, stateKeeper())
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.NavigateTo){
            (sideEffect.destination as? NavigateDestinations.RecoveryWords)?.also {
                navigate(
                    RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryWordsFragment(
                        args = it.args
                    )
                )
            }

            (sideEffect.destination as? NavigateDestinations.RecoveryPhrase)?.also {
                navigate(
                    RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryPhraseFragment(
                        wallet = it.args.greenWallet,
                        isLightning = it.args.isLightning
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, true)
                    }
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
                    RecoveryIntroScreen(viewModel = viewModel)
                }
            }
        }
    }
}
