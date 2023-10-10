package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.WatchOnlyPolicyViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.WatchOnlyPolicyFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import org.koin.androidx.viewmodel.ext.android.viewModel

class WatchOnlyPolicyFragment : AppFragment<WatchOnlyPolicyFragmentBinding>(
    R.layout.watch_only_policy_fragment,
    menuRes = 0
) {
    override val screenName = "OnBoardWatchOnlyChooseSecurity"

    val viewModel: WatchOnlyPolicyViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonMultisig.setOnClickListener {
            viewModel.postEvent(WatchOnlyPolicyViewModel.LocalEvents.SelectPolicy(isMultisig = true))
        }

        binding.buttonSinglesig.setOnClickListener {
            viewModel.postEvent(WatchOnlyPolicyViewModel.LocalEvents.SelectPolicy(isMultisig = false))
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.NavigateTo){

            (sideEffect.destination as? WatchOnlyPolicyViewModel.Destination.ChooseNetwork)?.also {
                navigate(
                    WatchOnlyPolicyFragmentDirections.actionWatchOnlyPolicyFragmentToChooseNetworkFragment(
                        it.setupArgs
                    )
                )
            }

            (sideEffect.destination as? WatchOnlyPolicyViewModel.Destination.Singlesig)?.also {
                navigate(
                    WatchOnlyPolicyFragmentDirections.actionWatchOnlyPolicyFragmentToWatchOnlyCredentialsFragment(
                        it.setupArgs
                    )
                )
            }
        }
    }
}