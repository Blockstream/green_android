package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.AddWalletViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.AddWalletFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.EnvironmentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.EnvironmentListener
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddWalletFragment :
    AppFragment<AddWalletFragmentBinding>(R.layout.add_wallet_fragment, menuRes = 0),
    EnvironmentListener
{
    private var _pendingSideEffect: AddWalletViewModel.LocalSideEffects.SelectEnvironment? = null

    val viewModel: AddWalletViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel() = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonNewWallet.setOnClickListener {
            viewModel.postEvent(AddWalletViewModel.LocalEvents.NewWallet)
        }

        binding.buttonRestoreWallet.setOnClickListener {
            viewModel.postEvent(AddWalletViewModel.LocalEvents.RestoreWallet)
        }

        binding.buttonWatchOnly.setOnClickListener {
            viewModel.postEvent(AddWalletViewModel.LocalEvents.WatchOnly)
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        (sideEffect as? AddWalletViewModel.LocalSideEffects.SelectEnvironment)?.also {
            _pendingSideEffect = it
            EnvironmentBottomSheetDialogFragment.show(withCustomNetwork = true, fragmentManager = childFragmentManager)
        }

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

            (to.destination as? NavigateDestinations.NewWatchOnlyWallet)?.also {
                navigate(AddWalletFragmentDirections.actionAddWalletFragmentToWatchOnlyCredentialsFragment())
            }
        }
    }

    override fun onEnvironmentSelected(isTestnet: Boolean?, customNetwork: Network?) {
        if (isTestnet != null) {
            _pendingSideEffect?.also {
                viewModel.postEvent(
                    AddWalletViewModel.LocalEvents.SelectEnviroment(
                        isTestnet = isTestnet,
                        customNetwork = customNetwork
                    )
                )
            }
        }
    }
}