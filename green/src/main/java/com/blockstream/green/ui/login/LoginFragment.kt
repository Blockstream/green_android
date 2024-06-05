package com.blockstream.green.ui.login

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.isEmpty
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LoginFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = R.menu.login
) {
    val args: LoginFragmentArgs by navArgs()

    val viewModel: LoginViewModel by viewModel { parametersOf(args.wallet, args.isLightningShortcut, args.autoLoginWallet, args.deviceId) }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val title: String
        get() = viewModel.greenWallet.name

    override val subtitle: String?
        get() = if(args.isLightningShortcut) getString(R.string.id_lightning_account) else null

    override val useCompose: Boolean = true

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        when (sideEffect) {
            is SideEffects.WalletDelete -> {
                NavGraphDirections.actionGlobalHomeFragment().let { directions ->
                    navigate(directions.actionId, directions.arguments, isLogout = true)
                }
            }

            is SideEffects.NavigateTo -> {

                (sideEffect.destination as? NavigateDestinations.WalletOverview)?.also {
                    navigate(LoginFragmentDirections.actionGlobalWalletOverviewFragment(wallet = it.greenWallet))
                }


                (sideEffect.destination as? NavigateDestinations.RecoveryPhrase)?.setupArgs?.credentials?.also {
                    logger.i { "Emergency Recovery Phrase" }
                    navigate(
                        LoginFragmentDirections.actionGlobalRecoveryPhraseFragment(
                            wallet = null,
                            credentials = it
                        )
                    )
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.walletName.onEach {
            updateToolbar()
        }.launchIn(lifecycleScope)

        viewModel.error.onEach {
            invalidateMenu()
        }.launchIn(lifecycleScope)

        viewModel.passwordCredentials.onEach {
            invalidateMenu()
        }

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    LoginScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

        menu.findItem(R.id.help).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware && !viewModel.greenWallet.isWatchOnly && viewModel.pinCredentials.isEmpty() && viewModel.passwordCredentials.isEmpty()
        menu.findItem(R.id.bip39_passphrase).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware && !viewModel.greenWallet.isWatchOnly
        menu.findItem(R.id.rename).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware
        menu.findItem(R.id.delete).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware
        menu.findItem(R.id.show_recovery_phrase).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware && !viewModel.greenWallet.isWatchOnly
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.bip39_passphrase -> {
                viewModel.postEvent(NavigateDestinations.Bip39Passphrase(viewModel.bip39Passphrase.value))
            }
            R.id.delete -> {
                viewModel.postEvent(NavigateDestinations.DeleteWallet(viewModel.greenWallet))
            }
            R.id.rename -> {
                viewModel.postEvent(NavigateDestinations.RenameWallet(viewModel.greenWallet))
            }
            R.id.show_recovery_phrase -> {
                viewModel.postEvent(LoginViewModel.LocalEvents.EmergencyRecovery(true))
            }
            R.id.help -> {
                viewModel.postEvent(LoginViewModel.LocalEvents.ClickHelp)
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    companion object: Loggable()
}

