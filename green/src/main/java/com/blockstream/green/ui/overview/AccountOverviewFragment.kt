package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.needs2faActivation
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.overview.AccountOverviewScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class AccountOverviewFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = R.menu.account_overview
) {
    val args: AccountOverviewFragmentArgs by navArgs()

    val viewModel: AccountOverviewViewModel by viewModel {
        parametersOf(args.wallet, args.accountAsset)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.NavigateToRoot) {
            findNavController().popBackStack(R.id.walletOverviewFragment, false)
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
                    AccountOverviewScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        // Prevent from archiving all your accounts
        menu.findItem(R.id.archive).isVisible =
            !viewModel.session.isWatchOnly && viewModel.accounts.value.size > 1 && !viewModel.account.isLightning
        menu.findItem(R.id.help).isVisible = viewModel.account.isAmp
        menu.findItem(R.id.enhance_security).isVisible =
            !viewModel.session.isWatchOnly && viewModel.account.needs2faActivation(viewModel.session)
        menu.findItem(R.id.rename).isVisible =
            !viewModel.session.isWatchOnly && !viewModel.account.isLightning
        menu.findItem(R.id.node_info).isVisible = viewModel.account.isLightning
        menu.findItem(R.id.remove).isVisible = viewModel.account.isLightning && !viewModel.greenWallet.isLightning

        menu.findItem(R.id.lightning_shortcut).also {
            // Only allow the removal of LN Shortcut on Wallet
            it.isVisible =
                viewModel.account.isLightning && !viewModel.greenWallet.isLightning && (viewModel.hasLightningShortcut.value == true || !viewModel.greenWallet.isHardware)
            it.title =
                getString(if (viewModel.hasLightningShortcut.value == true) R.string.id_remove_lightning_shortcut else R.string.id_add_lightning_shortcut)
            it.icon = ContextCompat.getDrawable(
                requireContext(),
                if (viewModel.hasLightningShortcut.value == true) R.drawable.ic_lightning_slash else R.drawable.ic_lightning
            )
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.help -> {
                openBrowser(Urls.HELP_AMP_ASSETS)
                true
            }

            R.id.rename -> {
                viewModel.postEvent(NavigateDestinations.RenameAccount(viewModel.account))
                true
            }

            R.id.archive -> {
                viewModel.postEvent(Events.ArchiveAccount(viewModel.account))
                true
            }

            R.id.enhance_security -> {
                navigate(
                    AccountOverviewFragmentDirections.actionGlobalTwoFractorAuthenticationFragment(
                        wallet = viewModel.greenWallet,
                        network = viewModel.account.network
                    )
                )
                true
            }

            R.id.node_info -> {
                viewModel.postEvent(NavigateDestinations.LightningNode)
                true
            }

            R.id.lightning_shortcut -> {
                if (viewModel.hasLightningShortcut.value == true) {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.RemoveLightningShortcut)
                } else {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.EnableLightningShortcut)
                }
                true
            }

            R.id.remove -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_remove)
                    .setMessage(R.string.id_are_you_sure_you_want_to_remove)
                    .setPositiveButton(R.string.id_remove) { _, _ ->
                        viewModel.postEvent(Events.RemoveAccount(viewModel.account))
                        snackbar(R.string.id_account_has_been_removed)
                    }
                    .setNegativeButton(R.string.id_cancel, null)
                    .show()

                true
            }

            else -> super.onMenuItemSelected(menuItem)
        }
    }
}
