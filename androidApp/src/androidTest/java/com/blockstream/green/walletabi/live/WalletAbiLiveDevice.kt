package com.blockstream.green.walletabi.live

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.LogoutReason
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Utxo
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.managers.SessionManager
import com.blockstream.domain.wallet.NewWalletUseCase
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.green.GreenActivity
import com.blockstream.data.walletabi.request.WalletAbiAndroidDemoRequestOverrideStore
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectSnapshotStore
import java.io.File
import java.net.URL
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

class WalletAbiLiveDevice(
    private val instrumentation: android.app.Instrumentation = InstrumentationRegistry.getInstrumentation(),
) {
    private val device = UiDevice.getInstance(instrumentation)
    private val targetContext = instrumentation.targetContext
    private val targetPackageName = targetContext.packageName
    private val logTag = "WalletAbiLiveDevice"
    private var currentWallet: GreenWallet? = null
    private var currentPin: String? = null
    private val compatibilitySelectors = listOf(
        By.text("Android App Compatibility"),
        By.textContains("App Compatibility"),
    )
    private val compatibilityButtons = listOf("Got it", "OK", "Continue", "Done")
    private val upgradeSelectors = listOf(
        By.text("Green is now the Blockstream app"),
        By.textContains("Blockstream app"),
    )
    private val upgradeButtons = listOf("Get Started", "Continue")

    fun unlockWalletToOverview(
        pin: String,
        walletIndex: Int = 0,
        minimumLiquidFundingBalance: Long = DEFAULT_MINIMUM_FUNDING_SAT
    ): GreenWallet {
        require(pin.length == 6) { "Expected a 6-digit wallet PIN" }

        val (wallet, fundingConfirmed) = runBlocking {
            ensureWallet(
                index = walletIndex,
                pin = pin,
                requireLiquidFunding = true,
                minimumLiquidFundingBalance = minimumLiquidFundingBalance
            )
        }
        currentWallet = wallet
        currentPin = pin

        resetWalletConnectPersistentState(wallet)

        instrumentation.uiAutomation.grantRuntimePermission(
            targetPackageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        instrumentation.waitForIdleSync()
        device.pressHome()
        SystemClock.sleep(POLL_INTERVAL_MS)

        targetContext.startActivity(
            Intent(GreenActivity.OPEN_WALLET).apply {
                setClassName(targetPackageName, GreenActivity::class.java.name)
                putExtra(GreenActivity.WALLET, GreenJson.json.encodeToString(wallet))
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        enterPinUntilOverview(pin)
        if (!fundingConfirmed) {
            waitForLiquidFunding(
                wallet = wallet,
                pin = pin,
                minimumLiquidFundingBalance = minimumLiquidFundingBalance
            )
        }
        return wallet
    }

    fun recipientConfidentialAddress(
        pin: String,
        walletIndex: Int = 1
    ): String {
        return runBlocking {
            val wallet = ensureWallet(
                index = walletIndex,
                pin = pin,
                requireLiquidFunding = false,
                minimumLiquidFundingBalance = 0L
            ).first
            val session = walletSession(wallet)
            if (!session.isConnected) {
                loginLiquidOnly(wallet = wallet, pin = pin, session = session)
            }
            val liquidAccount = defaultLiquidSinglesigAccount(session)
            val address = session.getReceiveAddressAsString(liquidAccount)
            disconnectSession(wallet = wallet, session = session)
            address
        }
    }

    fun injectRequestEnvelope(requestEnvelope: String) {
        WalletAbiAndroidDemoRequestOverrideStore.writeRequestEnvelope(
            context = targetContext,
            requestEnvelope = requestEnvelope
        )
    }

    fun openTransactTab() {
        var transactTab = findBySelector(ResourceSelector("bottom_nav_transact"))
            ?: findBySelector(TextSelector("Transact"))

        if (transactTab == null && currentWallet != null) {
            reopenWalletOverview()
            transactTab = findBySelector(ResourceSelector("bottom_nav_transact"))
                ?: findBySelector(TextSelector("Transact"))
        }

        (transactTab ?: waitForAnyObject(
            selectors = listOf(
                ResourceSelector("bottom_nav_transact"),
                TextSelector("Transact"),
            ),
            context = "waiting for the wallet overview shell"
        )).click()
        device.waitForIdle()
        waitForAnyObject(
            selectors = listOf(
                ResourceSelector("transact_wallet_abi_entry"),
                TextSelector("Open Wallet ABI demo"),
            ),
            context = "waiting for the Wallet ABI demo entry"
        )
    }

    fun openWalletAbiDemo() {
        waitForAnyObject(
            selectors = listOf(
                ResourceSelector("transact_wallet_abi_entry"),
                TextSelector("Open Wallet ABI demo"),
            ),
            context = "waiting for the Wallet ABI demo entry"
        ).click()
        device.waitForIdle()
        waitForAnyObject(
            selectors = listOf(
                ResourceSelector("wallet_abi_flow_request_title"),
                ResourceSelector("wallet_abi_flow_loading"),
                TextSelector("Review"),
                TextSelector("Wallet ABI"),
            ),
            context = "waiting for the Wallet ABI flow"
        )
    }

    fun approveWalletAbiRequest(
        timeoutMs: Long = REQUEST_APPROVAL_TIMEOUT_MS,
    ): String {
        val initialState = waitForWalletConnectState(
            targetStates = setOf(
                WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
                WalletConnectHarnessUiState.TERMINAL,
            ),
            timeoutMs = timeoutMs,
            context = "waiting for Wallet ABI transaction approval state",
        )
        if (initialState == WalletConnectHarnessUiState.TERMINAL) {
            findWalletAbiSuccessTxHash()?.let { return it }
        }

        val deadline = SystemClock.uptimeMillis() + timeoutMs

        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            if (dismissUpgradeOverlay()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            failIfWalletAbiError("waiting for the Wallet ABI approve action")

            findWalletAbiSuccessTxHash()?.let { return it }

            findBySelector(ResourceSelector("wallet_abi_flow_approve_action"))?.let { approveButton ->
                tapObject(approveButton)
                return waitForWalletAbiSuccessTxHash()
            }

            findBySelector(TextSelector("Approve request"))?.let { approveButton ->
                tapObject(approveButton)
                return waitForWalletAbiSuccessTxHash()
            }

            findBySelector(TextSelector("Approve with Jade"))?.let { approveButton ->
                tapObject(approveButton)
                return waitForWalletAbiSuccessTxHash()
            }

            SystemClock.sleep(POLL_INTERVAL_MS)
        }

        failIfWalletAbiError("waiting for the Wallet ABI approve action")
        error(
            "Expected one of ${
                listOf(
                    ResourceSelector("wallet_abi_flow_approve_action"),
                    TextSelector("Approve request"),
                    TextSelector("Approve with Jade"),
                ).joinToString()
            } while waiting for the Wallet ABI approve action"
        )
    }

    fun waitForTransactionConfirmation(
        wallet: GreenWallet,
        pin: String,
        txHash: String,
        timeoutMs: Long = CONFIRMATION_TIMEOUT_MS
    ) {
        runBlocking {
            val session = walletSession(wallet)
            if (!session.isConnected) {
                loginLiquidOnly(wallet = wallet, pin = pin, session = session)
            }
            val liquidAccount = defaultLiquidSinglesigAccount(session)
            val deadline = SystemClock.uptimeMillis() + timeoutMs

            while (SystemClock.uptimeMillis() < deadline) {
                val transaction = session.getTransactions(
                    account = liquidAccount,
                    params = TransactionParams(
                        subaccount = liquidAccount.pointer,
                        limit = CONFIRMATION_LOOKUP_LIMIT
                    )
                ).transactions.firstOrNull { tx ->
                    tx.txHash.equals(txHash, ignoreCase = true)
                }

                if ((transaction?.blockHeight ?: 0L) > 0L) {
                    return@runBlocking
                }

                SystemClock.sleep(POLL_INTERVAL_MS)
            }

            error("Expected Wallet ABI transaction $txHash to confirm on Liquid testnet")
        }
    }

    fun waitForTransactionSeen(
        wallet: GreenWallet,
        pin: String,
        txHash: String,
        timeoutMs: Long = TRANSACTION_LOOKUP_TIMEOUT_MS
    ) {
        runBlocking {
            val session = walletSession(wallet)
            if (!session.isConnected) {
                loginLiquidOnly(wallet = wallet, pin = pin, session = session)
            }
            val liquidAccount = defaultLiquidSinglesigAccount(session)
            val deadline = SystemClock.uptimeMillis() + timeoutMs

            while (SystemClock.uptimeMillis() < deadline) {
                val transaction = session.getTransactions(
                    account = liquidAccount,
                    params = TransactionParams(
                        subaccount = liquidAccount.pointer,
                        limit = CONFIRMATION_LOOKUP_LIMIT
                    )
                ).transactions.firstOrNull { tx ->
                    tx.txHash.equals(txHash, ignoreCase = true)
                }

                if (transaction != null) {
                    return@runBlocking
                }

                SystemClock.sleep(POLL_INTERVAL_MS)
            }

            error("Expected Wallet ABI transaction $txHash to appear in Liquid testnet history")
        }
    }

    fun reopenWalletOverview() {
        val wallet = currentWallet ?: error("Expected unlockWalletToOverview() before reopening the wallet")
        val pin = currentPin ?: error("Expected unlockWalletToOverview() to capture the live wallet PIN")
        targetContext.startActivity(
            Intent(GreenActivity.OPEN_WALLET).apply {
                setClassName(targetPackageName, GreenActivity::class.java.name)
                putExtra(GreenActivity.WALLET, GreenJson.json.encodeToString(wallet))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        device.waitForIdle()
        enterPinUntilOverview(pin)
    }

    fun openWalletConnectUri(pairingUri: String) {
        resetWalletConnectUiState()
        if (tryOpenWalletConnectViaPaste(pairingUri)) {
            return
        }

        targetContext.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(pairingUri)).apply {
                setClassName(targetPackageName, GreenActivity::class.java.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        device.waitForIdle()

        waitForWalletConnectState(
            targetStates = setOf(
                WalletConnectHarnessUiState.CONNECTION_APPROVAL,
                WalletConnectHarnessUiState.CONNECTED_IDLE,
                WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
            ),
            timeoutMs = WAIT_TIMEOUT_MS,
            context = "waiting for the WalletConnect session to appear after ACTION_VIEW",
        )
    }

    private fun tryOpenWalletConnectViaPaste(
        pairingUri: String,
    ): Boolean {
        if (!hasOverviewShell() && currentWallet != null) {
            reopenWalletOverview()
        }
        if (!hasOverviewShell()) {
            return false
        }

        findObject("bottom_nav_transact")?.also(::tapObject)
            ?: device.findObject(By.text("Transact"))?.also(::tapObject)
            ?: return false
        device.waitForIdle()

        val pasteButton = waitForWalletConnectEntryAction(
            resourceId = "transact_wallet_connect_paste",
            label = "Paste WalletConnect",
            timeoutMs = SHORT_WAIT_TIMEOUT_MS,
        ) ?: return false

        setClipboardText(pairingUri)
        tapObject(pasteButton)
        device.waitForIdle()
        runCatching {
            waitForWalletConnectState(
                targetStates = setOf(
                    WalletConnectHarnessUiState.CONNECTION_APPROVAL,
                    WalletConnectHarnessUiState.CONNECTED_IDLE,
                    WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
                ),
                timeoutMs = SHORT_WAIT_TIMEOUT_MS,
                context = "waiting briefly for the WalletConnect session to appear after paste",
            )
        }.onSuccess {
            return true
        }

        Log.w(logTag, "WalletConnect paste path did not react to the first tap, retrying with a direct device click")
        val retryPasteButton = findObject("transact_wallet_connect_paste")
            ?: device.findObject(By.text("Paste WalletConnect"))
            ?: return false
        val pasteBounds = retryPasteButton.visibleBounds
        check(device.click(pasteBounds.centerX(), pasteBounds.centerY())) {
            "Direct device tap failed for WalletConnect paste"
        }
        device.waitForIdle()
        return runCatching {
            waitForWalletConnectState(
                targetStates = setOf(
                    WalletConnectHarnessUiState.CONNECTION_APPROVAL,
                    WalletConnectHarnessUiState.CONNECTED_IDLE,
                    WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
                ),
                timeoutMs = WAIT_TIMEOUT_MS,
                context = "waiting for the WalletConnect session to appear after paste",
            )
            true
        }.getOrElse {
            Log.w(logTag, "WalletConnect paste path did not reach a visible session state", it)
            false
        }
    }

    private fun setClipboardText(text: String) {
        val clipboardManager = targetContext.getSystemService(ClipboardManager::class.java)
            ?: error("Expected clipboard manager while preparing WalletConnect pairing")
        clipboardManager.setPrimaryClip(ClipData.newPlainText("walletconnect", text))
    }

    fun approveWalletConnectSession() {
        val preApprovalState = waitForWalletConnectState(
            targetStates = setOf(
                WalletConnectHarnessUiState.CONNECTION_APPROVAL,
                WalletConnectHarnessUiState.CONNECTED_IDLE,
                WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
            ),
            timeoutMs = WAIT_TIMEOUT_MS,
            context = "waiting for a WalletConnect session state before approval",
        )
        if (preApprovalState != WalletConnectHarnessUiState.CONNECTION_APPROVAL) {
            return
        }

        val approveButton = waitForAnyObject(
            selectors = listOf(
                ResourceSelector("wallet_connect_connection_approve"),
                TextSelector("Approve session"),
            ),
            context = "waiting for the WalletConnect approve action"
        )
        val approveBounds = approveButton.visibleBounds
        tapObject(approveButton)

        val approvalAdvanced = runCatching {
            waitForWalletConnectState(
                targetStates = setOf(
                    WalletConnectHarnessUiState.OVERVIEW,
                    WalletConnectHarnessUiState.CONNECTED_IDLE,
                    WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
                ),
                timeoutMs = SHORT_WAIT_TIMEOUT_MS,
                context = "waiting for WalletConnect session approval to advance",
            )
        }.isSuccess

        if (approvalAdvanced) {
            return
        }

        Log.w(logTag, "WalletConnect approval did not advance after the first tap, retrying once with a direct device click")
        check(device.click(approveBounds.centerX(), approveBounds.centerY())) {
            "Direct device tap failed for WalletConnect session approval"
        }
        device.waitForIdle()

        runCatching {
            waitForWalletConnectState(
                targetStates = setOf(
                    WalletConnectHarnessUiState.OVERVIEW,
                    WalletConnectHarnessUiState.CONNECTED_IDLE,
                    WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
                ),
                timeoutMs = SHORT_WAIT_TIMEOUT_MS,
                context = "waiting for WalletConnect session approval to advance after retry",
            )
        }.onFailure { throwable ->
            Log.w(logTag, "WalletConnect approval progressed without a stable post-click UI state", throwable)
        }
    }

    private fun resetWalletConnectUiState() {
        val deadline = SystemClock.uptimeMillis() + WAIT_TIMEOUT_MS

        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }

            when (currentUiState()) {
                WalletConnectHarnessUiState.TERMINAL -> {
                    findObject("wallet_abi_flow_terminal_dismiss_action")?.click()
                        ?: device.findObject(By.text("Done"))?.click()
                        ?: device.findObject(By.text("Dismiss"))?.click()
                    device.waitForIdle()
                }

                WalletConnectHarnessUiState.CONNECTED_IDLE -> {
                    findObject("wallet_connect_disconnect")?.click()
                    device.waitForIdle()
                }

                WalletConnectHarnessUiState.CONNECTION_APPROVAL -> {
                    findObject("wallet_connect_connection_reject")?.click()
                        ?: device.findObject(By.text("Reject session"))?.click()
                    device.waitForIdle()
                }

                WalletConnectHarnessUiState.TRANSACTION_APPROVAL -> {
                    findObject("wallet_abi_flow_reject_action")?.click()
                        ?: device.findObject(By.text("Reject request"))?.click()
                    device.waitForIdle()
                }

                WalletConnectHarnessUiState.OVERVIEW -> {
                    findObject("bottom_nav_transact")?.click()
                        ?: device.findObject(By.text("Transact"))?.click()
                    device.waitForIdle()
                    val walletConnectCard = findObject("transact_wallet_connect_request_card")
                        ?: findObject("transact_wallet_connect_card")
                    if (walletConnectCard != null) {
                        walletConnectCard.click()
                        device.waitForIdle()
                    } else {
                        return
                    }
                }

                WalletConnectHarnessUiState.UNKNOWN -> {
                    device.pressBack()
                    device.waitForIdle()
                }
            }

            SystemClock.sleep(POLL_INTERVAL_MS)
        }
    }

    private fun currentUiState(): WalletConnectHarnessUiState {
        return when {
            findObject("wallet_abi_flow_terminal_dismiss_action") != null ||
                device.findObject(By.text("Done")) != null ||
                device.findObject(By.text("Dismiss")) != null -> WalletConnectHarnessUiState.TERMINAL

            findObject("wallet_connect_connection_approve") != null ||
                findObject("wallet_connect_connection_reject") != null ||
                findObject("wallet_connect_connection_approval") != null ||
                device.findObject(By.text("Approve session")) != null -> WalletConnectHarnessUiState.CONNECTION_APPROVAL

            findObject("wallet_abi_flow_approve_action") != null ||
                findObject("wallet_abi_flow_reject_action") != null ||
                device.findObject(By.text("Approve request")) != null ||
                device.findObject(By.text("Reject request")) != null ||
                device.findObject(By.text("Approve with Jade")) != null -> WalletConnectHarnessUiState.TRANSACTION_APPROVAL

            findObject("wallet_connect_disconnect") != null ||
                findObject("wallet_connect_connected") != null -> WalletConnectHarnessUiState.CONNECTED_IDLE

            hasOverviewShell() -> WalletConnectHarnessUiState.OVERVIEW

            else -> WalletConnectHarnessUiState.UNKNOWN
        }
    }

    private fun waitForWalletConnectState(
        targetStates: Set<WalletConnectHarnessUiState>,
        timeoutMs: Long,
        context: String,
    ): WalletConnectHarnessUiState {
        val deadline = SystemClock.uptimeMillis() + timeoutMs

        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            if (dismissUpgradeOverlay()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            failIfWalletAbiError(context)

            val state = currentUiState()
            if (state in targetStates) {
                return state
            }

            when (state) {
                WalletConnectHarnessUiState.OVERVIEW -> {
                    findObject("bottom_nav_transact")?.also(::tapObject)
                        ?: device.findObject(By.text("Transact"))?.also(::tapObject)
                    device.waitForIdle()

                    val walletConnectCard = findObject("transact_wallet_connect_request_card")
                        ?: findObject("transact_wallet_connect_card")
                    walletConnectCard?.also(::tapObject)
                    device.waitForIdle()
                }

                WalletConnectHarnessUiState.CONNECTED_IDLE,
                WalletConnectHarnessUiState.TRANSACTION_APPROVAL,
                WalletConnectHarnessUiState.CONNECTION_APPROVAL -> Unit

                WalletConnectHarnessUiState.TERMINAL -> {
                    if (!targetStates.contains(WalletConnectHarnessUiState.TERMINAL)) {
                        dismissWalletAbiTerminalIfPresent()
                    }
                }

                WalletConnectHarnessUiState.UNKNOWN -> {
                    if (currentWallet != null) {
                        reopenWalletOverview()
                    }
                }
            }

            SystemClock.sleep(POLL_INTERVAL_MS)
        }

        error("$context (lastState=${currentUiState()} diagnostics=${walletConnectDiagnostics()})")
    }

    private fun walletConnectDiagnostics(): String {
        val visibleTexts = listOf(
            "Review WalletConnect session",
            "Approve session",
            "Reject session",
            "Waiting for WalletConnect",
            "Ready for requests",
            "Approve request",
            "Reject request",
            "No active WalletConnect session",
            "Paste WalletConnect",
            "Transact",
        ).filter { text ->
            device.findObject(By.text(text)) != null || device.findObject(By.textContains(text)) != null
        }

        val hierarchyFile = File(targetContext.cacheDir, "walletconnect-live-hierarchy.xml")
        runCatching { device.dumpWindowHierarchy(hierarchyFile) }
            .onFailure { throwable ->
                Log.w(logTag, "Unable to dump WalletConnect hierarchy", throwable)
            }

        return "visibleTexts=${visibleTexts.joinToString()} hierarchy=${hierarchyFile.absolutePath}"
    }

    fun dismissWalletAbiTerminal() {
        waitForAnyObject(
            selectors = listOf(
                ResourceSelector("wallet_abi_flow_terminal_dismiss_action"),
                TextSelector("Done"),
                TextSelector("Dismiss"),
            ),
            context = "waiting for the Wallet ABI terminal dismiss action"
        ).click()
        device.waitForIdle()
    }

    fun dismissWalletAbiTerminalIfPresent(): Boolean {
        val dismissAction = listOf(
            findObject("wallet_abi_flow_terminal_dismiss_action"),
            device.findObject(By.text("Done")),
            device.findObject(By.text("Dismiss")),
        ).firstOrNull()
            ?: return false

        dismissAction.click()
        device.waitForIdle()
        return true
    }

    private fun enterPinUntilOverview(pin: String) {
        val deadline = SystemClock.uptimeMillis() + UNLOCK_TIMEOUT_MS
        var enteredDigits = 0

        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            if (dismissUpgradeOverlay()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            if (hasOverviewShell()) {
                return
            }

            if (enteredDigits < pin.length) {
                findPinDigit(pin[enteredDigits].toString())?.also { digit ->
                    digit.click()
                    enteredDigits += 1
                    SystemClock.sleep(POLL_INTERVAL_MS)
                    continue
                }
            }

            failIfWalletAbiError("unlocking the wallet to overview")
            SystemClock.sleep(POLL_INTERVAL_MS)
        }

        error("Expected wallet overview shell after unlocking the live wallet")
    }

    private fun findPinDigit(digit: String): UiObject2? {
        return device.findObject(By.res(targetPackageName, digit))
            ?: device.findObject(By.res(digit))
            ?: device.findObject(By.text(digit))
    }

    private fun hasOverviewShell(): Boolean {
        return findBySelector(ResourceSelector("bottom_nav_transact")) != null ||
            findBySelector(TextSelector("Transact")) != null
    }

    private fun waitForAnyObject(
        selectors: List<Selector>,
        context: String,
        timeoutMs: Long = WAIT_TIMEOUT_MS
    ): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            if (dismissUpgradeOverlay()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            failIfWalletAbiError(context)
            selectors.firstNotNullOfOrNull(::findBySelector)?.also { return it }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        failIfWalletAbiError(context)
        error("Expected one of ${selectors.joinToString()} while $context")
    }

    private fun waitForWalletAbiSuccessTxHash(timeoutMs: Long = WAIT_TIMEOUT_MS): String {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            if (dismissUpgradeOverlay()) {
                SystemClock.sleep(POLL_INTERVAL_MS)
                continue
            }
            failIfWalletAbiError("waiting for Wallet ABI success")
            findWalletAbiSuccessTxHash()?.let { return it }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }

        failIfWalletAbiError("waiting for Wallet ABI success")
        error(
            "Expected one of ${
                listOf(
                    ResourceSelector("wallet_abi_flow_success_tx_hash"),
                    TextPrefixSelector("Transaction: "),
                ).joinToString()
            } while waiting for Wallet ABI success"
        )
    }

    private fun findWalletAbiSuccessTxHash(): String? {
        val txHashText = findBySelector(ResourceSelector("wallet_abi_flow_success_tx_hash"))
            ?.text
            ?.orEmpty()
            ?.takeIf(String::isNotBlank)
            ?: findBySelector(TextPrefixSelector("Transaction: "))
                ?.text
                ?.orEmpty()
                ?.takeIf(String::isNotBlank)
            ?: return null

        return txHashText.substringAfter("Transaction: ").trim().takeIf { txHash ->
            txHash.isNotBlank() && txHash != "unavailable"
        }
    }

    private fun findObject(resourceId: String): UiObject2? {
        return device.findObject(By.res(resourceId))
            ?: device.findObject(By.res(targetPackageName, resourceId))
    }

    private fun findBySelector(selector: Selector): UiObject2? {
        return when (selector) {
            is ResourceSelector -> findObject(selector.resourceId)
            is TextSelector -> device.findObject(By.text(selector.text))
            is TextPrefixSelector -> device.findObject(By.textStartsWith(selector.textPrefix))
        }
    }

    private fun waitForWalletConnectEntryAction(
        resourceId: String,
        label: String,
        timeoutMs: Long,
    ): UiObject2? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            findObject(resourceId)?.let { return it }
            device.findObject(By.text(label))?.let { return it }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    private fun tapObject(target: UiObject2) {
        runCatching {
            val bounds = target.visibleBounds
            check(device.click(bounds.centerX(), bounds.centerY())) {
                "Coordinate tap failed for ${target.resourceName ?: target.text ?: target.contentDescription}"
            }
        }.recoverCatching {
            target.click()
        }.getOrThrow()
        device.waitForIdle()
    }

    private fun tapFirstText(labels: List<String>): Boolean {
        repeat(3) {
            labels.forEach { label ->
                val target = device.findObject(By.text(label)) ?: return@forEach
                val tapped = runCatching {
                    tapObject(target)
                    true
                }.getOrDefault(false)
                if (tapped) {
                    return true
                }
            }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun failIfWalletAbiError(context: String) {
        val title = safeText(findObject("wallet_abi_flow_error_title"))
        val body = safeText(findObject("wallet_abi_flow_error"))
        if (tryDismissRecoverableWalletAbiError(title = title, body = body)) {
            return
        }
        if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
            error("$context: ${(title ?: "Wallet ABI error") + " " + (body ?: "")}".trim())
        }
    }

    private fun safeText(target: UiObject2?): String? {
        return try {
            target?.text
        } catch (_: StaleObjectException) {
            null
        }
    }

    private fun tryDismissRecoverableWalletAbiError(
        title: String?,
        body: String?,
    ): Boolean {
        val normalizedTitle = title.orEmpty().lowercase()
        val normalizedBody = body.orEmpty().lowercase()
        val isExpiredRequest = normalizedTitle.contains("wallet abi execution failed") &&
            normalizedBody.contains("request has expired")

        if (!isExpiredRequest) {
            return false
        }

        val dismissAction = findObject("wallet_abi_flow_terminal_dismiss_action")
            ?: device.findObject(By.text("Done"))
            ?: device.findObject(By.text("Dismiss"))
            ?: return false

        Log.d(logTag, "Dismissing recoverable Wallet ABI error: $title $body")
        dismissAction.click()
        device.waitForIdle()
        SystemClock.sleep(POLL_INTERVAL_MS)
        return true
    }

    private fun dismissCompatibilityDialog(): Boolean {
        return if (compatibilitySelectors.any(device::hasObject)) {
            tapFirstText(compatibilityButtons)
            device.waitForIdle()
            true
        } else {
            false
        }
    }

    private fun dismissUpgradeOverlay(): Boolean {
        return if (upgradeSelectors.any(device::hasObject)) {
            tapFirstText(upgradeButtons)
            device.waitForIdle()
            true
        } else {
            false
        }
    }

    private fun resetWalletConnectPersistentState(wallet: GreenWallet) {
        WalletAbiAndroidDemoRequestOverrideStore.clear(targetContext)

        runBlocking {
            val koin = GlobalContext.get()
            koin.get<WalletAbiFlowSnapshotRepository>().clear(wallet.id)
            koin.get<WalletAbiWalletConnectSnapshotStore>().clear(wallet.id)
        }

        clearWalletConnectSharedPreferences("wc_key_store")
        clearWalletConnectDatabase("WalletConnectV2.db")
        clearWalletConnectDatabase("WalletConnectAndroidCore.db")
    }

    private fun clearWalletConnectSharedPreferences(name: String) {
        targetContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        File(targetContext.applicationInfo.dataDir, "shared_prefs/$name.xml").delete()
    }

    private fun clearWalletConnectDatabase(name: String) {
        targetContext.deleteDatabase(name)
        val path = targetContext.getDatabasePath(name)
        if (path.exists()) {
            path.delete()
        }
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            File(path.parentFile, "${path.name}$suffix").delete()
        }
    }

    private suspend fun ensureWallet(
        index: Int,
        pin: String,
        requireLiquidFunding: Boolean,
        minimumLiquidFundingBalance: Long
    ): Pair<GreenWallet, Boolean> {
        val koin = GlobalContext.get()
        val database: Database = koin.get()
        val sessionManager: SessionManager = koin.get()
        val newWalletUseCase: NewWalletUseCase = koin.get()

        val existingWallets = database.getAllWallets()
            .filter { wallet -> wallet.isTestnet && !wallet.isHardware }
            .sortedBy { wallet -> wallet.id }
            .toMutableList()

        while (existingWallets.size <= index) {
            existingWallets += newWalletUseCase(
                session = sessionManager.getOnBoardingSession(),
                pin = pin,
                isTestnet = true
            )
        }

        val wallet = existingWallets[index]
        if (!requireLiquidFunding) {
            disconnectSession(wallet = wallet, session = walletSession(wallet))
            return wallet to false
        }

        val session = walletSession(wallet)
        if (!session.isConnected) {
            loginLiquidOnly(wallet = wallet, pin = pin, session = session)
        }

        val liquidAccount = defaultLiquidSinglesigAccount(session)
        val fundingConfirmed = hasMinimumLiquidFunding(session, liquidAccount, minimumLiquidFundingBalance)
        if (!fundingConfirmed) {
            val receiveAddress = session.getReceiveAddressAsString(liquidAccount)
            val faucetUrl = buildString {
                append("https://liquidtestnet.com/faucet?address=")
                append(URLEncoder.encode(receiveAddress, UTF_8.name()))
                append("&action=lbtc")
            }
            val faucetResponse = URL(faucetUrl).readText()
            check(!faucetResponse.contains("error", ignoreCase = true)) {
                "Liquid faucet request failed for live Wallet ABI smoke"
            }
        }

        disconnectSession(wallet = wallet, session = session)
        return wallet to fundingConfirmed
    }

    private fun waitForLiquidFunding(
        wallet: GreenWallet,
        pin: String,
        minimumLiquidFundingBalance: Long
    ) {
        runBlocking {
            val session = walletSession(wallet)
            if (!session.isConnected) {
                loginLiquidOnly(wallet = wallet, pin = pin, session = session)
            }

            val liquidAccount = defaultLiquidSinglesigAccount(session)
            val deadline = SystemClock.uptimeMillis() + FUNDING_TIMEOUT_MS
            while (SystemClock.uptimeMillis() < deadline) {
                if (hasMinimumLiquidFunding(session, liquidAccount, minimumLiquidFundingBalance)) {
                    disconnectSession(wallet = wallet, session = session)
                    return@runBlocking
                }
                SystemClock.sleep(POLL_INTERVAL_MS)
            }

            disconnectSession(wallet = wallet, session = session)
            error("Expected Liquid funding before running Wallet ABI live smoke")
        }
    }

    private suspend fun hasMinimumLiquidFunding(
        session: GdkSession,
        account: Account,
        minimumLiquidFundingBalance: Long
    ): Boolean {
        val confirmedPolicyUtxos: List<Utxo> = session.getUnspentOutputs(account)
            .unspentOutputsAsUtxo
            .values
            .flatten()
            .filter { utxo ->
                utxo.assetId == account.network.policyAsset &&
                    (utxo.blockHeight ?: 0L) > 0L &&
                    utxo.satoshi > 0L
            }

        val policyBalance = session.getBalance(account).policyAsset
        Log.d(
            logTag,
            "hasMinimumLiquidFunding wallet=${account.name} utxos=${confirmedPolicyUtxos.size} balance=$policyBalance"
        )
        return confirmedPolicyUtxos.isNotEmpty() && policyBalance >= minimumLiquidFundingBalance
    }

    private suspend fun loginLiquidOnly(
        wallet: GreenWallet,
        pin: String,
        session: GdkSession,
    ) {
        val database: Database = GlobalContext.get().get()
        val loginCredentials = database.getLoginCredentials(wallet.id).firstOrNull { credentials ->
            credentials.credential_type == CredentialType.PIN_PINDATA
        } ?: error("Expected PIN credentials for live wallet ${wallet.id}")

        val liquidNetwork = session.networkBy("electrum-testnet-liquid")
        val credentials = session.emergencyRestoreOfRecoveryPhrase(
            wallet = wallet,
            pin = pin,
            loginCredentials = loginCredentials,
        )

        session.loginWithMnemonic(
            isTestnet = wallet.isTestnet,
            wallet = wallet,
            loginCredentialsParams = LoginCredentialsParams.fromCredentials(credentials),
            initNetworks = listOf(liquidNetwork),
            initializeSession = false,
            isSmartDiscovery = false,
            isCreate = false,
            isRestore = false,
        )
    }

    private fun walletSession(wallet: GreenWallet): GdkSession {
        val sessionManager: SessionManager = GlobalContext.get().get()
        return sessionManager.getWalletSessionOrNull(wallet)
            ?: sessionManager.getWalletSessionOrCreate(wallet)
    }

    private fun defaultLiquidSinglesigAccount(session: GdkSession): Account {
        return Account(
            gdkName = "SegWit",
            pointer = 1L,
            type = AccountType.BIP84_SEGWIT,
        ).also { account ->
            account.setup(session = session, network = session.networkBy("electrum-testnet-liquid"))
        }
    }

    private fun disconnectSession(wallet: GreenWallet, session: GdkSession) {
        if (!session.isConnected) return
        session.disconnectAsync(reason = LogoutReason.USER_ACTION)
        val deadline = SystemClock.uptimeMillis() + DISCONNECT_TIMEOUT_MS
        while (session.isConnected && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        runBlocking {
            GlobalContext.get().get<WalletAbiFlowSnapshotRepository>().clear(wallet.id)
        }
    }

    companion object {
        const val TESTNET_POLICY_ASSET =
            "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
        private const val DEFAULT_MINIMUM_FUNDING_SAT = 5_000L
        private const val POLL_INTERVAL_MS = 1_000L
        private const val SHORT_WAIT_TIMEOUT_MS = 7_500L
        private const val WAIT_TIMEOUT_MS = 30_000L
        private const val REQUEST_APPROVAL_TIMEOUT_MS = 150_000L
        private const val UNLOCK_TIMEOUT_MS = 120_000L
        private const val DISCONNECT_TIMEOUT_MS = 15_000L
        private const val FUNDING_TIMEOUT_MS = 240_000L
        private const val TRANSACTION_LOOKUP_TIMEOUT_MS = 120_000L
        private const val CONFIRMATION_TIMEOUT_MS = 480_000L
        private const val CONFIRMATION_LOOKUP_LIMIT = 50
    }

    private sealed interface Selector
    private data class ResourceSelector(val resourceId: String) : Selector
    private data class TextSelector(val text: String) : Selector
    private data class TextPrefixSelector(val textPrefix: String) : Selector
    private enum class WalletConnectHarnessUiState {
        OVERVIEW,
        CONNECTION_APPROVAL,
        TRANSACTION_APPROVAL,
        CONNECTED_IDLE,
        TERMINAL,
        UNKNOWN,
    }
}
