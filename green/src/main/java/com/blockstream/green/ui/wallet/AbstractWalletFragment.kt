package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.gdk.data.Pricing
import com.blockstream.gdk.params.ReconnectHintParams
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.DenominationExchangeDialogBinding
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.iconResource
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.login.LoginFragment
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import mu.KLogging

abstract class AbstractWalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    abstract val walletOrNull: Wallet?

    var sessionOrNull: GdkSession? = null
    val session: GdkSession
        get() = sessionOrNull!!

    private var networkSnackbar: Snackbar? = null

    override val segmentation
        get() = if (isSessionAndWalletRequired() && isSessionNetworkInitialized) countly.sessionSegmentation(
            session = session
        ) else null

    val isSessionInitialized
        get() = sessionOrNull != null

    internal val isSessionNetworkInitialized
        get() = isSessionInitialized && session.isNetworkInitialized

    val isSessionConnected
        get() = isSessionNetworkInitialized && session.isConnected

    val wallet: Wallet
        get() = getWalletViewModel().wallet

    override fun updateToolbar() {
        super.updateToolbar()
        if (isSessionAndWalletRequired() && isSessionInitialized) {

            // Prevent showing network icon when the title is empty
            if(toolbar.title.isNotBlank() || !title.isNullOrBlank()) {

                toolbar.setLogo(
                    toolbarIcon ?: wallet.iconResource(session)
                )
            }
        }
    }

    // Guard initialization code for fragments that requires a connected session
    abstract fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessionOrNull = walletOrNull?.let { wallet ->
            sessionManager.getWalletSessionOrNull(wallet) ?: run {
                // Try to recreate it from Wallet, only for sww and only on LoginFragment (to avoid view models to be initialized that require a connected session).
                if (this is LoginFragment && !wallet.isHardware) sessionManager.getWalletSession(wallet) else null
            }
        }

        // Recovery intro screen is reused in onBoarding
        // where we don't have a session yet
        // Skip initializing the WalletViewModel as it doesn't exists
        if (isSessionAndWalletRequired()) {

            if(sessionOrNull == null){
                return
            }

            this.sessionOrNull = sessionOrNull

            if (isLoggedInRequired() && !isSessionConnected) {
                // If session is not initialized, avoid getting the ViewModel as can use GreenSession
                // without being properly initialized and can lead to a crash
                if (isSessionInitialized) {
                    logger.info { "A logged in session is required, but session is uninitialized" }
                    getWalletViewModel().logout(AbstractWalletViewModel.LogoutReason.TIMEOUT)
                } else {
                    logger.info { "A logged in session is required, but session is not connected" }
                    navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
                    // Stop flow as usage of WalletViewModel will lead to crash
                    return
                }
            }

            // Check if we have a disconnect event eg. the Notification button
            sessionManager.connectionChangeEvent.observe(viewLifecycleOwner) {
                if(isLoggedInRequired() && !session.isConnected){
                    getWalletViewModel().logout(AbstractWalletViewModel.LogoutReason.USER_ACTION)
                }
            }

            getWalletViewModel().let {

                setupDeviceInteractionEvent(it.onDeviceInteractionEvent)

                it.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
                    consumableEvent.getContentIfNotHandledForType<AbstractWalletViewModel.WalletEvent.Logout>()?.let {
                        // If is ephemeral wallet, prefer going to intro
                        if (wallet.isEphemeral || wallet.isHardware) {
                            NavGraphDirections.actionGlobalIntroFragment()
                        } else {
                            NavGraphDirections.actionGlobalLoginFragment(wallet)
                        }.let { directions ->
                            navigate(directions.actionId, directions.arguments, isLogout = true)
                        }

                        when(it.reason){
                            AbstractWalletViewModel.LogoutReason.DISCONNECTED -> {
                                snackbar(R.string.id_unstable_internet_connection)
                            }
                            AbstractWalletViewModel.LogoutReason.TIMEOUT -> {
                                snackbar(R.string.id_auto_logout_timeout_expired)
                            }
                            AbstractWalletViewModel.LogoutReason.DEVICE_DISCONNECTED -> {
                                snackbar(R.string.id_your_device_was_disconnected)
                            }
                            AbstractWalletViewModel.LogoutReason.USER_ACTION -> {

                            }
                        }
                    }
                }

                it.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
                    consumableEvent?.getContentIfNotHandledForType<AbstractWalletViewModel.WalletEvent.DeleteWallet>()?.let {
                        NavGraphDirections.actionGlobalIntroFragment().let { directions ->
                            navigate(directions.actionId, directions.arguments, isLogout = true)
                        }
                    }

                    consumableEvent?.getContentIfNotHandledForType<AbstractWalletViewModel.WalletEvent.RenameWallet>()?.let {
                        updateToolbar()
                    }
                }

                it.onReconnectEvent.observe(viewLifecycleOwner) { event ->
                    event.getContentIfNotHandledOrReturnNull()?.let{ time ->
                        if(time == -1L){
                            networkSnackbar = networkSnackbar?.let { snackbar ->
                                snackbar.dismiss()
                                null
                            }
                        }else{
                            networkSnackbar  = networkSnackbar ?: Snackbar.make(
                                view,
                                R.string.id_you_are_not_connected,
                                Snackbar.LENGTH_INDEFINITE
                            ).apply { show() }

                            if(time == 0L){
                                networkSnackbar?.setAction(null, null)
                            }else{
                                networkSnackbar?.setAction(R.string.id_now){
                                    session.reconnectHint(ReconnectHintParams(
                                        hint = ReconnectHintParams.KEY_CONNECT
                                    ))
                                    networkSnackbar = null
                                }
                            }

                            networkSnackbar?.setText(
                                getString(
                                    if(time == 0L) R.string.id_connecting else R.string.id_not_connected_connecting_in_ds_, time
                                )
                            )
                        }
                    }
                }
            }
        }

        // Session must be initialized first if required as AppFragment can request appViewModel
        super.onViewCreated(view, savedInstanceState)

        // Prevent
        if (!isSessionAndWalletRequired() || !isLoggedInRequired() || isSessionConnected) {
            onViewCreatedGuarded(view, savedInstanceState)
        }
    }

    open fun isLoggedInRequired(): Boolean = true
    open fun isSessionAndWalletRequired(): Boolean = true

    abstract fun getWalletViewModel(): AbstractWalletViewModel

    override fun getAppViewModel() : AppViewModel? {
        // Prevent initializing WalletViewModel if session is not initialized
        if (isSessionAndWalletRequired() && (isLoggedInRequired() && !isSessionInitialized)) {
            return null
        }

        return getWalletViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Recovery screens are reused in onBoarding
        // where we don't have a session yet.
        if (isSessionAndWalletRequired()) {
            if (!isSessionInitialized && walletOrNull?.isHardware == true) {
                navigate(NavGraphDirections.actionGlobalIntroFragment())
            } else if (isLoggedInRequired() && !isSessionConnected) {
                // If session is not initialized, avoid getting the ViewModel as can use GreenSession
                // without being properly initialized and can lead to a crash
                if (isSessionNetworkInitialized) {
                    getWalletViewModel().logout(AbstractWalletViewModel.LogoutReason.TIMEOUT)
                } else {
                    // Use walletOrNull else wallet will trigger ViewModel initialization
                    walletOrNull?.also {
                        navigate(NavGraphDirections.actionGlobalLoginFragment(it))
                    } ?: run {
                        navigate(NavGraphDirections.actionGlobalIntroFragment())
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dismiss network to avoid UI leaking to other fragments
        networkSnackbar?.dismiss()
    }

    @Suppress("UNCHECKED_CAST")
    fun showDenominationAndExchangeRateDialog(onSuccess: (() -> Unit)? = null) {

        session.getSettings()?.let { settings ->

            val denominationsUnitsIdentities = resources.getTextArray(R.array.btc_units_entries).map {
                it.toString()
            }

            val denominationsUnits = if (session.isTestnet) {
                resources.getTextArray(
                    R.array.testnet_units_entries
                ).map { it.toString() }
            } else if (session.mainAssetNetwork.isLiquid) {
                resources.getTextArray(
                    R.array.liquid_units_entries
                ).map { it.toString() }
            } else {
                denominationsUnitsIdentities
            }

            var selectedUnit = settings.unit

            var exchangeRates = listOf<String>()
            var exchangeRatesIdentities = listOf<Pricing>()

            var selectedPricing = settings.pricing

            try{
                val availableCurrencies = session.availableCurrencies()
                exchangeRates = availableCurrencies.map {
                    it.toString(context = requireContext(), R.string.id_s_from_s)
                }.let {
                    if(isDevelopmentOrDebug){
                        listOf("NULL (Debug)") + it
                    }else{
                        it
                    }
                }

                exchangeRatesIdentities = availableCurrencies.let {
                    if(isDevelopmentOrDebug){
                        listOf(Pricing("null", "null")) + it
                    }else{
                        it
                    }
                }
            }catch (e: Exception){
                countly.recordException(e)
            }


            val dialogBinding = DenominationExchangeDialogBinding.inflate(LayoutInflater.from(context))

            // Denomination
            dialogBinding.denomination.setOnItemClickListener { _, _, position, _ ->
                selectedUnit = denominationsUnitsIdentities[position]
            }

            dialogBinding.denomination.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    denominationsUnits
                )
            )

            dialogBinding.denomination.setText(selectedUnit.let {
                denominationsUnits.getOrNull(denominationsUnitsIdentities.indexOf(it)) ?: it
            }, false)

            // Exchange Rate
            dialogBinding.exchangeRate.setOnItemClickListener { _, _, position, _ ->
                selectedPricing = exchangeRatesIdentities[position]
            }

            dialogBinding.exchangeRate.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    exchangeRates
                )
            )
            dialogBinding.exchangeRate.setText(selectedPricing.toString(context = requireContext(), R.string.id_s_from_s), false)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_denomination__exchange_rate)
                .setView(dialogBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    getWalletViewModel().saveGlobalSettings(settings.copy(unit = selectedUnit, pricing = selectedPricing), onSuccess)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    companion object : KLogging()
}