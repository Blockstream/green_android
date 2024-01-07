package com.blockstream.common.models.add

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Asset
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.utils.randomChars
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class ExportLightningKeyViewModelAbstract(
    greenWallet: GreenWallet
) : AddAccountViewModelAbstract(greenWallet = greenWallet) {
    override fun screenName(): String = "ExportLightningKey"

    @NativeCoroutinesState
    abstract val bcurPart: StateFlow<String?>
}

@OptIn(ExperimentalStdlibApi::class)
class ExportLightningKeyViewModel(greenWallet: GreenWallet) :
    ExportLightningKeyViewModelAbstract(greenWallet = greenWallet) {
    private var privateKey: ByteArray? = null
    private var currentIndex = 0
    private var bcurParts = listOf<String>()

    private val _bcurPart: MutableStateFlow<String?> = MutableStateFlow(null)
    override val bcurPart: StateFlow<String?> = _bcurPart.asStateFlow()

    private var _lightningMnemonic: String? = null

    class LocalEvents {
        data class JadeBip8539Reply(val publicKey: String, val encrypted: String) : Event
    }

    class LocalSideEffects {
        object ScanQr : SideEffect
    }

    init {
        doAsync({
            session.jadeBip8539Request()
        }, onSuccess = {
            privateKey = it.first

            it.second.parts.also {
                _bcurPart.value = it.firstOrNull()
                bcurParts = it

                // Rotate qr codes
                if (it.size > 1) {
                    viewModelScope.coroutineScope.launch(context = logException(countly)) {
                        while (isActive) {
                            if (currentIndex >= bcurParts.size) {
                                currentIndex = 0
                            }
                            _bcurPart.value = bcurParts[currentIndex]
                            currentIndex++
                            delay(2000L)
                        }
                    }
                }
            }
        })

        bootstrap()
    }

    override fun handleEvent(event: Event) {

        when (event) {
            is LocalEvents.JadeBip8539Reply -> {
                handleReply(event.publicKey, event.encrypted)
            }

            is Events.Continue -> {
                postSideEffect(LocalSideEffects.ScanQr)
            }

            is AddAccountViewModelAbstract.LocalEvents.EnableLightningShortcut -> {
                _lightningMnemonic?.also {
                    enableLightningShortcut(lightningMnemonic = it)
                }
            }

            else -> {
                super.handleEvent(event)
            }
        }
    }

    private fun handleReply(publicKey: String, encrypted: String) {
        doAsync({
            val lightningMnemonic = session.jadeBip8539Reply(
                privateKey = privateKey!!,
                publicKey = publicKey.hexToByteArray(),
                encrypted = encrypted.hexToByteArray()
            )
            lightningMnemonic ?: throw Exception("id_decoding_error_try_again_by_scanning")
        }, onSuccess = { lightningMnemonic: String ->
            createLightning(lightningMnemonic)
        })
    }

    private fun createLightning(lightningMnemonic: String) {
        _lightningMnemonic = lightningMnemonic
        AccountType.LIGHTNING.also {
            createAccount(
                accountType = it,
                accountName = it.toString(),
                network = networkForAccountType(it, Asset.createEmpty(BTC_POLICY_ASSET)),
                mnemonic = lightningMnemonic,
                xpub = null
            )
        }
    }
}

class ExportLightningKeyViewModelPreview(greenWallet: GreenWallet) :
    ExportLightningKeyViewModelAbstract(greenWallet) {

    override val bcurPart: StateFlow<String?> = MutableStateFlow(randomChars(128))

    companion object {
        fun preview() = ExportLightningKeyViewModelPreview(
            greenWallet = previewWallet(isHardware = true)
        )
    }
}


