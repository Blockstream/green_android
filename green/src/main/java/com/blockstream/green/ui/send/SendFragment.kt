package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.send.SendScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SendFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    val args: SendFragmentArgs by navArgs()

    val viewModel: com.blockstream.common.models.send.SendViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.address,
            args.addressType
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        when (sideEffect) {
            is SideEffects.TransactionSent -> {
                val sendTransactionSuccess = sideEffect.data

                val message = sendTransactionSuccess.message ?: ""
                val isUrl = sendTransactionSuccess.url.isNotBlank()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_success)
                    .setMessage(getString(R.string.id_message_from_recipient_s, message))
                    .setPositiveButton(if (isUrl) R.string.id_open else android.R.string.ok) { _, _ ->
                        if (isUrl) {
                            openBrowser(sendTransactionSuccess.url ?: "")
                        }
                        navigateToRoot()
                    }.apply {
                        if (isUrl) {
                            setNegativeButton(android.R.string.cancel) { _, _ ->
                                navigateToRoot()
                            }
                        }
                    }
                    .show()
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
                    SendScreen(viewModel = viewModel)
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

    private fun navigateToRoot(){
        findNavController().popBackStack(R.id.walletOverviewFragment, false)
    }
}