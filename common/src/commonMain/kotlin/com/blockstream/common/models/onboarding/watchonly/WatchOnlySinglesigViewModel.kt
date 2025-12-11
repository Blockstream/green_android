package com.blockstream.common.models.onboarding.watchonly

import androidx.lifecycle.viewModelScope
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.WatchOnlyCredentialType
import com.blockstream.common.utils.WatchOnlyDetector
import com.blockstream.ui.events.Event
import com.blockstream.ui.sideeffects.SideEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Source
import okio.buffer
import okio.use
import org.koin.core.component.inject

abstract class WatchOnlySinglesigViewModelAbstract(val setupArgs: SetupArgs) : GreenViewModel() {
    override fun screenName(): String = "OnBoardWatchOnlySinglesig"

    override fun segmentation(): HashMap<String, Any>? =
        setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }
    abstract val isLiquid: StateFlow<Boolean>
    abstract val isLoginEnabled: StateFlow<Boolean>
    abstract val watchOnlyDescriptor: MutableStateFlow<String>
    abstract val isOutputDescriptors: MutableStateFlow<Boolean>
}

class WatchOnlySinglesigViewModel(setupArgs: SetupArgs) :
    WatchOnlySinglesigViewModelAbstract(setupArgs = setupArgs) {

    private val watchOnlyDetector: WatchOnlyDetector by inject()
    private var detectedNetwork: String? = null

    override val isLiquid: MutableStateFlow<Boolean> =
        MutableStateFlow(setupArgs.network?.isLiquid == true)

    override val watchOnlyDescriptor: MutableStateFlow<String> =
        MutableStateFlow("")

    override val isOutputDescriptors: MutableStateFlow<Boolean> =
        MutableStateFlow(setupArgs.network?.isLiquid == true)

    override val isLoginEnabled: StateFlow<Boolean>

    class LocalEvents {
        data class AppendWatchOnlyDescriptor(val value: String) : Event
        class ImportFile(val source: Source) : Event
    }

    class LocalSideEffects {
        object RequestCipher : SideEffect
    }

    init {
        watchOnlyDescriptor.onEach { input ->
            if (input.isNotBlank()) {
                val detectionResult = watchOnlyDetector.detect(input)

                if (input.contains("(") || detectionResult.credentialType == WatchOnlyCredentialType.CORE_DESCRIPTORS) {
                    isOutputDescriptors.value = true
                }

                detectionResult.network?.also {
                    detectedNetwork = it
                }
            }
        }.launchIn(this)

        isLoginEnabled = combine(
            watchOnlyDescriptor,
            onProgress
        ) { descriptor, onProgress ->
            !onProgress && descriptor.isNotBlank()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is LocalEvents.AppendWatchOnlyDescriptor -> {
                appendWatchOnlyDescriptor(event.value)
            }

            is LocalEvents.ImportFile -> {
                importFile(event.source)
            }

            is Events.Continue -> {
                createSinglesigWatchOnlyWallet()
            }
        }
    }

    private fun appendWatchOnlyDescriptor(vararg value: String) {
        watchOnlyDescriptor.value = watchOnlyDescriptor.value.trimMargin()
            .let { it + (if (it.isNotBlank()) "\n" else "") + value.joinToString("\n") }
    }

    private fun importFile(source: Source) {
        doAsync({
            val xpubs = mutableListOf<String>()
            source.use {
                it.buffer().use {
                    Json.parseToJsonElement(it.readUtf8()).jsonObject.also { json ->
                        val keys = json.keys

                        // Coldcard
                        keys.forEach { key ->
                            (json[key] as? JsonObject)?.also { inner ->
                                // Filter only supported account types
                                inner["name"]?.jsonPrimitive?.content?.also { name ->
                                    if (
                                        name == AccountType.BIP44_LEGACY.gdkType ||
                                        name == AccountType.BIP49_SEGWIT_WRAPPED.gdkType ||
                                        name == AccountType.BIP84_SEGWIT.gdkType ||
                                        name == AccountType.BIP86_TAPROOT.gdkType
                                    ) {
                                        ((inner["_pub"] as? JsonPrimitive)
                                            ?: (inner["xpub"] as? JsonPrimitive))?.content?.also { xpub ->
                                            xpubs += xpub
                                        }
                                    }
                                }
                            }
                        }

                        // Electrum
                        ((json["keystore"] as? JsonObject)?.get("xpub") as? JsonPrimitive)?.content?.also { xpub ->
                            xpubs += xpub
                        }
                    }
                }
            }

            if (xpubs.isEmpty()) {
                throw Exception("id_format_is_not_supported_or_no_data")
            }

            xpubs
        }, onSuccess = {
            appendWatchOnlyDescriptor(*it.toTypedArray())
        })
    }

    private fun createSinglesigWatchOnlyWallet() {
        val watchOnlyDescriptors =
            watchOnlyDescriptor.value.takeIf { it.isNotBlank() }?.split("|", "\n")
                ?.map { it.trim().trimIndent().trimMargin() }?.filter { it.isNotBlank() }
                ?.toSet()
                ?.toList()

        val watchOnlyCredentials = if (isOutputDescriptors.value) {
            WatchOnlyCredentials(
                coreDescriptors = watchOnlyDescriptors
            )
        } else {
            WatchOnlyCredentials(
                slip132ExtendedPubkeys = watchOnlyDescriptors
            )
        }

        val network = setupArgs.network ?: detectedNetwork?.let {
            when (it) {
                com.blockstream.common.gdk.data.Network.ElectrumMainnet -> session.networks.bitcoinElectrum
                com.blockstream.common.gdk.data.Network.ElectrumTestnet -> session.networks.testnetBitcoinElectrum
                com.blockstream.common.gdk.data.Network.ElectrumTestnetLiquid -> session.networks.testnetLiquidElectrum
                else -> session.networks.liquidElectrum
            }
        } ?: session.networks.bitcoinElectrum

        createNewWatchOnlyWallet(
            network = network,
            persistLoginCredentials = false,
            watchOnlyCredentials = watchOnlyCredentials
        )
    }
}