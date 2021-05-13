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
import com.blockstream.green.ui.wallet.WalletViewModel
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = sessionManager.getWalletSession(wallet)

        if (isLoggedInRequired() && !session.isConnected) {
            navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
            return
        }

        getWalletViewModel()?.let{

            it.onDeviceInteractionEvent.observe(viewLifecycleOwner) {  event ->
                event.getContentIfNotHandledOrReturnNull()?.let {
                    // KISS: It's not what v3 had done in the past, but it's simpler
                    snackbar(R.string.id_please_follow_the_instructions, Snackbar.LENGTH_LONG)
                }
            }

            it.onNetworkEvent.observe(viewLifecycleOwner) { event ->
                if(event.loginRequired){
                    logger().info { "Logout from network event " }
                    logout()
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

    abstract fun getWalletViewModel(): WalletViewModel?

    override fun onResume() {
        super.onResume()
    }

    fun logout(){
        if(Bridge.useGreenModule){
            session.disconnectAsync()
            NavGraphDirections.actionGlobalLoginFragment(wallet).let {
                navigate(it.actionId, it.arguments, isLogout = true)
            }
        }else{
            Bridge.navigateToLogin(requireActivity(), wallet.id)
        }
    }

    companion object : KLogging()
}