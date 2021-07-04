package com.blockstream.green.ui.settings

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.preference.*
import com.blockstream.gdk.data.SettingsNotification
import com.blockstream.gdk.data.asPricing
import com.blockstream.green.*
import com.blockstream.green.R
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.databinding.SettingsWatchOnlyDialogBinding
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.filters.NumberValueFilter
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.greenaddress.Bridge.versionName
import com.greenaddress.greenbits.ui.BuildConfig
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import java.security.InvalidAlgorithmParameterException
import javax.inject.Inject

@AndroidEntryPoint
class WalletSettingsFragment :
    WalletFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    private val itemAdapter = ItemAdapter<GenericItem>()

    private lateinit var watchOnlyPreference: PreferenceListItem
    private lateinit var unitPreference: PreferenceListItem
    private lateinit var priceSourcePreference: PreferenceListItem
    private lateinit var txPriorityPreference: PreferenceListItem
    private lateinit var customFeeRatePreference: PreferenceListItem
    private lateinit var pgpPreference: PreferenceListItem
    private lateinit var altTimeoutPreference: PreferenceListItem
    private lateinit var twoFactorAuthenticationPreference: PreferenceListItem
    private lateinit var recoveryPreference: PreferenceListItem

    // Recovery Transactions
    private lateinit var recoveryTransactionsPreference: PreferenceListItem
    private lateinit var setupEmailRecoveryTransactionsPreference: PreferenceListItem
    private lateinit var recoveryTransactionEmailsPreference: PreferenceListItem
    private lateinit var requestRecoveryTransactionsPreference: PreferenceListItem

    private lateinit var biometricsPreference: PreferenceListItem
    private lateinit var changePinPreference: PreferenceListItem

    private lateinit var versionPreference: PreferenceListItem
    private lateinit var termsOfServicePreference: PreferenceListItem
    private lateinit var privacyPolicyPreference: PreferenceListItem

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var appKeystore: AppKeystore

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by navGraphViewModels(R.id.settings_nav_graph) {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            updateSharedPreferencesSummaries()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<Boolean>()?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.updateTwoFactorConfig()
                clearNavigationResult()
            }
        }

        if (args.bridgeTwoFactorReset) {
            navigate(
                directions = WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFactorSetupFragment(
                    wallet = wallet,
                    method = TwoFactorMethod.EMAIL,
                    action = args.bridgeTwoFactorSetupType
                ),
                navOptionsBuilder = NavOptions.Builder()
                    .setPopUpTo(findNavController().backStack.first.destination.id, true)
            )
        }

        binding.vm = viewModel

        if(args.showRecoveryTransactions) {
            setToolbar(getString(R.string.id_recovery_transactions))
        }else if(args.bridgeShowPIN){
            setToolbar(getString(R.string.id_setup_pin))
        }

        watchOnlyPreference = PreferenceListItem(StringHolder(R.string.id_watchonly_login))
        unitPreference = PreferenceListItem(StringHolder(R.string.id_bitcoin_denomination))
        priceSourcePreference = PreferenceListItem(StringHolder(R.string.id_reference_exchange_rate))
        txPriorityPreference = PreferenceListItem(StringHolder(R.string.id_default_transaction_priority))
        customFeeRatePreference = PreferenceListItem(StringHolder(R.string.id_default_custom_fee_rate))
        changePinPreference = PreferenceListItem(StringHolder(R.string.id_change_pin))
        biometricsPreference = PreferenceListItem(
            StringHolder(R.string.id_login_with_biometrics),
            withSwitch = true
        )
        altTimeoutPreference = PreferenceListItem(StringHolder(R.string.id_auto_logout_timeout))
        recoveryPreference = PreferenceListItem(
            StringHolder(R.string.id_back_up_recovery_phrase), StringHolder(
                R.string.id_touch_to_display
            )
        )
        // Recovery Transactions
        recoveryTransactionsPreference = PreferenceListItem(
            StringHolder(R.string.id_recovery_transactions),
            StringHolder(R.string.id_legacy_outputs)
        )
        setupEmailRecoveryTransactionsPreference = PreferenceListItem(
            StringHolder(R.string.id_set_an_email_for_recovery)
        )
        recoveryTransactionEmailsPreference = PreferenceListItem(
            StringHolder(R.string.id_recovery_transaction_emails),
            withSwitch = true
        )
        requestRecoveryTransactionsPreference = PreferenceListItem(
            StringHolder(R.string.id_request_recovery_transactions)
        )

        twoFactorAuthenticationPreference = PreferenceListItem(StringHolder(R.string.id_twofactor_authentication))
        pgpPreference = PreferenceListItem(StringHolder(R.string.id_pgp_key))

        versionPreference = PreferenceListItem(StringHolder(R.string.id_version), StringHolder(
            String.format(
                "%s %s",
                getString(R.string.app_name),
                getString(
                    R.string.id_version_1s_2s,
                    versionName,
                    BuildConfig.BUILD_TYPE
                )
            )))
        termsOfServicePreference = PreferenceListItem(StringHolder(R.string.id_terms_of_service))
        privacyPolicyPreference = PreferenceListItem(StringHolder(R.string.id_privacy_policy))

        updateAdapter()

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, iItem: GenericItem, _: Int ->
                when (iItem) {
                    watchOnlyPreference -> {
                        handleWatchOnly()
                    }
                    changePinPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToChangePinFragment(
                                wallet
                            )
                        )
                    }
                    recoveryPreference -> {
                        navigate(
                            NavGraphDirections.actionGlobalRecoveryIntroFragment(
                                wallet = wallet
                            )
                        )
                    }
                    setupEmailRecoveryTransactionsPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFactorSetupFragment(
                                wallet = wallet,
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.SETUP_EMAIL
                            )
                        )
                    }
                    recoveryTransactionsPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentSelf(
                                wallet = wallet,
                                showRecoveryTransactions = true
                            )
                        )
                    }
                    recoveryTransactionEmailsPreference -> {
                        toggleRecoveryTransactionsEmails()
                    }
                    requestRecoveryTransactionsPreference -> {
                        viewModel.sendNlocktimes()
                    }
                    twoFactorAuthenticationPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFractorAuthenticationFragment(
                                wallet
                            )
                        )
                    }
                    unitPreference -> {
                        handleUnit()
                    }
                    priceSourcePreference -> {
                        handlePriceSource()
                    }
                    txPriorityPreference -> {
                        handleTxPriority()
                    }
                    altTimeoutPreference -> {
                        handleAltTimeout()
                    }
                    customFeeRatePreference -> {
                        handleCustomFeeRate()
                    }
                    pgpPreference -> {
                        handlePGP()
                    }
                    termsOfServicePreference -> {
                        openBrowser(settingsManager.getApplicationSettings(), Urls.TERMS_OF_SERVICE)
                    }
                    privacyPolicyPreference -> {
                        openBrowser(settingsManager.getApplicationSettings(), Urls.PRIVACY_POLICY)
                    }
                    biometricsPreference -> {
                        if (viewModel.biometricsLiveData.value == null) {
                            enableBiometrics()
                        } else {
                            viewModel.removeBiometrics()
                        }
                    }
                    else -> {

                    }
                }
                true
            }

        fastAdapter.addClickListener<ListItemHelpBinding, GenericItem>({ binding -> binding.buttonOutline }) { _, _, _, item ->
            openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_NLOCKTIMES)
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.settingsLiveData.observe(viewLifecycleOwner) {
            it?.let {
                unitPreference.subtitle = StringHolder((if (session.isLiquid) "L-" else "") + it.unit)
                priceSourcePreference.subtitle = StringHolder(
                    getString(
                        R.string.id_s_from_s,
                        it.pricing.currency,
                        it.pricing.exchange
                    )
                )
                txPriorityPreference.subtitle = StringHolder(prioritySummary(it.requiredNumBlocks))
                pgpPreference.subtitle = StringHolder(R.string.id_add_a_pgp_public_key_to_receive)

                altTimeoutPreference.subtitle = StringHolder(
                    if (it.altimeout == 1) "1 " + getString(R.string.id_minute) else getString(
                        R.string.id_1d_minutes,
                        it.altimeout
                    )
                )

                recoveryTransactionEmailsPreference.switchChecked = it.notifications?.emailIncoming == true

                notifyDataSetChanged()
            }
        }

        viewModel.watchOnlyUsernameLiveData.observe(viewLifecycleOwner) {
            watchOnlyPreference.subtitle = StringHolder(
                if (it.isNullOrBlank()) {
                    getString(R.string.id_set_up_watchonly_credentials)
                } else {
                    getString(R.string.id_enabled_1s, it)
                }
            )

            notifyDataSetChanged()
        }

        viewModel.biometricsLiveData.distinctUntilChanged().observe(viewLifecycleOwner) {
            biometricsPreference.switchChecked = it != null
            updateBiometricsSubtitle()
            notifyDataSetChanged()
        }

        viewModel.twoFactorConfigLiveData.observe(viewLifecycleOwner) {
            // use updateAdapter as we show/hide elements related to twofactorconfig eg. set recovery email
            updateAdapter()
        }
    }

    private fun updateBiometricsSubtitle(){
        val canUseBiometrics = appKeystore.canUseBiometrics(requireContext())

        biometricsPreference.subtitle = StringHolder(
            if (canUseBiometrics) {
                if (viewModel.biometricsLiveData.value == null) {
                    getString(R.string.id_biometric_login_is_disabled)
                } else {
                    getString(R.string.id_biometric_login_is_enabled)
                }
            } else {
                //getString(R.string.id_biometrics_are_disabled_in_your_device)
                getString(R.string.id_a_screen_lock_must_be_enabled)
            }
        )

        biometricsPreference.isEnabled = canUseBiometrics
    }

    private fun updateAdapter(){
        val isLiquid = wallet.isLiquid

        val settings = viewModel.settingsLiveData.value
        val twoFactorConfig = viewModel.twoFactorConfigLiveData.value

        val anyTwoFactorEnabled = twoFactorConfig?.anyEnabled ?: false

        val list = mutableListOf<GenericItem>()

        if(args.showRecoveryTransactions){

            list += HelpListItem(
                message = StringHolder(R.string.id_if_you_have_some_coins_on_the_legacy),
                buttonOutline = StringHolder(R.string.id_read_more)
            )

            if(twoFactorConfig?.email?.confirmed == false){
                list += setupEmailRecoveryTransactionsPreference
            }else{
                list += recoveryTransactionEmailsPreference
                list += requestRecoveryTransactionsPreference
            }

        }else if(args.bridgeShowPIN){
            list += changePinPreference
            list += biometricsPreference
        }else {
            val is2faReset = session.getTwoFactorReset()?.isActive == true

            if (is2faReset) {

                list += TitleListItem(StringHolder(R.string.id_recovery))
                list += recoveryPreference

            } else {
                list += TitleListItem(StringHolder(R.string.id_general))

                if (!isLiquid) {
                    list += watchOnlyPreference
                }

                list += unitPreference

                if (!isLiquid) {
                    list += priceSourcePreference
                    list += txPriorityPreference
                    list += customFeeRatePreference
                }


                list += TitleListItem(StringHolder(R.string.id_security))

                list += changePinPreference

                list += biometricsPreference

                list += altTimeoutPreference

                list += twoFactorAuthenticationPreference

                list += TitleListItem(StringHolder(R.string.id_recovery))
                list += recoveryPreference

                if(!session.isLiquid && !session.isElectrum) {
                    list += recoveryTransactionsPreference
                }

                list += TitleListItem(StringHolder(R.string.id_advanced))

                list += pgpPreference
            }

            list += TitleListItem(StringHolder(R.string.id_about))

            list += versionPreference
            list += termsOfServicePreference
            list += privacyPolicyPreference
        }

        updateBiometricsSubtitle()

        FastAdapterDiffUtil.set(itemAdapter, list, false)
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        updateSharedPreferencesSummaries()

        updateAdapter()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
    }

    private fun updateSharedPreferencesSummaries() {
        customFeeRatePreference.subtitle = StringHolder(getDefaultFeeRateAsDouble().feeRateWithUnit())
        notifyDataSetChanged()
    }

    private fun notifyDataSetChanged() { binding.recycler.adapter?.notifyDataSetChanged() }


    private fun handleWatchOnly(): Boolean {
        val dialogBinding =
            SettingsWatchOnlyDialogBinding.inflate(requireActivity().layoutInflater)

        viewModel.watchOnlyUsernameLiveData.value?.let {
            dialogBinding.username = it
        }

        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Green_MaterialAlertDialog
        )
            .setTitle(R.string.id_watchonly_login)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (dialogBinding.username.isNullOrBlank() || dialogBinding.password.isNullOrBlank()) {
                    snackbar(R.string.id_the_password_cant_be_empty)
                    return@setPositiveButton
                }

                viewModel.setWatchOnly(
                    dialogBinding.username ?: "",
                    dialogBinding.password ?: ""
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        return true
    }

    private fun handlePGP() {
        viewModel.settingsLiveData.value?.let { settings ->

            val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))
            dialogBinding.text = settings.pgp

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_pgp_key)
                .setView(dialogBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.saveSettings(settings.copy(pgp = dialogBinding.text))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun handleUnit() {
        viewModel.settingsLiveData.value?.let { settings ->

            val denominationsUnits = resources.getTextArray(R.array.btc_units_entries)
            val denominationsEntries = if (wallet.isLiquid) resources.getTextArray(R.array.liquid_units_entries) else denominationsUnits

            showChoiceDialog(
                getString(R.string.id_bitcoin_denomination),
                denominationsEntries,
                denominationsUnits.indexOf(
                    settings.unit
                )
            ) {
                viewModel.saveSettings(settings.copy(unit = denominationsUnits[it].toString()))
            }
        }
    }

    private fun handlePriceSource() {

        viewModel.settingsLiveData.value?.let { settings ->

            val currencies = session.availableCurrencies

            val entries : Array<CharSequence> = currencies.map {
                getString(R.string.id_s_from_s, it.currency, it.exchange)
            }.toTypedArray()

            val values = currencies.map {
                it.toIdentifiable()
            }.toTypedArray()

            showChoiceDialog(
                getString(R.string.id_reference_exchange_rate), entries, values.indexOf(
                    settings.pricing.toIdentifiable()
                )
            ) {
                values[it].asPricing()?.let{ pricing ->
                    viewModel.saveSettings(settings.copy(pricing = pricing))

                    // Update Limits as changing exchange reference can also change limits
                    viewModel.updateTwoFactorConfig()

                    // Show 2FA warning
                    viewModel.twoFactorConfigLiveData.value?.let { twoFactorConfig ->
                        if(twoFactorConfig.limits.satoshi > 0){
                            dialog(
                                R.string.id_warning,
                                R.string.id_changing_reference_exchange
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleRecoveryTransactionsEmails() {
        viewModel.settingsLiveData.value?.let { settings ->
            settings.notifications?.let { notifications ->
                val toggled = !notifications.emailIncoming
                viewModel.saveSettings(
                    settings.copy(
                        notifications = SettingsNotification(
                            emailIncoming = toggled,
                            emailOutgoing = toggled
                        )
                    )
                )
            }
        }
    }

    private fun handleTxPriority() {
        viewModel.settingsLiveData.value?.let { settings ->
            val entries = resources.getTextArray(R.array.fee_target_entries)
            val values = resources.getTextArray(R.array.fee_target_values)

            showChoiceDialog(
                getString(R.string.id_default_transaction_priority), entries, values.indexOf(
                    settings.requiredNumBlocks.toString()
                )
            ) {
                viewModel.saveSettings(
                    settings.copy(
                        requiredNumBlocks = values[it].toString().toInt()
                    )
                )
            }
        }
    }

    private fun handleAltTimeout() {
        viewModel.settingsLiveData.value?.let { settings ->
            val values = resources.getStringArray(R.array.auto_logout_values)
            val entries : Array<CharSequence> = values.map {
                val minutes = Integer.valueOf(it)
                if (minutes == 1) "1 " + getString(R.string.id_minute) else getString(
                    R.string.id_1d_minutes,
                    minutes
                )
            }.toTypedArray()

            showChoiceDialog(
                getString(R.string.id_auto_logout_timeout), entries, values.indexOf(
                    settings.altimeout.toString()
                )
            ) {
                viewModel.saveSettings(settings.copy(altimeout = values[it].toInt()))
            }
        }
    }

    private fun handleCustomFeeRate(){
        val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))

        // TODO add locale
        dialogBinding.editText.setPlaceholder("0.00")
        dialogBinding.editText.keyListener = NumberValueFilter(2)
        dialogBinding.text = sharedPreferences.getString(Preferences.DEFAULT_FEE_RATE, "")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_default_custom_fee_rate)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                try {
                    dialogBinding.text.let { input ->

                        if (input.isNullOrBlank()) {
                            sharedPreferences.edit().remove(Preferences.DEFAULT_FEE_RATE).apply()
                        } else {
                            val minFeeRateKB: Long = session.getFees()[0]
                            val enteredFeeRate = dialogBinding.text?.toDouble() ?: 0.0
                            if (enteredFeeRate * 1000 < minFeeRateKB) {
                                snackbar(
                                    getString(
                                        R.string.id_fee_rate_must_be_at_least_s, String.format(
                                            "%.2f",
                                            minFeeRateKB / 1000.0
                                        )
                                    ), Snackbar.LENGTH_SHORT
                                )
                            } else {
                                // Save to SharedPreferences
                                sharedPreferences.edit()
                                    .putString(Preferences.DEFAULT_FEE_RATE, dialogBinding.text).apply()
                            }
                        }
                    }

                } catch (e: Exception) {
                    snackbar(R.string.id_error_setting_fee_rate, Snackbar.LENGTH_SHORT)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }

    private fun showChoiceDialog(
        title: String,
        items: Array<CharSequence>,
        checkedItem: Int,
        listener: (position: Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(items, checkedItem) { dialog: DialogInterface, position: Int ->
                listener.invoke(position)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun getDefaultFeeRateAsDouble(): Double {
        // As a fallback set default fee from session
        var defaultFee = (session.getFees().firstOrNull() ?: 1000L) / 1000.0

        try {
            if(sharedPreferences.contains(Preferences.DEFAULT_FEE_RATE)) {
                defaultFee =
                    sharedPreferences.getString(Preferences.DEFAULT_FEE_RATE, null)!!.toDouble()
            }
        } catch (e: Exception) {

        }
        return defaultFee
    }

    private fun prioritySummary(blocks: Int): String {
        val blocksPerHour = 6
        val n: Int =
            if (blocks % blocksPerHour == 0) blocks / blocksPerHour else blocks * (60 / blocksPerHour)
        val confirmationInBlocks: String = getString(R.string.id_confirmation_in_d_blocks, blocks)
        val idTime: Int =
            if (blocks % blocksPerHour == 0) (if (blocks == blocksPerHour) R.string.id_hour else R.string.id_hours) else R.string.id_minutes
        return "%s, %d %s %s".format(
            confirmationInBlocks, n, resources.getString(idTime),
            getString(R.string.id_on_average)
        )
    }

    private fun enableBiometrics(){
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_login_with_biometrics))
            .setDescription(getString(R.string.id_green_uses_biometric))
            .setNegativeButtonText(getString(R.string.id_cancel))
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    handleBiometricsError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    result.cryptoObject?.cipher?.let {
                        viewModel.enableBiometrics(it)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        try {
            biometricPrompt.authenticate(
                promptInfo.build(),
                BiometricPrompt.CryptoObject(appKeystore.getBiometricsEncryptionCipher())
            )
        }catch (e: InvalidAlgorithmParameterException){
            // At least one biometric must be enrolled
            errorDialog(getString(R.string.id_please_activate_at_least_one))
        } catch (e: Exception) {
            errorDialog(e)
        }
    }
}
