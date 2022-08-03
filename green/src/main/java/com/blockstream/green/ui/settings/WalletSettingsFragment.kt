package com.blockstream.green.ui.settings

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.navArgs
import com.blockstream.base.Urls
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.SettingsNotification
import com.blockstream.gdk.data.asPricing
import com.blockstream.green.BuildConfig
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.extensions.authenticateWithBiometrics
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
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
import java.security.InvalidAlgorithmParameterException
import javax.inject.Inject

@AndroidEntryPoint
class WalletSettingsFragment :
    AbstractWalletFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName by lazy { if (args.showRecoveryTransactions) "WalletSettingsRecoveryTransactions" else "WalletSettings" }

    private val bannerAdapter = ItemAdapter<GenericItem>()
    private val itemAdapter = ItemAdapter<GenericItem>()

    private lateinit var logoutPreference: PreferenceListItem
    private lateinit var archivedAccountsPreference: PreferenceListItem
    private lateinit var supportIdPreference: PreferenceListItem
    private lateinit var watchOnlyPreference: PreferenceListItem
    private lateinit var unitPreference: PreferenceListItem
    private lateinit var priceSourcePreference: PreferenceListItem
    private lateinit var pgpPreference: PreferenceListItem
    private lateinit var altTimeoutPreference: PreferenceListItem
    private lateinit var twoFactorAuthenticationPreference: PreferenceListItem
    private lateinit var recoveryPreference: PreferenceListItem

    // Multisig
    private lateinit var multisigBitcoinPreference: PreferenceListItem
    private lateinit var multisigLiquidPreference: PreferenceListItem

    // Recovery Transactions
    private lateinit var recoveryTransactionsPreference: PreferenceListItem

    // Show Recovery Transactions
    private lateinit var setupEmailRecoveryTransactionsPreference: PreferenceListItem
    private lateinit var recoveryTransactionEmailsPreference: PreferenceListItem
    private lateinit var requestRecoveryTransactionsPreference: PreferenceListItem

    private lateinit var biometricsPreference: PreferenceListItem
    private lateinit var changePinPreference: PreferenceListItem

    private lateinit var versionPreference: PreferenceListItem

    val networkOrNull: Network?
        get() = args.network

    val network: Network
        get() = networkOrNull!!

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var appKeystore: AppKeystore

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by viewModels {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override val title: String?
        get() = if (args.showRecoveryTransactions) getString(R.string.id_recovery_transactions) else if(networkOrNull != null) getString(R.string.id_settings) else null

    override val subtitle: String?
        get() = if(networkOrNull != null) getString(R.string.id_multisig) else null

    override val toolbarIcon: Int?
        get() = networkOrNull?.getNetworkIcon()

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        getNavigationResult<Boolean>()?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.updateTwoFactorConfig()
                clearNavigationResult()
            }
        }

        binding.vm = viewModel

        supportIdPreference = PreferenceListItem(
            StringHolder(R.string.id_support),
            subtitle = StringHolder(R.string.id_copy_support_id),
            iconRes = R.drawable.ic_copy
        )
        watchOnlyPreference =
            PreferenceListItem(title = StringHolder(R.string.id_watchonly), isInnerMenu = true)
        logoutPreference = PreferenceListItem(
            StringHolder(R.string.id_logout),
            StringHolder(wallet.name),
            subtitleColor = R.color.red,
            iconRes = R.drawable.ic_baseline_logout_24
        )
        archivedAccountsPreference = PreferenceListItem(
            StringHolder(R.string.id_archived_accounts),
            isInnerMenu = true
        )
        unitPreference = PreferenceListItem(StringHolder(R.string.id_bitcoin_denomination))
        priceSourcePreference =
            PreferenceListItem(StringHolder(R.string.id_reference_exchange_rate))
        changePinPreference =
            PreferenceListItem(StringHolder(R.string.id_change_pin), isInnerMenu = true)
        multisigBitcoinPreference = PreferenceListItem(StringHolder("Bitcoin"), isInnerMenu = true)
        multisigLiquidPreference = PreferenceListItem(StringHolder("Liquid"), isInnerMenu = true)

        biometricsPreference = PreferenceListItem(
            StringHolder(R.string.id_login_with_biometrics),
            withSwitch = true
        )
        altTimeoutPreference = PreferenceListItem(StringHolder(R.string.id_auto_logout_timeout))
        recoveryPreference = PreferenceListItem(
            StringHolder(R.string.id_backup_recovery_phrase), StringHolder(
                R.string.id_touch_to_display
            ), isInnerMenu = true
        )
        // Recovery Transactions
        recoveryTransactionsPreference = PreferenceListItem(
            StringHolder(R.string.id_recovery_transactions),
            StringHolder(R.string.id_legacy_script_coins),
            isInnerMenu = true
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

        twoFactorAuthenticationPreference = PreferenceListItem(
            StringHolder(R.string.id_twofactor_authentication),
            isInnerMenu = true,
        )
        pgpPreference =
            PreferenceListItem(StringHolder(R.string.id_pgp_key))

        versionPreference = PreferenceListItem(
            StringHolder(R.string.id_version), StringHolder(
                String.format(
                    "%s %s",
                    getString(R.string.app_name),
                    getString(
                        R.string.id_version_1s_2s,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.BUILD_TYPE
                    )
                )
            )
        )

        updateAdapter()

        val fastAdapter = FastAdapter.with(listOf(bannerAdapter, itemAdapter))

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, iItem: GenericItem, _: Int ->
                when (iItem) {
                    logoutPreference -> {
                        viewModel.logout(AbstractWalletViewModel.LogoutReason.USER_ACTION)
                    }
                    archivedAccountsPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionGlobalArchivedAccountsFragment(
                                wallet = args.wallet
                            )
                        )
                    }
                    supportIdPreference -> {
                        viewModel.supportId?.also {
                            copyToClipboard("AccountID", it, requireContext())
                            snackbar(R.string.id_copied_to_clipboard)
                        }
                    }
                    watchOnlyPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToWatchOnlyFragment(
                                wallet
                            )
                        )
                    }
                    changePinPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToChangePinFragment(
                                wallet
                            )
                        )
                    }
                    multisigBitcoinPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentSelf(
                                wallet = wallet,
                                network = session.bitcoinMultisig
                            )
                        )
                    }
                    multisigLiquidPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentSelf(
                                wallet = wallet,
                                network = session.liquidMultisig
                            )
                        )
                    }
                    recoveryPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToRecoveryIntroFragment(
                                wallet = wallet, isAuthenticateUser = true
                            )
                        )
                    }
                    setupEmailRecoveryTransactionsPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFactorSetupFragment(
                                wallet = wallet,
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.SETUP_EMAIL,
                                network = network
                            )
                        )
                    }

                    recoveryTransactionsPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentSelf(
                                wallet = wallet,
                                showRecoveryTransactions = true,
                                network = session.bitcoinMultisig!!
                            )
                        )
                    }
                    recoveryTransactionEmailsPreference -> {
                        toggleRecoveryTransactionsEmails(network)
                    }
                    requestRecoveryTransactionsPreference -> {
                        viewModel.sendNlocktimes(network)
                    }
                    twoFactorAuthenticationPreference -> {
                        navigate(
                            WalletSettingsFragmentDirections.actionWalletSettingsFragmentToTwoFractorAuthenticationFragment(
                                wallet = wallet
                            )
                        )
                    }
                    unitPreference -> {
                        handleUnit()
                    }
                    priceSourcePreference -> {
                        handlePriceSource()
                    }
                    altTimeoutPreference -> {
                        handleAltTimeout()
                    }
                    pgpPreference -> {
                        handlePGP()
                    }
                    biometricsPreference -> {
                        if (viewModel.biometricsLiveData.value == null) {
                            enableBiometrics()
                        } else {
                            viewModel.removeBiometrics()
                        }
                    }
                    versionPreference -> {
                        navigate(
                            NavGraphDirections.actionGlobalAboutFragment()
                        )
                    }
                    else -> {

                    }
                }
                true
            }

        fastAdapter.addClickListener<ListItemHelpBinding, GenericItem>({ binding -> binding.buttonOutline }) { _, _, _, _ ->
            openBrowser(Urls.HELP_NLOCKTIMES)
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

        viewModel.prominentNetworkSettings.observe(viewLifecycleOwner) {
            it?.let {
                // TODO change it to only btc
                unitPreference.subtitle =
                    StringHolder(getBitcoinOrLiquidUnit(session = session, overrideDenomination = it.unit))
                if(session.defaultNetwork.isSinglesig){
                    priceSourcePreference.subtitle = StringHolder(it.pricing.currency)
                }else{
                    priceSourcePreference.subtitle = StringHolder(
                        getString(
                            R.string.id_s_from_s,
                            it.pricing.currency,
                            it.pricing.exchange
                        )
                    )
                }

                altTimeoutPreference.subtitle = StringHolder(
                    if (it.altimeout == 1) "1 " + getString(R.string.id_minute) else getString(
                        R.string.id_1d_minutes,
                        it.altimeout
                    )
                )

                notifyDataSetChanged()
            }
        }

        session.activeMultisig.firstOrNull()?.also {
            viewModel.networkSettingsLiveData(it).observe(viewLifecycleOwner) {
                pgpPreference.subtitle = if (it.pgp.isNullOrBlank()) StringHolder(R.string.id_add_a_pgp_public_key_to_receive) else StringHolder(null)
                fastAdapter.getItemById(pgpPreference.identifier)?.let {
                    it.second?.let { it1 -> fastAdapter.notifyAdapterItemChanged(it1) }
                }
            }
        }

        if (args.showRecoveryTransactions) {
            viewModel.networkSettingsLiveData(network).observe(viewLifecycleOwner) {
                recoveryTransactionEmailsPreference.switchChecked =
                    it.notifications?.emailIncoming == true
            }
        }

        viewModel.biometricsLiveData.distinctUntilChanged().observe(viewLifecycleOwner) {
            biometricsPreference.switchChecked = it != null
            updateBiometricsSubtitle()
            notifyDataSetChanged()
        }

        session.activeSessions.forEach {
            viewModel.networkTwoFactorConfigLiveData(it).observe(viewLifecycleOwner) {
                // use updateAdapter as we show/hide elements related to twofactorconfig eg. set recovery email
                updateAdapter()
            }
        }

        viewModel.archivedAccountsLiveData.observe(viewLifecycleOwner){
            archivedAccountsPreference.subtitle = viewModel.archivedAccounts.let {
                StringHolder(if(it > 0)  "(${it})" else null)
            }
        }
    }

    private fun updateBiometricsSubtitle() {
        val canUseBiometrics = appKeystore.canUseBiometrics(requireContext())

        biometricsPreference.subtitle = StringHolder(
            if (canUseBiometrics) {
                if (viewModel.biometricsLiveData.value == null) {
                    getString(R.string.id_biometric_login_is_disabled)
                } else {
                    getString(R.string.id_biometric_login_is_enabled)
                }
            } else {
                getString(R.string.id_a_screen_lock_must_be_enabled)
            }
        )

        biometricsPreference.isEnabled = canUseBiometrics
    }

    private fun updateAdapter() {
        // val isLiquid = wallet.isLiquid
        //val twoFactorConfig = viewModel.twoFactorConfigLiveData.value

        val list = mutableListOf<GenericItem>()

        val hasMultisig = session.activeBitcoinMultisig != null || session.activeLiquidMultisig != null

        if(networkOrNull != null){
            if (args.showRecoveryTransactions) {
                list += HelpListItem(
                    message = StringHolder(R.string.id_if_you_have_some_coins_on_the),
                    buttonOutline = StringHolder(R.string.id_more_info)
                )

                val twoFactorConfig = viewModel.networkTwoFactorConfigLiveData(network).value

                if (twoFactorConfig?.email?.confirmed == false) {
                    list += setupEmailRecoveryTransactionsPreference
                } else {
                    list += recoveryTransactionEmailsPreference
                    list += requestRecoveryTransactionsPreference
                }

            }else {
                val is2faReset = session.getTwoFactorReset(network)?.isActive == true
                if (is2faReset) {
                    // TODO cancel 2fa
                } else {

                }
            }
        } else {
            list += logoutPreference

            if (!session.isWatchOnly) {

                // General
                list += TitleListItem(StringHolder(R.string.id_general))
                list += unitPreference

                val bitcoin = session.bitcoin
                if (bitcoin != null) {
                    list += priceSourcePreference
                }

                list += archivedAccountsPreference

                // No support for Liquid Singlesig yet
                if (session.activeSessions.firstOrNull { (it.isMultisig || (it.isSinglesig && it.isBitcoin)) } != null) {
                    // Disable it until is supported by GDK
                     list += watchOnlyPreference
                }

                // Security
                list += TitleListItem(StringHolder(R.string.id_security))

                if (!wallet.isEphemeral) {
                    list += changePinPreference
                    list += biometricsPreference
                }

                if(hasMultisig){
                    list += twoFactorAuthenticationPreference
                    list += pgpPreference
                }

                list += altTimeoutPreference

                if(session.activeBitcoinMultisig != null || !session.isHardwareWallet) {
                    list += TitleListItem(StringHolder(R.string.id_recovery))

                    if (!session.isHardwareWallet) {
                        list += recoveryPreference
                    }

                    if(session.activeBitcoinMultisig != null) {
                        list += recoveryTransactionsPreference
                    }
                }
            }

            list += TitleListItem(StringHolder(R.string.id_about))

            list += versionPreference

            if(hasMultisig) {
                list += supportIdPreference
            }
        }

        updateBiometricsSubtitle()

        FastAdapterDiffUtil.set(itemAdapter, list, true)
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel


    private fun notifyDataSetChanged() {
        binding.recycler.adapter?.notifyDataSetChanged()
    }

    private fun handlePGP() {
        viewModel.networkSettingsLiveData(session.activeMultisig.first()).value?.let { settings ->
            val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))
            dialogBinding.text = settings.pgp
            dialogBinding.textInputLayout.endIconCustomMode()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_pgp_key)
                .setView(dialogBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.savePGP(dialogBinding.text?.trim())
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleUnit() {
        viewModel.prominentNetworkSettings.value?.let { settings ->

            val denominationsUnits = resources.getTextArray(R.array.btc_units_entries)
            val denominationsEntries =
                if (session.isTestnet) resources.getTextArray(R.array.btc_units_entries).map {
                    // TODO change it to bitcoin only
                    getBitcoinOrLiquidUnit(session = session, overrideDenomination = it.toString())
                }
                    .toTypedArray() as Array<CharSequence> else if (session.mainAssetNetwork.isLiquid) resources.getTextArray(
                    R.array.liquid_units_entries
                ) else denominationsUnits

            showChoiceDialog(
                getString(R.string.id_bitcoin_denomination),
                denominationsEntries,
                denominationsUnits.indexOf(
                    settings.unit
                )
            ) {
                val unit = denominationsUnits[it].toString()

                viewModel.saveGlobalSettings(settings.copy(unit = unit))
            }
        }
    }

    private fun handlePriceSource() {
        viewModel.prominentNetworkSettings.value?.let { settings ->
            try {
                val currencies = session.availableCurrencies()
                val entries: Array<CharSequence> = currencies.map {
                    getString(R.string.id_s_from_s, it.currency, it.exchange)
                }.let {
                    if(isDevelopmentOrDebug){
                        listOf("NULL (Debug)") + it
                    }else{
                        it
                    }
                }.toTypedArray()

                val values = currencies.map {
                    it.toIdentifiable()
                }.let {
                    if(isDevelopmentOrDebug){
                        listOf("null null") + it
                    }else{
                        it
                    }
                }.toTypedArray()

                showChoiceDialog(
                    getString(R.string.id_reference_exchange_rate), entries, values.indexOf(
                        settings.pricing.toIdentifiable()
                    )
                ) {
                    try{
                        values[it].asPricing()?.also { pricing ->
                            viewModel.saveGlobalSettings(settings.copy(pricing = pricing))

                            // Update Limits as changing exchange reference can also change limits
                            viewModel.updateTwoFactorConfig()

                            // Show 2FA warning
                            session.bitcoinMultisig?.let { bitcoinMultisig ->
                                viewModel.networkTwoFactorConfigLiveData(bitcoinMultisig).value?.let { twoFactorConfig ->
                                    if (twoFactorConfig.limits.satoshi > 0) {
                                        dialog(
                                            R.string.id_warning,
                                            R.string.id_changing_reference_exchange
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            errorDialog(getString(R.string.id_error))
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                        errorDialog(e)
                    }
                }

            } catch (e: Exception) {
                errorDialog(e)
            }
        }
    }

    private fun toggleRecoveryTransactionsEmails(network: Network) {
        viewModel.networkSettingsLiveData(network).value?.let { settings ->
            settings.notifications?.let { notifications ->
                val toggled = !notifications.emailIncoming
                viewModel.saveNetworkSettings(network, settings.copy(
                    notifications = SettingsNotification(
                        emailIncoming = toggled,
                        emailOutgoing = toggled
                    )
                ))
            }
        }
    }

    private fun handleAltTimeout() {
        viewModel.prominentNetworkSettings.value?.let { settings ->
            val values = resources.getStringArray(R.array.auto_logout_values)
            val entries: Array<CharSequence> = values.map {
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
                val altimeout = values[it].toInt()
                viewModel.saveGlobalSettings(settings.copy(altimeout = altimeout))
            }
        }
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

    private fun enableBiometrics(onlyDeviceCredentials: Boolean = false) {

        if (appKeystore.isBiometricsAuthenticationRequired()) {
            authenticateWithBiometrics(object : AuthenticationCallback(fragment = this) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    enableBiometrics(onlyDeviceCredentials = false)
                }
            }, onlyDeviceCredentials = onlyDeviceCredentials)
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_login_with_biometrics))
            .setDescription(getString(R.string.id_green_uses_biometric))
            .setNegativeButtonText(getString(R.string.id_cancel))
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : AuthenticationCallback(fragment = this) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let {
                        viewModel.enableBiometrics(it)
                    }
                }
            })

        try {
            biometricPrompt.authenticate(
                promptInfo.build(),
                BiometricPrompt.CryptoObject(appKeystore.getBiometricsEncryptionCipher())
            )
        } catch (e: InvalidAlgorithmParameterException) {
            // At least one biometric must be enrolled
            errorDialog(getString(R.string.id_please_activate_at_least_one))
        } catch (e: Exception) {
            errorDialog(e)
        }
    }
}
