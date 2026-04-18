package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionEvent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletAbiFlowRouteViewModel(
    greenWallet: GreenWallet,
    private val store: WalletAbiFlowStore,
    private val snapshotRepository: WalletAbiFlowSnapshotRepository,
    private val driver: FakeWalletAbiFlowDriver
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "WalletAbiFlow"
    override val isLoginRequired: Boolean = false

    val state: StateFlow<WalletAbiFlowState> = store.state

    init {
        _navData.value = NavData(
            title = "Wallet ABI",
            subtitle = greenWallet.name
        )
        store.outputs.onEach { output ->
            if (output is WalletAbiFlowOutput.PersistSnapshot) {
                val snapshot = output.snapshot
                if (snapshot == null) {
                    snapshotRepository.clear(greenWallet.id)
                } else {
                    snapshotRepository.save(greenWallet.id, snapshot)
                }
            }
        }.launchIn(viewModelScope)
        startFlow(greenWallet = greenWallet)
        bootstrap()
    }

    fun dispatch(intent: WalletAbiFlowIntent) {
        viewModelScope.launch {
            store.dispatch(intent)
        }
    }

    private fun startFlow(greenWallet: GreenWallet) {
        viewModelScope.launch {
            val requestContext = WalletAbiStartRequestContext(
                requestId = DEMO_REQUEST_ID,
                walletId = greenWallet.id
            )
            store.dispatch(
                WalletAbiFlowIntent.Start(
                    requestContext = requestContext
                )
            )
            val reviewPayload = driver.loadRequest(
                requestId = requestContext.requestId,
                walletId = requestContext.walletId
            )
            store.dispatch(
                WalletAbiFlowIntent.OnExecutionEvent(
                    WalletAbiExecutionEvent.RequestLoaded(
                        review = WalletAbiFlowReview(
                            requestContext = requestContext,
                            title = reviewPayload.title,
                            message = reviewPayload.message,
                            accounts = reviewPayload.accounts.map { account ->
                                WalletAbiAccountOption(
                                    accountId = account.accountId,
                                    name = account.name
                                )
                            },
                            selectedAccountId = reviewPayload.selectedAccountId,
                            approvalTarget = when (reviewPayload.approvalTarget.kind) {
                                "jade" -> WalletAbiApprovalTarget.Jade(
                                    deviceName = reviewPayload.approvalTarget.deviceName,
                                    deviceId = reviewPayload.approvalTarget.deviceId
                                )

                                else -> WalletAbiApprovalTarget.Software
                            }
                        )
                    )
                )
            )
        }
    }

    companion object {
        const val DEMO_REQUEST_ID = "wallet-abi-demo-request"
    }
}
