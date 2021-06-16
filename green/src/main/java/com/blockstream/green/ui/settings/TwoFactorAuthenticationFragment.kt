package com.blockstream.green.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.gdk.data.TwoFactorConfig
import com.blockstream.green.*
import com.blockstream.green.databinding.*
import com.blockstream.green.settings.ApplicationSettings
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
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TwoFactorAuthenticationFragment : WalletFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    private val itemAdapter = ItemAdapter<GenericItem>()

    private lateinit var emailPreference: PreferenceListItem
    private lateinit var smsPreference: PreferenceListItem
    private lateinit var callPreference: PreferenceListItem
    private lateinit var toptPreference: PreferenceListItem
    private lateinit var thresholdPreference: PreferenceListItem
    private lateinit var expirationPreference: PreferenceListItem

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by navGraphViewModels(R.id.settings_nav_graph) {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        emailPreference = PreferenceListItem(StringHolder(R.string.id_email), withSwitch = true)
        smsPreference = PreferenceListItem(StringHolder(R.string.id_sms), withSwitch = true)
        callPreference = PreferenceListItem(StringHolder(R.string.id_call), withSwitch = true)
        toptPreference =
            PreferenceListItem(StringHolder(R.string.id_authenticator_app), withSwitch = true)

        thresholdPreference = PreferenceListItem(StringHolder(R.string.id_twofactor_threshold))
        expirationPreference = PreferenceListItem(StringHolder(R.string.id_customize_2fa_expiration_of))

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, item: GenericItem, _: Int ->
                viewModel.twoFactorConfigLiveData.value?.let {
                    when (item) {
                        emailPreference -> {
                            if (it.email.enabled) {
                                disable2FA("email")
                            } else {
                                enable2FA("email")
                            }
                        }

                        smsPreference -> {
                            if (it.sms.enabled) {
                                disable2FA("sms")
                            } else {
                                enable2FA("sms")
                            }
                        }

                        callPreference -> {
                            if (it.phone.enabled) {
                                disable2FA("phone")
                            } else {
                                enable2FA("phone")
                            }
                        }

                        toptPreference -> {
                            if (it.gauth.enabled) {
                                disable2FA("gauth")
                            } else {
                                enable2FA("gauth")
                            }
                        }

                        thresholdPreference -> {
                            handleTwoFactorThreshold()
                        }

                        expirationPreference -> {
                            navigate(TwoFactorAuthenticationFragmentDirections.actionTwoFractorAuthenticationFragmentToTwoFactorExpirationFragment(
                                wallet
                            ))
                        }

                        else -> {

                        }
                    }
                }
                true
            }

        fastAdapter.addClickListener<ListItemPreferenceBinding, GenericItem>({ binding -> binding.button }) { _, _, _, item ->
            viewModel.twoFactorConfigLiveData.value?.let {

                when (item) {
                    emailPreference -> {

                        if (it.email.enabled) {

                        } else {

                        }


                    }

                    else -> {

                    }
                }
            }
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
            }
        }

        viewModel.twoFactorConfigLiveData.observe(viewLifecycleOwner) {
            updateAdapter(it)
        }


//        viewModel.twoFactorConfigLiveData.observe(viewLifecycleOwner) {
//            twoFactorLimitsPreference.subtitle = StringHolder(it.limits.let { limits ->
//                if(!limits.isFiat && limits.satoshi == 0L){
//                    getString(R.string.id_set_twofactor_threshold)
//                }else if(limits.isFiat){
//                    limits.fiat()
//                }else{
//                    limits.btc(session)
//                }
//            })
//
//            updateAdapter()
//        }
    }

    private fun updateAdapter(twoFactorConfig: TwoFactorConfig) {
        val list = mutableListOf<GenericItem>()

        list += HelpListItem(
            StringHolder(R.string.id_enable_twofactor_authentication),
            StringHolder(R.string.id_tip_we_recommend_you_enable)
        )

        list += TitleListItem(StringHolder(R.string.id_2fa_methods))

        list += emailPreference.also {
            it.switchChecked = twoFactorConfig.email.enabled
            it.subtitle = StringHolder(twoFactorConfig.email.data.ifBlank { null })
            it.withButton = twoFactorConfig.email.enabled

        }
        list += smsPreference.also {
            it.switchChecked = twoFactorConfig.sms.enabled
            it.subtitle = StringHolder(twoFactorConfig.sms.data.ifBlank { null })
            it.withButton = twoFactorConfig.sms.enabled
        }
        list += callPreference.also {
            it.switchChecked = twoFactorConfig.phone.enabled
            it.subtitle = StringHolder(twoFactorConfig.phone.data.ifBlank { null })
            it.withButton = twoFactorConfig.phone.enabled
        }
        list += toptPreference.also {
            it.switchChecked = twoFactorConfig.gauth.enabled
            it.subtitle =
                StringHolder(if (twoFactorConfig.gauth.enabled) getString(R.string.id_enabled) else null)
            it.withButton = twoFactorConfig.gauth.enabled
        }

        list += TitleListItem(StringHolder(R.string.id_2fa_expiration))

        if(!session.isLiquid && !session.isElectrum) {
            list += expirationPreference
        }

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

        list += TitleListItem(StringHolder(R.string.id_2fa_expiry))

        list += HelpListItem(
            message = StringHolder(R.string.id_your_2fa_expires_so_that_if_you),
            buttonText = StringHolder(R.string.id_recovery_tool)
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
            } else { binding.amount = limits.btc(session, withUnit = false)
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

    private fun enable2FA(method: String) {

    }

    private fun disable2FA(method: String) {
        viewModel.twoFactorConfigLiveData.value?.let {
            val binding = CustomTitleDialogBinding.inflate(requireActivity().layoutInflater)

            binding.title = getString(R.string.id_security_change)
            binding.message =
                getString(if (it.enabledMethods.size == 1) R.string.id_confirm_via_2fa_that_you else R.string.id_another_2fa_method_is_already)


            MaterialAlertDialogBuilder(requireContext())
                .setCustomTitle(binding.root)
                .setSingleChoiceItems(it.enabledMethods.toTypedArray(), -1) { dialog, i: Int ->
                    viewModel.disable2FA(
                        method,
                        DialogTwoFactorResolver(requireContext(), method = method)
                    )
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.id_i_lost_my_2fa) { dialogInterface: DialogInterface, i: Int ->
                    reset2FA()
                }
                .show()
        }
    }

    private fun reset2FA() {



    }
}