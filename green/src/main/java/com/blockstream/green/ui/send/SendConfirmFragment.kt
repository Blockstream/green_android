package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.SendConfirmViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.send.SendConfirmScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.utils.isDevelopmentOrDebug
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SendConfirmFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = R.menu.send_confirm
) {
    val args: SendConfirmFragmentArgs by navArgs()

    val viewModel: SendConfirmViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.accountAsset,
            args.denomination
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
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
                    SendConfirmScreen(viewModel = viewModel)
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

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.sign_transaction).isVisible = isDevelopmentOrDebug
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_note -> {
                viewModel.postEvent(SendConfirmViewModel.LocalEvents.Note)
                return true
            }
            R.id.sign_transaction -> {
                viewModel.postEvent(
                    CreateTransactionViewModelAbstract.LocalEvents.SignTransaction(
                        broadcastTransaction = false
                    )
                )
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    companion object : Loggable()
}