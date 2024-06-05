package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.add.ReviewAddAccountViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.add.ReviewAddAccountScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class ReviewAddAccountFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    val args: ReviewAddAccountFragmentArgs by navArgs()

    val viewModel: ReviewAddAccountViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if (sideEffect is SideEffects.AccountCreated){
            // Find if there is a Receive screen in the backstack or a Network overview
            val destinationId = findNavController().currentBackStack.value.let { backQueue ->
                (backQueue.find { it.destination.id == R.id.receiveFragment } ?: backQueue.find { it.destination.id == R.id.walletOverviewFragment })!!.destination.id
            }
            setNavigationResult(
                result = sideEffect.accountAsset, key = SET_ACCOUNT, destinationId = destinationId
            )
        }
    }

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
                    ReviewAddAccountScreen(viewModel = viewModel)
                }
            }
        }
    }

    companion object {
        const val SET_ACCOUNT = "SET_ACCOUNT"
    }
}