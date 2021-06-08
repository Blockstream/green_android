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
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.snackbar
import com.google.android.material.snackbar.Snackbar
import com.greenaddress.Bridge
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging

abstract class WalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    abstract val wallet: Wallet
    lateinit var session: GreenSession

    private var networkSnackbar: Snackbar? = null

    // Protect the fragment/vm to continue initializing when fragment is finishing
    protected var isFinishingGuard: Boolean = false
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recovery screens are reused in onBoarding
        // where we don't have a session yet. Skip initializing the WalletViewModel as it doesn't exists
        if(!isSessionRequired()) {
            return
        }

        session = sessionManager.getWalletSession(wallet)

        // Assuming we are in v4 codebase flow
        if (isLoggedInRequired() && !session.isConnected) {
            navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
            isFinishingGuard = true
            return
        }

        getWalletViewModel()?.let{

            it.onNavigationEvent.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandledOrReturnNull()?.let {
                    if(Bridge.useGreenModule){
                        NavGraphDirections.actionGlobalLoginFragment(wallet).let {
                            navigate(it.actionId, it.arguments, isLogout = true)
                        }
                    }else{
                        Bridge.navigateToLogin(requireActivity(), wallet.id)
                    }
                }
            }

            it.onDeviceInteractionEvent.observe(viewLifecycleOwner) {  event ->
                event.getContentIfNotHandledOrReturnNull()?.let {
                    // KISS: It's not what v3 had done in the past, but it's simpler
                    snackbar(R.string.id_please_follow_the_instructions, Snackbar.LENGTH_LONG)
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

    open fun isLoggedInRequired(): Boolean = true
    open fun isSessionRequired(): Boolean = true

    abstract fun getWalletViewModel(): AbstractWalletViewModel?

    override fun onResume() {
        super.onResume()

        // Recovery screens are reused in onboarding
        // where we don't have a session yet.
        if(!isSessionRequired()) return

        if (isLoggedInRequired() && !session.isConnected) {
            getWalletViewModel()?.logout()
        }
    }

    companion object : KLogging()
}