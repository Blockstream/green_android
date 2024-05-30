package com.blockstream.green.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.green.R
import com.blockstream.green.databinding.CustomTitleDialogBinding
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.databinding.SettingsLimitsDialogBinding
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.localized2faMethods
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.TwoFactorResetBottomSheetDialogFragment
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.utils.AmountTextWatcher
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.getFiatCurrency
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.toAmountLook
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NetworkTwoFactorAuthenticationFragment :
    AppFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: TwoFactorAuthenticationFragmentArgs by navArgs()

    private val itemAdapter = ItemAdapter<GenericItem>()

    private lateinit var emailPreference: PreferenceListItem
    private lateinit var smsPreference: PreferenceListItem
    private lateinit var callPreference: PreferenceListItem
    private lateinit var toptPreference: PreferenceListItem
    private lateinit var telegramPreference: PreferenceListItem
    private lateinit var thresholdPreference: PreferenceListItem

    // Recovery Transactions
    private lateinit var recoveryTransactionsPreference: PreferenceListItem

    private val csvBucketPreferences by lazy {
        val titles = resources.getStringArray(R.array.csv_titles)
        val subtitles = resources.getStringArray(R.array.csv_subtitles)

        titles.mapIndexed { index, title ->
            PreferenceListItem(
                StringHolder(title),
                StringHolder(subtitles[index]),
                withRadio = true
            )
        }
    }

    val network: Network by lazy { args.network!! }

    override val subtitle: String
        get() = getString(R.string.id_multisig)

    override val toolbarIcon: Int
        get() = network.getNetworkIcon()

    val viewModel: WalletSettingsViewModel by viewModel {
        parametersOf(args.wallet, args.network, WalletSettingsSection.TwoFactor )
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Success){
            updateAdapter()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<Boolean>(key = network.id)?.observe(viewLifecycleOwner) {
            it?.let {
                clearNavigationResult(key = network.id)
            }
        }

        binding.vm = viewModel

        emailPreference = PreferenceListItem(StringHolder(requireContext(),R.string.id_email), withSwitch = true)
        smsPreference = PreferenceListItem(StringHolder(requireContext(),R.string.id_sms), withSwitch = true)
        callPreference = PreferenceListItem(StringHolder(requireContext(),R.string.id_call), withSwitch = true)
        toptPreference = PreferenceListItem(StringHolder(requireContext(),R.string.id_authenticator_app), withSwitch = true)
        telegramPreference = PreferenceListItem(StringHolder(requireContext(),R.string.id_telegram), withSwitch = true)

        thresholdPreference = PreferenceListItem(StringHolder(requireContext(),R.string.id_2fa_threshold))

        recoveryTransactionsPreference = PreferenceListItem(
            StringHolder(requireContext(),R.string.id_recovery_transactions),
            StringHolder(requireContext(),R.string.id_legacy_script_coins),
            isInnerMenu = true
        )

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, item: GenericItem, _: Int ->
                viewModel.session.twoFactorConfig(network).value?.let {
                    when (item) {
                        emailPreference -> {
                            if (it.email.enabled) {
                                disable2FA(TwoFactorMethod.EMAIL)
                            } else {
                                enable2FA(TwoFactorMethod.EMAIL)
                            }
                        }

                        smsPreference -> {
                            if (it.sms.enabled) {
                                disable2FA(TwoFactorMethod.SMS)
                            } else {
                                enable2FA(TwoFactorMethod.SMS)
                            }
                        }

                        callPreference -> {
                            if (it.phone.enabled) {
                                disable2FA(TwoFactorMethod.PHONE)
                            } else {
                                enable2FA(TwoFactorMethod.PHONE)
                            }
                        }

                        telegramPreference -> {
                            if (it.telegram.enabled) {
                                disable2FA(TwoFactorMethod.TELEGRAM)
                            } else {
                                enable2FA(TwoFactorMethod.TELEGRAM)
                            }
                        }

                        toptPreference -> {
                            if (it.gauth.enabled) {
                                disable2FA(TwoFactorMethod.AUTHENTICATOR)
                            } else {
                                enable2FA(TwoFactorMethod.AUTHENTICATOR)
                            }
                        }

                        thresholdPreference -> {
                            handleTwoFactorThreshold()
                        }

                        recoveryTransactionsPreference -> {
                            navigate(
                                    TwoFactorAuthenticationFragmentDirections.actionTwoFractorAuthenticationFragmentToWalletSettingsFragment(
                                    wallet = viewModel.greenWallet,
                                    showRecoveryTransactions = true,
                                    network = viewModel.session.bitcoinMultisig!!
                                )
                            )
                        }

                        else -> {
                            if (csvBucketPreferences.contains(item)) {


                                csvBucketPreferences.indexOf(item).takeIf { index -> index != -1 }
                                    ?.let { index -> network.csvBuckets.getOrNull(index) }
                                    ?.also { csvTime ->
                                        viewModel.postEvent(
                                            WalletSettingsViewModel.LocalEvents.SetCsvTime(
                                                csvTime,
                                                DialogTwoFactorResolver(this)
                                            )
                                        )
                                    }
                            }
                        }
                    }
                }
                true
            }

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.button }) { _, _, _, _ ->
            // Recovery tool
            if(viewModel.session.walletExistsAndIsUnlocked(network)){
                openBrowser(settingsManager.getApplicationSettings(), Urls.RECOVERY_TOOL)
            }else{
                // 2FA Reset
                viewModel.session.getTwoFactorReset(network)?.also {
                    TwoFactorResetBottomSheetDialogFragment.show(
                        network,
                        it,
                        childFragmentManager
                    )
                }
            }
        }

        binding.recycler.apply {
            itemAnimator = AlphaCrossFadeAnimator()
            adapter = fastAdapter
        }

        // Update when both available
        combine(
            viewModel.session.settings(network),
            viewModel.session.twoFactorConfig(network)
        ) { settings, twoFactorConfig ->
            settings to twoFactorConfig
        }.onEach {
            updateAdapter()
        }.launchIn(lifecycleScope)
    }

    private fun updateAdapter() {
        val settings = viewModel.session.settings(network).value
        val twoFactorConfig = viewModel.session.twoFactorConfig(network).value

        if (settings == null || twoFactorConfig == null) {
            if (settings == null) {
                viewModel.session.updateSettings(network)
            }
            return
        }


        val list = mutableListOf<GenericItem>()
        if(viewModel.session.walletExistsAndIsUnlocked(network)) {
            list += TitleListItem(StringHolder(requireContext(),R.string.id_2fa_methods))

            twoFactorConfig.allMethods.also { methods ->
                if (methods.contains(TwoFactorMethod.EMAIL.gdkType)) {
                    list += emailPreference.also {
                        it.switchChecked = twoFactorConfig.email.enabled
                        // email has the old value even if disabled
                        it.subtitle =
                            StringHolder(if (twoFactorConfig.email.enabled) twoFactorConfig.email.data.ifBlank { null } else null)
                    }
                }
                if (methods.contains(TwoFactorMethod.SMS.gdkType)) {
                    list += smsPreference.also {
                        it.switchChecked = twoFactorConfig.sms.enabled
                        it.subtitle = StringHolder(twoFactorConfig.sms.data.ifBlank { null })
                        // it.withButton = twoFactorConfig.sms.enabled
                    }
                }
                if (methods.contains(TwoFactorMethod.PHONE.gdkType)) {
                    list += callPreference.also {
                        it.switchChecked = twoFactorConfig.phone.enabled
                        it.subtitle = StringHolder(twoFactorConfig.phone.data.ifBlank { null })
                    }
                }
                if (methods.contains(TwoFactorMethod.AUTHENTICATOR.gdkType)) {
                    list += toptPreference.also {
                        it.switchChecked = twoFactorConfig.gauth.enabled
                        it.subtitle =
                            StringHolder(if (twoFactorConfig.gauth.enabled) getString(R.string.id_enabled) else null)
                    }
                }
                if (methods.contains(TwoFactorMethod.TELEGRAM.gdkType)) {
                    list += telegramPreference.also {
                        it.switchChecked = twoFactorConfig.telegram.enabled
                        it.subtitle =
                            StringHolder(if (twoFactorConfig.telegram.enabled) getString(R.string.id_enabled) else null)
                    }
                }
            }

            if (!network.isLiquid) {
                list += TitleListItem(StringHolder(requireContext(),R.string.id_2fa_threshold))
                list += ActionListItem(message = StringHolder(requireContext(),R.string.id_spend_your_bitcoin_without_2fa))

                list += thresholdPreference.also {
                    it.subtitle = StringHolder(twoFactorConfig.limits.let { limits ->
                        if (!limits.isFiat && limits.satoshi == 0L) {
                            getString(R.string.id_set_twofactor_threshold)
                        } else {
                            if (limits.isFiat) {
                                // GDK 0.0.58.post1 - GA_get_twofactor_config: Fiat pricing limits no longer return corresponding
                                // converted BTC amounts. When "is_fiat" is true, the caller should convert
                                // the amount themselves using GA_convert_amount if desired.
                                limits.fiat
                            } else {
                                limits.toAmountLook(
                                    session = viewModel.session
                                )
                            }
                        }
                    })
                }
            }

            list += TitleListItem(StringHolder(requireContext(),R.string.id_2fa_expiry))

            if (!network.isLiquid) {
                list += ActionListItem(message = StringHolder(requireContext(),R.string.id_customize_2fa_expiration_of))
                list += csvBucketPreferences

                val selectedIndex = network.csvBuckets.indexOf(settings.csvTime)

                csvBucketPreferences.forEachIndexed { index, radioPreference ->
                    radioPreference.radioChecked = index == selectedIndex
                }
            }

            list += ActionListItem(
                message = StringHolder(requireContext(),R.string.id_your_2fa_expires_so_that_if_you),
                button = StringHolder(requireContext(),R.string.id_recovery_tool)
            )

            if(network.isBitcoin && viewModel.session.walletExistsAndIsUnlocked(network)) {
                list += recoveryTransactionsPreference
            }
        }else{
            list += TitleListItem(StringHolder(requireContext(),R.string.id_2fa_reset_in_progress))

            viewModel.session.getTwoFactorReset(network)?.also {
                list += ActionListItem(
                    message = StringHolder(getString(R.string.id_your_wallet_is_locked_for_a, it.daysRemaining)),
                    button = StringHolder(requireContext(),R.string.id_learn_more)
                )
            }
        }

        itemAdapter.set(list)
        // Redraw all cells as we use checkboxes and radio buttons
        itemAdapter.fastAdapter?.notifyAdapterDataSetChanged()
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    private fun handleTwoFactorThreshold() {
        val binding = SettingsLimitsDialogBinding.inflate(requireActivity().layoutInflater)
        binding.amountInputLayout.endIconCustomMode()

        // Warning, don't change the order of fiat and btc,
        val currencies = listOf(getBitcoinOrLiquidUnit(session = viewModel.session, assetId = network.policyAsset), getFiatCurrency(viewModel.session))
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = adapter

        viewModel.session.twoFactorConfig(network).value?.limits?.let { limits ->
            // Deprecate setting fiat value if not already setup
            if(limits.isFiat){
                binding.showFiat = true

                // GDK 0.0.58.post1 - GA_get_twofactor_config: Fiat pricing limits no longer return corresponding
                // converted BTC amounts. When "is_fiat" is true, the caller should convert
                // the amount themselves using GA_convert_amount if desired.
                binding.amount = limits.fiat
            }else{
                binding.currency = getBitcoinOrLiquidUnit(assetId = network.policyAsset, session = viewModel.session)
                binding.amount = limits.toAmountLook(
                    session = viewModel.session,
                    withUnit = false
                )
            }
            binding.currencySpinner.setSelection(if (limits.isFiat) 1 else 0)
        }

        AmountTextWatcher.watch(binding.amountEditText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_set_twofactor_threshold)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val isFiat = binding.currencySpinner.selectedItemPosition == 1
                    val input = UserInput.parseUserInputSafe(
                        session = viewModel.session,
                        input = binding.amount.takeIf { it.isNotBlank() } ?: "0",
                        denomination = Denomination.fiatOrNull(viewModel.session, isFiat))

                    viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SetLimits(input.toLimit(), DialogTwoFactorResolver(this)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun enable2FA(method: TwoFactorMethod) {
        navigate(
            TwoFactorAuthenticationFragmentDirections.actionTwoFractorAuthenticationFragmentToTwoFactorSetupFragment(
                wallet = viewModel.greenWallet,
                method = method,
                action = TwoFactorSetupAction.SETUP,
                network = network
            )
        )
    }

    private fun disable2FA(method: TwoFactorMethod) {
        viewModel.session.twoFactorConfig(network).value?.let {
            val binding = CustomTitleDialogBinding.inflate(requireActivity().layoutInflater)

            binding.title = getString(R.string.id_security_change)
            binding.message =
                getString(if (it.enabledMethods.size == 1) R.string.id_confirm_via_2fa_that_you else R.string.id_another_2fa_method_is_already)

            val methods = if(it.enabledMethods.size > 1) it.enabledMethods.filter { it1 -> it1 != method.gdkType } else it.enabledMethods

            MaterialAlertDialogBuilder(requireContext())
                .setCustomTitle(binding.root)
                .setSingleChoiceItems(requireContext().localized2faMethods(methods).toTypedArray(), -1) { dialog, i: Int ->
                    viewModel.postEvent(WalletSettingsViewModel.LocalEvents.Disable2FA(method, DialogTwoFactorResolver(this, selectedMethod = methods[i])))
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .apply {
                    if(network.isBitcoin) {
                        setNeutralButton(R.string.id_i_lost_my_2fa) { _: DialogInterface, _: Int ->
                            navigate(
                                TwoFactorAuthenticationFragmentDirections.actionTwoFractorAuthenticationFragmentToTwoFactorSetupFragment(
                                    wallet = viewModel.greenWallet,
                                    action = TwoFactorSetupAction.RESET,
                                    network = network
                                )
                            )
                        }
                    }
                }

                .show()
        }
    }
}
