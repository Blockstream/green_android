package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.add.ChooseAccountTypeViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.add.ChooseAccountTypeScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.jade.JadeQRFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChooseAccountTypeFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: ChooseAccountTypeFragmentArgs by navArgs()

    val viewModel: ChooseAccountTypeViewModel by viewModel {
        parametersOf(args.wallet, args.asset?.let { AssetBalance(it) }, args.isReceive)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if (sideEffect is SideEffects.AccountCreated){
            // Find if there is a Receive screen in the backstack or a Network overview
            val destinationId = findNavController().currentBackStack.value.let { backQueue ->
                (backQueue.find { it.destination.id == R.id.receiveFragment } ?: backQueue.find { it.destination.id == R.id.walletOverviewFragment })!!.destination.id
            }
            setNavigationResult(
                result = sideEffect.accountAsset, key = ReviewAddAccountFragment.SET_ACCOUNT, destinationId = destinationId
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<String>(JadeQRFragment.MNEMONIC_RESULT)?.observe(viewLifecycleOwner) { mnemonic ->
            if (mnemonic != null) {
                clearNavigationResult(JadeQRFragment.MNEMONIC_RESULT)
                viewModel.postEvent(
                    ChooseAccountTypeViewModel.LocalEvents.CreateLightningAccount(
                        mnemonic
                    )
                )
            }
        }

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    ChooseAccountTypeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
