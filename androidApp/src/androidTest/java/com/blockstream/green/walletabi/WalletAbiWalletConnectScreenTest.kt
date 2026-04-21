package com.blockstream.green.walletabi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.models.walletabi.WalletAbiWalletConnectPreparingLook
import com.blockstream.compose.models.walletabi.WalletAbiWalletConnectScreenState
import com.blockstream.compose.screens.overview.WalletAbiTransactEntry
import com.blockstream.compose.screens.walletabi.WalletAbiWalletConnectScreenContent
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class WalletAbiWalletConnectScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun transactEntry_hides_demo_entry_in_non_development_build() {
        composeRule.setContent {
            GreenPreview {
                WalletAbiTransactEntry(
                    isDevelopment = false,
                    pendingSnapshot = null,
                    walletConnectCard = null,
                    hasPendingWalletConnectRequest = false,
                    onScanWalletConnect = {},
                    onPasteWalletConnect = {},
                    onOpenWalletConnect = {},
                    onOpenDemo = {},
                    onResumePending = {},
                )
            }
        }

        composeRule.onNodeWithTag("transact_wallet_connect_scan").assertIsDisplayed()
        composeRule.onNodeWithTag("transact_wallet_connect_paste").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("transact_wallet_abi_entry").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun walletConnectScreen_shows_preparing_review_state() {
        composeRule.setContent {
            GreenPreview {
                WalletAbiWalletConnectScreenContent(
                    screenState = WalletAbiWalletConnectScreenState.Preparing(
                        WalletAbiWalletConnectPreparingLook(
                            requestId = "wallet-abi-request-1",
                            peerName = "Simplicity Lending",
                            chainId = "walabi:testnet-liquid",
                        ),
                    ),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithTag("wallet_connect_preparing_review")
            .assertIsDisplayed()
            .assertTextContains("Preparing Wallet ABI review")
        composeRule.onNodeWithTag("wallet_connect_preparing_request")
            .assertIsDisplayed()
        composeRule.onNodeWithText("wallet-abi-request-1").assertIsDisplayed()
        composeRule.onNodeWithText("Simplicity Lending").assertIsDisplayed()
    }

    @Test
    fun walletConnectScreen_shows_pairing_state() {
        composeRule.setContent {
            GreenPreview {
                WalletAbiWalletConnectScreenContent(
                    screenState = WalletAbiWalletConnectScreenState.Pairing,
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithTag("wallet_connect_pairing")
            .assertIsDisplayed()
            .assertTextContains("Pairing WalletConnect")
        composeRule.onNodeWithText("Waiting for session proposal").assertIsDisplayed()
    }
}
