package com.blockstream.green.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.base.Urls
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.CustomTitleDialogBinding
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.databinding.SettingsLimitsDialogBinding
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.localized2faMethods
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.AmountTextWatcher
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.UserInput
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class NetworkTwoFactorAuthenticationFragment :
    AbstractWalletFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: TwoFactorAuthenticationFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "WalletSettings2FA"

    private val itemAdapter = ItemAdapter<GenericItem>()

    private lateinit var emailPreference: PreferenceListItem
    private lateinit var smsPreference: PreferenceListItem
    private lateinit var callPreference: PreferenceListItem
    private lateinit var toptPreference: PreferenceListItem
    private lateinit var telegramPreference: PreferenceListItem
    private lateinit var thresholdPreference: PreferenceListItem

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

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by viewModels {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        getNavigationResult<Boolean>(key = network.id)?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.updateTwoFactorConfig(network)
                clearNavigationResult(key = network.id)
            }
        }

        binding.vm = viewModel

        emailPreference = PreferenceListItem(StringHolder(R.string.id_email), withSwitch = true)
        smsPreference = PreferenceListItem(StringHolder(R.string.id_sms), withSwitch = true)
        callPreference = PreferenceListItem(StringHolder(R.string.id_call), withSwitch = true)
        toptPreference = PreferenceListItem(StringHolder(R.string.id_authenticator_app), withSwitch = true)
        telegramPreference = PreferenceListItem(StringHolder(R.string.id_telegram), withSwitch = true)

        thresholdPreference = PreferenceListItem(StringHolder(R.string.id_2fa_threshold))

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, item: GenericItem, position: Int ->
                viewModel.networkTwoFactorConfigLiveData(network).value?.let {
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

                        else -> {
                            if (csvBucketPreferences.contains(item)) {
                                csvBucketPreferences.forEach { pref ->
                                    pref.radioChecked = pref == item
                                    fastAdapter.notifyItemChanged(fastAdapter.getPosition(pref))
                                }

                                val selectedIndex = csvBucketPreferences.indexOfFirst { it.radioChecked }
                                if (selectedIndex > -1) {
                                    val csvTime = network.csvBuckets[selectedIndex]
                                    viewModel.setCsvTime(
                                        network,
                                        csvTime,
                                        DialogTwoFactorResolver(requireContext())
                                    )
                                }
                            }
                        }
                    }
                }
                true
            }

        fastAdapter.addClickListener<ListItemHelpBinding, GenericItem>({ binding -> binding.button }) { _, _, _, _ ->
            openBrowser(settingsManager.getApplicationSettings(), Urls.RECOVERY_TOOL)
        }

        binding.recycler.apply {
            itemAnimator = AlphaCrossFadeAnimator()
            adapter = fastAdapter
        }

        viewModel.onError.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
                updateAdapter()
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledForType<GdkEvent.Success>()?.let {
                updateAdapter()
            }
        }

        // Update when both available
        combine(
            viewModel.networkSettingsLiveData(network).asFlow(),
            viewModel.networkTwoFactorConfigLiveData(network).asFlow()
        ) { settings, twoFactorConfig ->
            logger.info { "WTF flow ${network.id} ${twoFactorConfig.enabledMethods}" }
            settings to twoFactorConfig
        }.onEach {
            updateAdapter()
        }.launchIn(lifecycleScope)
    }

    private fun updateAdapter() {
        val settings = viewModel.networkSettings(network)
        val twoFactorConfig = viewModel.networkTwoFactorConfig(network)
        
        if (settings == null || twoFactorConfig == null) {
            if (settings == null) {
                session.updateSettings(network)
            }
            if (twoFactorConfig == null) {
                viewModel.updateTwoFactorConfig(network)
            }
            return
        }

        val list = mutableListOf<GenericItem>()

        list += TitleListItem(StringHolder(R.string.id_2fa_methods))
        

        twoFactorConfig.allMethods.also { methods ->
            if(methods.contains(TwoFactorMethod.EMAIL.gdkType)){
                list += emailPreference.also {
                    it.switchChecked = twoFactorConfig.email.enabled
                    // email has the old value even if disabled
                    it.subtitle = StringHolder(if(twoFactorConfig.email.enabled) twoFactorConfig.email.data.ifBlank { null } else null)
                }
            }
            if(methods.contains(TwoFactorMethod.SMS.gdkType)) {
                list += smsPreference.also {
                    it.switchChecked = twoFactorConfig.sms.enabled
                    it.subtitle = StringHolder(twoFactorConfig.sms.data.ifBlank { null })
                    // it.withButton = twoFactorConfig.sms.enabled
                }
            }
            if(methods.contains(TwoFactorMethod.PHONE.gdkType)) {
                list += callPreference.also {
                    it.switchChecked = twoFactorConfig.phone.enabled
                    it.subtitle = StringHolder(twoFactorConfig.phone.data.ifBlank { null })
                }
            }
            if(methods.contains(TwoFactorMethod.AUTHENTICATOR.gdkType)) {
                list += toptPreference.also {
                    it.switchChecked = twoFactorConfig.gauth.enabled
                    it.subtitle =
                        StringHolder(if (twoFactorConfig.gauth.enabled) getString(R.string.id_enabled) else null)
                }
            }
            if(methods.contains(TwoFactorMethod.TELEGRAM.gdkType)) {
                list += telegramPreference.also {
                    it.switchChecked = twoFactorConfig.telegram.enabled
                    it.subtitle =
                        StringHolder(if (twoFactorConfig.telegram.enabled) getString(R.string.id_enabled) else null)
                }
            }
        }

        if(!network.isLiquid){
            list += TitleListItem(StringHolder(R.string.id_2fa_threshold))
            list += HelpListItem(message = StringHolder(R.string.id_spend_your_bitcoin_without_2fa))

            list += thresholdPreference.also {
                it.subtitle = StringHolder(twoFactorConfig.limits.let { limits ->
                    if (!limits.isFiat && limits.satoshi == 0L) {
                        getString(R.string.id_set_twofactor_threshold)
                    } else {
                        limits.toAmountLook(
                            session = session,
                            isFiat = limits.isFiat
                        )
                    }
                })
            }
        }

        list += TitleListItem(StringHolder(R.string.id_2fa_expiry))

        if (!network.isLiquid) {
            list += HelpListItem(message = StringHolder(R.string.id_customize_2fa_expiration_of))
            list += csvBucketPreferences

            val selectedIndex = network.csvBuckets.indexOf(settings.csvTime)

            csvBucketPreferences.forEachIndexed { index, radioPreference ->
                radioPreference.radioChecked = index == selectedIndex
            }
        }

        list += HelpListItem(
            message = StringHolder(R.string.id_your_2fa_expires_so_that_if_you),
            button = StringHolder(R.string.id_recovery_tool)
        )

        itemAdapter.set(list)
        // Redraw all cells as we use checkboxes and radio buttons
        itemAdapter.fastAdapter?.notifyAdapterDataSetChanged()
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    private fun handleTwoFactorThreshold() {
        val binding = SettingsLimitsDialogBinding.inflate(requireActivity().layoutInflater)
        binding.amountInputLayout.endIconCustomMode()

        // Warning, don't change the order of fiat and btc,
        val currencies = listOf(getBitcoinOrLiquidUnit(network, session), getFiatCurrency(network, session))
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = adapter

        viewModel.networkTwoFactorConfig(network)?.limits?.let { limits ->
            // Deprecate setting fiat value if not already setup
            if(limits.isFiat){
                binding.showFiat = true
            }else{
                binding.currency = getBitcoinOrLiquidUnit(assetId = network.policyAsset, session = session)
            }
            binding.currencySpinner.setSelection(if (limits.isFiat) 1 else 0)
            binding.amount = limits.toAmountLook(
                session = session,
                isFiat = limits.isFiat,
                withUnit = false
            )
        }

        AmountTextWatcher.watch(binding.amountEditText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_set_twofactor_threshold)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val isFiat = binding.currencySpinner.selectedItemPosition == 1
                    val input = UserInput.parseUserInput(session, binding.amount, isFiat = isFiat)

                    viewModel.setLimits(network, input.toLimit(), DialogTwoFactorResolver(requireContext()))
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
                wallet = wallet,
                method = method,
                action = TwoFactorSetupAction.SETUP,
                network = network
            )
        )
    }

    private fun disable2FA(method: TwoFactorMethod) {
        viewModel.networkTwoFactorConfigLiveData(network).value?.let {
            val binding = CustomTitleDialogBinding.inflate(requireActivity().layoutInflater)

            binding.title = getString(R.string.id_security_change)
            binding.message =
                getString(if (it.enabledMethods.size == 1) R.string.id_confirm_via_2fa_that_you else R.string.id_another_2fa_method_is_already)

            val methods = if(it.enabledMethods.size > 1) it.enabledMethods.filter { it1 -> it1 != method.gdkType } else it.enabledMethods

            MaterialAlertDialogBuilder(requireContext())
                .setCustomTitle(binding.root)
                .setSingleChoiceItems(requireContext().localized2faMethods(methods).toTypedArray(), -1) { dialog, i: Int ->
                    viewModel.disable2FA(network,
                        method,
                        DialogTwoFactorResolver(requireContext(), selectedMethod = methods[i])
                    )
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.id_i_lost_my_2fa) { _: DialogInterface, _: Int ->
                    navigate(
                        TwoFactorAuthenticationFragmentDirections.actionTwoFractorAuthenticationFragmentToTwoFactorSetupFragment(
                            wallet = wallet,
                            action = TwoFactorSetupAction.RESET,
                            network = network
                        )
                    )
                }
                .show()
        }
    }
}
