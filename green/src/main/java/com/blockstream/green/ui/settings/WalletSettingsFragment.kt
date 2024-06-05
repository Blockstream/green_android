package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class WalletSettingsFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()

    val viewModel: WalletSettingsViewModel by viewModel {
        parametersOf(
            args.wallet,
            if (args.showRecoveryTransactions) WalletSettingsSection.RecoveryTransactions else WalletSettingsSection.General,
            args.network
        )
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override val title: String?
        get() = if (args.showRecoveryTransactions) getString(R.string.id_recovery_transactions) else if(args.network != null) getString(R.string.id_settings) else null

    override val subtitle: String?
        get() = if(args.network != null) getString(R.string.id_multisig) else null

    override val toolbarIcon: Int?
        get() = args.network?.getNetworkIcon()

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is SideEffects.NavigateTo -> {
                (sideEffect.destination as? NavigateDestinations.ChangePin)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToChangePinFragment(
                            wallet = viewModel.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.WatchOnly)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToWatchOnlyFragment(
                            wallet = viewModel.greenWallet
                        )
                    )
                }

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
                    WalletSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
