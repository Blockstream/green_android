package com.blockstream.green

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.database.wallet.LoginCredentials
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.PinData
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.models.MainViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenApp
import com.blockstream.compose.LocalActivity
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.services.TaskService
import com.blockstream.green.utils.DeepLinkHandler
import com.blockstream.green.utils.Loggable
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class GreenActivity : AppCompatActivity() {
    private val appInfo by inject<AppInfo>()
    private val gdk: Gdk by inject()
    private val countly: CountlyAndroid by inject()
    private val database: Database by inject()
    private val sessionManager: SessionManager by inject()
    private val settingsManager by inject<SettingsManager>()

    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        FileKit.init(this)

        enableEdgeToEdge()
        setContent {
            GreenChrome()
            GreenTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    GreenApp(mainViewModel = mainViewModel, modifier = Modifier.compatTestTagsAsResourceId())
                }
            }
        }

        if (appInfo.isDevelopmentOrDebug) {
            // On development flavor, you can change settings using intent data, useful for UI tests
            intent?.let {
                if (it.hasExtra(ENABLE_TESTNET)) {
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            testnet = it.getBooleanExtra(ENABLE_TESTNET, false)
                        )
                    )
                }

                if (it.hasExtra(ENABLE_TOR)) {
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            tor = it.getBooleanExtra(ENABLE_TOR, false)
                        )
                    )
                }

                if (it.hasExtra(PROXY_URL)) {
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            proxyUrl = it.getStringExtra(PROXY_URL)
                        )
                    )
                }

                if (it.hasExtra(PERSONAL_ELECTRUM_SERVER)) {
                    val electrumServer = it.getStringExtra(PERSONAL_ELECTRUM_SERVER);
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            electrumNode = !electrumServer.isNullOrBlank(),
                            personalBitcoinElectrumServer = electrumServer,
                            personalLiquidElectrumServer = electrumServer,
                            personalTestnetElectrumServer = electrumServer,
                            personalTestnetLiquidElectrumServer = electrumServer,
                        )
                    )
                }

                if (it.hasExtra(REGISTER_NETWORK_ID) && it.hasExtra(REGISTER_NETWORK_HOSTNAME)) {
                    gdk.registerCustomNetwork(
                        it.getStringExtra(REGISTER_NETWORK_ID) ?: "", it.getStringExtra(
                            REGISTER_NETWORK_HOSTNAME
                        ) ?: ""
                    )
                }

                if (it.hasExtra(ADD_WALLET)) {
                    lifecycleScope.launch {
                        val json = Json.parseToJsonElement(
                            String(
                                Base64.decode(
                                    it.getStringExtra(ADD_WALLET)!!, Base64.DEFAULT
                                )!!
                            )
                        )

                        val walletJson = json.jsonObject["wallet"]!!.jsonObject
                        val network = walletJson["network"]!!.jsonPrimitive.content
                        val name = walletJson["name"]?.jsonPrimitive?.content

                        val wallet = GreenWallet.createWallet(
                            name = name ?: network.replaceFirstChar { n -> n.titlecase() },
                            activeNetwork = network,
                            isTestnet = Network.isTestnet(network)
                        ).also {
                            database.insertWallet(it)
                        }

                        json.jsonObject["login_credentials"]?.jsonObject?.let { loginCredentialsJson ->

                            val pinData = loginCredentialsJson["pin_data"]!!.jsonObject.let {
                                PinData(
                                    encryptedData = it["encrypted_data"]!!.jsonPrimitive.content,
                                    pinIdentifier = it["pin_identifier"]!!.jsonPrimitive.content,
                                    salt = it["salt"]!!.jsonPrimitive.content,
                                )
                            }

                            database.replaceLoginCredential(
                                LoginCredentials(
                                    wallet_id = wallet.id,
                                    credential_type = CredentialType.PIN_PINDATA,
                                    network = network,
                                    pin_data = pinData,
                                    keystore = null,
                                    encrypted_data = null,
                                    counter = 0
                                )
                            )
                        }
                    }
                }
            }
        }

        handleIntent(intent)

        // Start TaskService
        try {
            // Prefer starting the service here instead of GreenApplication as Android may restart the App
            // and this leads to BackgroundServiceStartNotAllowedException be thrown
            startService(Intent(applicationContext, TaskService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        countly.onStart(this)
    }

    override fun onStop() {
        super.onStop()
        countly.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        countly.onConfigurationChanged(newConfig)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logger.d { "onNewIntent called with: ${intent.data}" }
        setIntent(intent) // Important: Update the activity's intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        logger.d { "handleIntent called with action: ${intent?.action}, data: ${intent?.data}" }

        // Handle blockstream:// scheme with DeepLinkHandler
        if (intent?.action == Intent.ACTION_VIEW && intent.data?.scheme == "blockstream") {
            logger.d { "Handling blockstream:// deep link" }
            if (DeepLinkHandler.handleDeepLink(intent.data, mainViewModel)) {
                return
            }
        }

        if (intent?.action == Intent.ACTION_VIEW && intent.data?.toString()
                ?.let { it.contains("/jade/setup") || it.contains("/j/s") } == true
        ) {
            mainViewModel.postEvent(Events.NavigateTo(NavigateDestinations.DeviceList(isJade = true)))
        } else if (intent?.action == OPEN_WALLET) {

            intent.getStringExtra(WALLET)?.let { GreenJson.json.decodeFromString<GreenWallet>(it) }?.let { wallet ->
                mainViewModel.navigate(
                    wallet = wallet, deviceId = intent.getStringExtra(DEVICE_ID)
                )
            }
        } else if (intent?.action == HIDE_AMOUNTS) {
            settingsManager.saveApplicationSettings(
                settingsManager.getApplicationSettings().copy(hideAmounts = true)
            )
        } else {
            // Handle Uri (BIP-21 or lightning)
            intent?.data?.let {
                sessionManager.pendingUri.value = it.toString()
            }
        }
    }

    companion object : Loggable() {
        const val OPEN_WALLET = "OPEN_WALLET"
        const val WALLET = "WALLET"
        const val DEVICE_ID = "DEVICE_ID"

        const val ENABLE_TESTNET = "ENABLE_TESTNET"
        const val ENABLE_TOR = "ENABLE_TOR"
        const val PROXY_URL = "PROXY_URL"
        const val PERSONAL_ELECTRUM_SERVER = "PERSONAL_ELECTRUM_SERVER"

        const val REGISTER_NETWORK_ID = "REGISTER_NETWORK_ID"
        const val REGISTER_NETWORK_HOSTNAME = "REGISTER_NETWORK_HOSTNAME"

        const val ADD_WALLET = "ADD_WALLET"
        const val HIDE_AMOUNTS = "HIDE_AMOUNTS"
    }
}