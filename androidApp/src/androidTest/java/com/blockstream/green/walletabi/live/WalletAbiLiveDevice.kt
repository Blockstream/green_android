package com.blockstream.green.walletabi.live

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
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

    fun unlockWalletToOverview(
        pin: String,
        minimumLiquidFundingBalance: Long = DEFAULT_MINIMUM_FUNDING_SAT
    ): GreenWallet {
        require(pin.length == 6) { "Expected a 6-digit wallet PIN" }

        val (wallet, fundingConfirmed) = runBlocking {
            ensureWallet(
                index = 0,
                pin = pin,
                requireLiquidFunding = true,
                minimumLiquidFundingBalance = minimumLiquidFundingBalance
            )
        }
        currentWallet = wallet
        currentPin = pin

        WalletAbiAndroidDemoRequestOverrideStore.clear(targetContext)
        runBlocking {
            GlobalContext.get().get<WalletAbiFlowSnapshotRepository>().clear(wallet.id)
        }

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

    fun approveWalletAbiRequest(): String {
        waitForAnyObject(
            selectors = listOf(
                ResourceSelector("wallet_abi_flow_approve_action"),
                TextSelector("Approve request"),
                TextSelector("Approve with Jade"),
            ),
            context = "waiting for the Wallet ABI approve action"
        ).click()
        device.waitForIdle()

        val txHashText = waitForAnyObject(
            selectors = listOf(
                ResourceSelector("wallet_abi_flow_success_tx_hash"),
                TextPrefixSelector("Transaction: "),
            ),
            context = "waiting for Wallet ABI success"
        ).text.orEmpty()

        return txHashText.substringAfter("Transaction: ").trim().also { txHash ->
            check(txHash.isNotBlank() && txHash != "unavailable") {
                "Expected a real Wallet ABI transaction hash on success, got '$txHashText'"
            }
        }
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

    private fun enterPinUntilOverview(pin: String) {
        val deadline = SystemClock.uptimeMillis() + UNLOCK_TIMEOUT_MS
        var enteredDigits = 0

        while (SystemClock.uptimeMillis() < deadline) {
            if (dismissCompatibilityDialog()) {
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
            failIfWalletAbiError(context)
            selectors.firstNotNullOfOrNull(::findBySelector)?.also { return it }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        failIfWalletAbiError(context)
        error("Expected one of ${selectors.joinToString()} while $context")
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

    private fun failIfWalletAbiError(context: String) {
        val title = findObject("wallet_abi_flow_error_title")?.text
        val body = findObject("wallet_abi_flow_error")?.text
        if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
            error("$context: ${(title ?: "Wallet ABI error") + " " + (body ?: "")}".trim())
        }
    }

    private fun dismissCompatibilityDialog(): Boolean {
        return if (compatibilitySelectors.any(device::hasObject)) {
            compatibilityButtons.firstNotNullOfOrNull { label ->
                device.findObject(By.text(label))?.also(UiObject2::click)
            }
            device.waitForIdle()
            true
        } else {
            false
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
        private const val WAIT_TIMEOUT_MS = 30_000L
        private const val UNLOCK_TIMEOUT_MS = 120_000L
        private const val DISCONNECT_TIMEOUT_MS = 15_000L
        private const val FUNDING_TIMEOUT_MS = 240_000L
        private const val CONFIRMATION_TIMEOUT_MS = 480_000L
        private const val CONFIRMATION_LOOKUP_LIMIT = 50
    }

    private sealed interface Selector
    private data class ResourceSelector(val resourceId: String) : Selector
    private data class TextSelector(val text: String) : Selector
    private data class TextPrefixSelector(val textPrefix: String) : Selector
}
