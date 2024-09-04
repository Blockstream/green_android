package com.blockstream.green.ui.receive

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.receive.ReceiveScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.utils.openBrowser
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class ReceiveFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view, menuRes = R.menu.menu_receive
) {
    val args: ReceiveFragmentArgs by navArgs()

    override val isAdjustResize: Boolean
        get() = true

    val viewModel: ReceiveViewModel by viewModel {
        parametersOf(args.accountAsset, args.wallet)
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

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
                    ReceiveScreen(viewModel = viewModel)
                }
            }
        }

        viewModel.navData.onEach {
            setToolbarVisibility(it.isVisible)
            onBackCallback.isEnabled = !it.isVisible
            (requireActivity() as MainActivity).lockDrawer(!it.isVisible)
        }.launchIn(lifecycleScope)

        viewModel.onProgress.onEach {
            // On HWWallet Block going back until address is generated
            onBackCallback.isEnabled = viewModel.session.isHardwareWallet && it
            invalidateMenu()
        }.launchIn(lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.add_description).isVisible = viewModel.account.isLightning && !viewModel.showLightningOnChainAddress.value && viewModel.receiveAddress.value == null
        menu.findItem(R.id.add_description).isEnabled = !viewModel.onProgress.value

        (viewModel.receiveAddress.value.isNotBlank() && viewModel.accountAsset.value?.account?.isLightning == false).also {
            menu.findItem(R.id.request_amount).isVisible = it
            menu.findItem(R.id.list_of_addresses).isVisible = it
            menu.findItem(R.id.sweep_from_paper_wallet).isVisible = it
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.help -> {
                openBrowser(if (viewModel.account.isAmp) Urls.HELP_AMP_ASSETS else Urls.HELP_RECEIVE_ASSETS)
            }
            R.id.add_description -> {
                viewModel.postEvent(NavigateDestinations.Note(note = viewModel.note.value ?: "", noteType = NoteType.Description))
            }
            R.id.request_amount -> {
                viewModel.postEvent(ReceiveViewModel.LocalEvents.ShowRequestAmount)
            }
            R.id.list_of_addresses -> {
                viewModel.accountAsset.value?.also {
                    viewModel.postEvent(NavigateDestinations.Addresses(accountAsset = it))
                }
            }
            R.id.sweep_from_paper_wallet -> {
                viewModel.accountAsset.value?.also {
                    viewModel.postEvent(NavigateDestinations.Sweep(accountAsset = it))
                }
            }

        }
        return super.onMenuItemSelected(menuItem)
    }
}