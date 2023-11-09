package com.blockstream.common.models.overview

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Assets
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow

abstract class WalletOverviewViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "WalletOverview"

    @NativeCoroutinesState
    abstract val balancePrimary: StateFlow<String>

    @NativeCoroutinesState
    abstract val balanceSecondary: StateFlow<String>

    @NativeCoroutinesState
    abstract val assets: StateFlow<Assets>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<List<Transaction>>
}

class WalletOverviewViewModel(greenWallet: GreenWallet) : WalletOverviewViewModelAbstract(greenWallet = greenWallet){
    override fun segmentation(): HashMap<String, Any> = countly.sessionSegmentation(session = session)

    override val balancePrimary: StateFlow<String>
        get() = TODO("Not yet implemented")
    override val balanceSecondary: StateFlow<String>
        get() = TODO("Not yet implemented")

    override val assets: StateFlow<Assets> = session.walletAssets
    override val transactions: StateFlow<List<Transaction>> = session.walletTransactions

    class LocalEvents{
        object Refresh: Event

        class ArchiveAccount(val account: Account): Event

        object ReconnectFailedNetworks: Event

    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.Refresh -> {
                session.refresh()
            }

            is LocalEvents.ArchiveAccount -> {
                updateAccount(account = event.account, isHidden = true)
            }
            is LocalEvents.ReconnectFailedNetworks -> {
                tryFailedNetworks()
            }
        }
    }

    private fun tryFailedNetworks() {
        session.tryFailedNetworks(hardwareWalletResolver = session.device?.let { device ->
            DeviceResolver.createIfNeeded(
                device.gdkHardwareWallet,
                // this // TODO enable hw interaction support to GreenViewModel
            )
        })
    }

}