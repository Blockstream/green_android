package com.blockstream.common.models.overview

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.gdk.data.Assets
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.toAmountLookOrNa
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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

    override val balancePrimary: StateFlow<String> = session.walletTotalBalance.map {
        session.ifConnected { it.toAmountLookOrNa(session) } ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    override val balanceSecondary: StateFlow<String>
        get() = TODO("Not yet implemented")

    override val assets: StateFlow<Assets> = session.walletAssets
    override val transactions: StateFlow<List<Transaction>> = session.walletTransactions

    class LocalEvents{
        object Refresh: Event

        object ReconnectFailedNetworks: Event
    }

    init {


        greenWalletFlow.filterNotNull().onEach {
            _navData.value = NavData(
                title = it.name
            )

//            menu = listOfNotNull(MenuEntry(
//                title = stringResource(R.string.id_logout),
//                iconRes = R.drawable.sign_out
//            ) {
//                viewModel.postEvent(Events.Logout(reason = LogoutReason.USER_ACTION))
//            }

        }.launchIn(this)

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.Refresh -> {
                session.refresh()
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