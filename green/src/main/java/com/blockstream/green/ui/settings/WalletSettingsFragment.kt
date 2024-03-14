package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.SettingsNotification
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.green.BuildConfig
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.extensions.authenticateWithBiometrics
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.showChoiceDialog
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.add.ChooseAccountTypeFragmentDirections
import com.blockstream.green.ui.dialogs.DenominationExchangeRateDialogFragment
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.colorText
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.openBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.security.InvalidAlgorithmParameterException


class WalletSettingsFragment : AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()

    val viewModel: WalletSettingsViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.network,
            if (args.showRecoveryTransactions) WalletSettingsSection.RecoveryTransactions else WalletSettingsSection.General
        )
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override val sideEffectsHandledByAppFragment: Boolean = false

    override val title: String?
        get() = if (args.showRecoveryTransactions) getString(R.string.id_recovery_transactions) else if(args.network != null) getString(R.string.id_settings) else null

    override val subtitle: String?
        get() = if(args.network != null) getString(R.string.id_multisig) else null

    override val toolbarIcon: Int?
        get() = args.network?.getNetworkIcon()

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is SideEffects.NavigateTo -> {
                (sideEffect.destination as? NavigateDestinations.RecoveryIntro)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToRecoveryIntroFragment(
                            setupArgs = it.args
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.TwoFactorAuthentication)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFractorAuthenticationFragment(
                            wallet = it.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.TwoFactorSetup)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFactorSetupFragment(
                            wallet = it.greenWallet,
                            method = it.method,
                            action = it.action,
                            network = it.network
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ArchivedAccounts)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionGlobalArchivedAccountsFragment(
                            wallet = it.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ChangePin)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToChangePinFragment(
                            wallet = it.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.WatchOnly)?.also {
                    navigate(
                        WalletSettingsFragmentDirections.actionWalletSettingsFragmentToWatchOnlyFragment(
                            wallet = it.greenWallet
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
