package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.gdk.params.ReconnectHintParams
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.iconResource
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.login.LoginFragment
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

    open val toolbarIcon: Int?
        get() = null

    override fun updateToolbar() {
        super.updateToolbar()
        if (isSessionAndWalletRequired() && isSessionInitialized) {

            // Only show toolbar icon if it's overridden eg. add account flow
            toolbarIcon?.let { toolbar.setLogo(it) }

            // Prevent showing network icon when the title is empty
            if(toolbar.title.isNotBlank() || !title.isNullOrBlank()) {

                toolbar.setLogo(
                    toolbarIcon ?: wallet.iconResource(session)
                )

//                toolbar.setLogo(
//                    when {
//                        toolbarIcon != null -> {
//                            toolbarIcon
//                        }
//                        wallet.isBip39Ephemeral -> {
//                            R.drawable.ic_bip39_passphrase_24
//                        }
//                        session.isHardwareWallet -> {
//                            session.device!!.getIcon()
//                        }
//                        else -> null
//                    } ?: session.getIcon()
//                )

//                if(wallet.isWatchOnly){
//                    toolbar.setBubble(
//                        ContextCompat.getDrawable(
//                            requireContext(),
//                            R.drawable.ic_watch_18
//                        )
//                    )
//                }

                // BIP39 Passhphrase
//                if(wallet.isBip39Ephemeral){
//                    toolbar.logo = ContextCompat.getDrawable(
//                        requireContext(),
//                        R.drawable.ic_bip39_passphrase_24
//                    )
//                }

//                session.device?.let {
//                    // toolbar.subtitle = subtitle ?: it.name
//                    toolbar.setBubble(
//                        ContextCompat.getDrawable(
//                            requireContext(),
//                            it.getIcon()
//                        )
//                    )
//                }
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