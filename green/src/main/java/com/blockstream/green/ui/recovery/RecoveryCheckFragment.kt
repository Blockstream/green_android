package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryCheckFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    private val args: RecoveryCheckFragmentArgs by navArgs()

    override val useCompose: Boolean = true

    private val viewModel: RecoveryCheckViewModel by viewModel {
        parametersOf(args.args)
    }

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getGreenViewModel(): GreenViewModel = viewModel

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if (sideEffect is SideEffects.NavigateTo) {
            if (sideEffect.destination is NavigateDestinations.SetPin) {
                val recoveryArgs = (sideEffect.destination as NavigateDestinations.SetPin).setupArgs
                navigate(
                    RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentToPinFragment(
                        setupArgs = recoveryArgs
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, false)
                    }
                )
            } else if (sideEffect.destination is NavigateDestinations.AddAccount) {
                val recoveryArgs = (sideEffect.destination as NavigateDestinations.AddAccount).setupArgs
                navigate(
                    RecoveryCheckFragmentDirections.actionGlobalReviewAddAccountFragment(
                        setupArgs = recoveryArgs
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, false)
                    }
                )
            }
        } else if (sideEffect is SideEffects.NavigateBack) {
            snackbar(R.string.id_wrong_choice_check_your)
            findNavController().popBackStack(R.id.recoveryIntroFragment, false)
        } else {
            super.handleSideEffect(sideEffect)
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
                    RecoveryCheckScreen(viewModel = viewModel)
                }
            }
        }
    }
}