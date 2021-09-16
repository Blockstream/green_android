package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.snackbar
import com.google.android.material.snackbar.Snackbar
import com.greenaddress.Bridge
import mu.KLogging

abstract class WalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    abstract val wallet: Wallet
    lateinit var session: GreenSession

    private var networkSnackbar: Snackbar? = null

    // Guard initialization code for fragments that requires a connected session
    abstract fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Recovery intro screen is reused in onBoarding
        // where we don't have a session yet
        // Skip initializing the WalletViewModel as it doesn't exists
        if(isSessionAndWalletRequired()){
            session = sessionManager.getWalletSession(wallet)

            // Assuming we are in v4 codebase flow
            if (isLoggedInRequired() && !session.isConnected) {
                navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
                return
            }

            getWalletViewModel().let{

                setupDeviceInteractionEvent(it.onDeviceInteractionEvent)

                it.onNavigationEvent.observe(viewLifecycleOwner) { consumableEvent ->
                    consumableEvent.getContentIfNotHandledOrReturnNull()?.let {
                        // If is hardware wallet, prefer going to intro
                        if (wallet.isHardware) {
                            NavGraphDirections.actionGlobalIntroFragment()
                        } else {
                            NavGraphDirections.actionGlobalLoginFragment(wallet)
                        }.let { directions ->
                            navigate(directions.actionId, directions.arguments, isLogout = true)
                        }

                        when(it){
                            AbstractWalletViewModel.NavigationEvent.DISCONNECTED -> {
                                snackbar(R.string.id_unstable_internet_connection)
                            }
                            AbstractWalletViewModel.NavigationEvent.TIMEOUT -> {
                                snackbar(R.string.id_auto_logout_timeout_expired)
                            }
                            AbstractWalletViewModel.NavigationEvent.DEVICE_DISCONNECTED -> {
                                snackbar(R.string.id_your_device_was_disconnected)
                            }
                        }
                    }
                }

                it.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
                    consumableEvent?.getContentIfNotHandledOrReturnNull{ event ->
                        event == AbstractWalletViewModel.Event.DELETE_WALLET
                    }?.let {
                        popBackStack()
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
                                    session.reconnectHint()
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

        onViewCreatedGuarded(view, savedInstanceState)
    }

    open fun isLoggedInRequired(): Boolean = true
    open fun isSessionAndWalletRequired(): Boolean = true

    abstract fun getWalletViewModel(): AbstractWalletViewModel

    override fun onResume() {
        super.onResume()

        // Recovery screens are reused in onboarding
        // where we don't have a session yet.
        if(isSessionAndWalletRequired()) {
            if (isLoggedInRequired() && !session.isConnected) {
                getWalletViewModel().logout(AbstractWalletViewModel.NavigationEvent.TIMEOUT)
            }
        }
    }

    companion object : KLogging()
}