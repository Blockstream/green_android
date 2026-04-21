package com.blockstream.green.walletabi.live

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletAbiWalletConnectLiveE2ETest {
    @Test
    fun approves_walletconnect_transfer_and_split_requests() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString(ARG_ENABLE_LIVE_E2E) == "true")

        val pairingUri = arguments.getString(ARG_PAIRING_URI)
            ?: error("Expected WalletConnect pairing URI in instrumentation args")
        val pin = arguments.getString(ARG_PIN) ?: DEFAULT_PIN
        val approvalCount = arguments.getString(ARG_APPROVAL_COUNT)?.toIntOrNull() ?: 2
        val walletIndex = arguments.getString(ARG_WALLET_INDEX)?.toIntOrNull() ?: 0

        val liveDevice = WalletAbiLiveDevice()
        val senderWallet = liveDevice.unlockWalletToOverview(pin = pin, walletIndex = walletIndex)
        liveDevice.openWalletConnectUri(pairingUri)
        liveDevice.approveWalletConnectSession()

        repeat(approvalCount) {
            val txHash = liveDevice.approveWalletAbiRequest()
            liveDevice.dismissWalletAbiTerminalIfPresent()
            liveDevice.waitForTransactionSeen(
                wallet = senderWallet,
                pin = pin,
                txHash = txHash
            )
        }
    }

    private companion object {
        const val ARG_ENABLE_LIVE_E2E = "walletAbiWalletConnectLive"
        const val ARG_PAIRING_URI = "walletAbiPairingUri"
        const val ARG_PIN = "walletAbiPin"
        const val ARG_APPROVAL_COUNT = "walletAbiApprovalCount"
        const val ARG_WALLET_INDEX = "walletAbiWalletIndex"
        const val DEFAULT_PIN = "111111"
    }
}
