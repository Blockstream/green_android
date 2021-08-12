package com.blockstream.green.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.TwoFactorConfig
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.*
import com.blockstream.green.lifecycle.MergeLiveData
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TwoFactorAuthenticationFragment :
    WalletFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

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

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by viewModels {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<Boolean>()?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.updateTwoFactorConfig()
                clearNavigationResult()
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
            { _: View?, _: IAdapter<GenericItem>, item: GenericItem, _: Int ->
                viewModel.twoFactorConfigLiveData.value?.let {
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
                                csvBucketPreferences.forEach { it -> it.radioChecked = false }
                                (item as PreferenceListItem).radioChecked = true
                                notifyDataSetChanged()

                                val selectedIndex =
                                    csvBucketPreferences.indexOfFirst { it.radioChecked }
                                if (selectedIndex > -1) {
                                    val csvTime = session.network.csvBuckets[selectedIndex]
                                    viewModel.setCsvTime(
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

        fastAdapter.addClickListener<ListItemHelpBinding, GenericItem>({ binding -> binding.button }) { _, _, _, item ->
            openBrowser(settingsManager.getApplicationSettings(), Urls.RECOVERY_TOOL)
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        viewModel.onError.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
                updateAdapter()
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                updateAdapter()
            }
        }

        // Update when both available
        MergeLiveData(
            viewModel.settingsLiveData,
            viewModel.twoFactorConfigLiveData
        ) { settings: Settings, twoFactorConfig: TwoFactorConfig ->
            settings to twoFactorConfig
        }.observe(viewLifecycleOwner) {
            updateAdapter()
        }
    }

    private fun updateAdapter() {
        val settings = viewModel.settingsLiveData.value!!
        val twoFactorConfig = viewModel.twoFactorConfigLiveData.value!!

        val list = mutableListOf<GenericItem>()

        list += HelpListItem(
            StringHolder(R.string.id_enable_twofactor_authentication),
            StringHolder(R.string.id_tip_we_recommend_you_enable)
        )

        list += TitleListItem(StringHolder(R.string.id_2fa_methods))

        viewModel.twoFactorConfigLiveData.value?.allMethods?.let { methods ->
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


        if(!session.isLiquid){
            list += TitleListItem(StringHolder(R.string.id_2fa_threshold))

            list += HelpListItem(message = StringHolder(R.string.id_spend_your_bitcoin_without_2fa))

            list += thresholdPreference.also {
                it.subtitle = StringHolder(twoFactorConfig.limits.let { limits ->
                    if (!limits.isFiat && limits.satoshi == 0L) {
                        getString(R.string.id_set_twofactor_threshold)
                    } else if (limits.isFiat) {
                        limits.fiat()
                    } else {
                        limits.btc(session)
                    }
                })
            }
        }

        list += TitleListItem(StringHolder(R.string.id_2fa_expiry))

        if (!session.isLiquid) {

            list += HelpListItem(message = StringHolder(R.string.id_customize_2fa_expiration_of))

            list += csvBucketPreferences

            val selectedIndex = session.network.csvBuckets.indexOf(settings.csvTime)

            csvBucketPreferences.forEachIndexed { index, radioPreference ->
                radioPreference.radioChecked = index == selectedIndex
            }
        }

        list += HelpListItem(
            message = StringHolder(R.string.id_your_2fa_expires_so_that_if_you),
            button = StringHolder(R.string.id_recovery_tool)
        )

        FastAdapterDiffUtil.set(itemAdapter, list, false)
        notifyDataSetChanged()
    }

    override fun getWalletViewModel(): AbstractWalletViewModel? = viewModel

    private fun notifyDataSetChanged() {
        binding.recycler.adapter?.notifyDataSetChanged()
    }

    private fun handleTwoFactorThreshold() {
        val binding = SettingsLimitsDialogBinding.inflate(requireActivity().layoutInflater)

        // Warning, don't change the order of fiat and btc,
        val currencies = listOf(getBitcoinOrLiquidUnit(session), getFiatCurrency(session))
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currency.adapter = adapter

        viewModel.twoFactorConfigLiveData.value?.limits?.let { limits ->
            binding.currency.setSelection(if (limits.isFiat) 1 else 0)

            if (limits.isFiat) {
                binding.amount = limits.fiat(withUnit = false)
            } else {
                binding.amount = limits.btc(session, withUnit = false)
            }
        }

        AmountTextWatcher.watch(binding.amountEditText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_set_twofactor_threshold)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val isFiat = binding.currency.selectedItemPosition == 1
                    val input = UserInput.parseUserInput(session, binding.amount, isFiat = isFiat)

                    viewModel.setLimits(input.toLimit(), DialogTwoFactorResolver(requireContext()))
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
                action = TwoFactorSetupAction.SETUP
            )
        )
    }

    private fun disable2FA(method: TwoFactorMethod) {
        viewModel.twoFactorConfigLiveData.value?.let {
            val binding = CustomTitleDialogBinding.inflate(requireActivity().layoutInflater)

            binding.title = getString(R.string.id_security_change)
            binding.message =
                getString(if (it.enabledMethods.size == 1) R.string.id_confirm_via_2fa_that_you else R.string.id_another_2fa_method_is_already)

            val methods = if(it.enabledMethods.size > 1) it.enabledMethods.filter { it -> it != method.gdkType } else it.enabledMethods

            MaterialAlertDialogBuilder(requireContext())
                .setCustomTitle(binding.root)
                .setSingleChoiceItems(requireContext().localized2faMethods(methods).toTypedArray(), -1) { dialog, i: Int ->
                    viewModel.disable2FA(
                        method,
                        DialogTwoFactorResolver(requireContext(), selectedMethod = methods[i])
                    )
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.id_i_lost_my_2fa) { _: DialogInterface, _: Int ->
                    navigate(TwoFactorAuthenticationFragmentDirections.actionTwoFractorAuthenticationFragmentToTwoFactorSetupFragment(
                        wallet = wallet,
                        action = TwoFactorSetupAction.RESET
                    ))
                }
                .show()
        }
    }
}
