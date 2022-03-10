package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.blockstream.gdk.params.ReconnectHintParams
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.snackbar
import com.google.android.material.snackbar.Snackbar
import mu.KLogging

abstract class WalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    abstract val wallet: Wallet
    lateinit var session: GreenSession

    private var networkSnackbar: Snackbar? = null

    override fun updateToolbar() {
        super.updateToolbar()
        if (isSessionAndWalletRequired()) {
            // Prevent showing network icon when the title is empty
            if(toolbar.title.isNotBlank() || !title.isNullOrBlank()) {
                toolbar.setLogo(wallet.getIcon())
                session.hwWallet?.device?.let {
                    toolbar.setBubble(
                        ContextCompat.getDrawable(
                            requireContext(),
                            it.getIcon()
                        )
                    )
                }
            }
        }
    }

    // Guard initialization code for fragments that requires a connected session
    abstract fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Recovery intro screen is reused in onBoarding
        // where we don't have a session yet
        // Skip initializing the WalletViewModel as it doesn't exists
        if(isSessionAndWalletRequired()){
            session = sessionManager.getWalletSession(wallet)

            // Assuming we are in v4 codebase flow
            if (isLoggedInRequired() && !session.isConnected) {
                // If session is not initialized, avoid getting the ViewModel as can use GreenSession
                // without being properly initialized and can lead to a crash
                if (session.isInitialized) {
                    getWalletViewModel().logout(AbstractWalletViewModel.LogoutReason.TIMEOUT)
                } else {
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

            getWalletViewModel().let{

                setupDeviceInteractionEvent(it.onDeviceInteractionEvent)

                it.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
                    consumableEvent.getContentIfNotHandledForType<AbstractWalletViewModel.WalletEvent.Logout>()?.let {
                        // If is hardware wallet, prefer going to intro
                        if (wallet.isHardware) {
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

        onViewCreatedGuarded(view, savedInstanceState)
    }

    open fun isLoggedInRequired(): Boolean = true
    open fun isSessionAndWalletRequired(): Boolean = true

    abstract fun getWalletViewModel(): AbstractWalletViewModel

    override fun getAppViewModel() : AppViewModel? {
        // Prevent initializing WalletViewModel if session is not initialized
        if(isSessionAndWalletRequired() && isLoggedInRequired() && !session.isInitialized) {
            return null
        }
        return getWalletViewModel()
    }

    override fun onResume() {
        super.onResume()

        // Recovery screens are reused in onboarding
        // where we don't have a session yet.
        if(isSessionAndWalletRequired()) {
            if (isLoggedInRequired() && !session.isConnected) {
                // If session is not initialized, avoid getting the ViewModel as can use GreenSession
                // without being properly initialized and can lead to a crash
                if (session.isInitialized) {
                    getWalletViewModel().logout(AbstractWalletViewModel.LogoutReason.TIMEOUT)
                } else {
                    navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dismiss network to avoid UI leaking to other fragments
        networkSnackbar?.dismiss()
    }

    companion object : KLogging()
}