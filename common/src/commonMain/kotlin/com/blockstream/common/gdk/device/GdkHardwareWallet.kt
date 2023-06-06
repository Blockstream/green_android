package com.blockstream.common.gdk.device

import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Device
import com.blockstream.common.gdk.data.InputOutput
import com.blockstream.common.gdk.data.Network
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement

interface HardwareWalletInteraction{
    fun interactionRequest(hw: GdkHardwareWallet, completable: CompletableDeferred<Boolean>?, text: String?)
    fun requestPinMatrix(deviceBrand: DeviceBrand?): String?
    fun requestPassphrase(deviceBrand: DeviceBrand?): String?
}

abstract class GdkHardwareWallet {
    abstract val disconnectEvent: StateFlow<Boolean>?

    abstract val firmwareVersion: String?

    abstract val model: String

    abstract val device: Device

    // Return the base58check encoded xpubs for each path in paths
    abstract fun getXpubs(
        network: Network,
        hwInteraction: HardwareWalletInteraction?,
        paths: List<List<Int>>
    ): List<String>

    // Sign message with the key resulting from path, and return it as hex encoded DER
    // If using Anti-Exfil protocol, also return the signerCommitment (if not this can be null).
    abstract fun signMessage(
        hwInteraction: HardwareWalletInteraction?,
        path: List<Int>,
        message: String,
        useAeProtocol: Boolean,
        aeHostCommitment: String?,
        aeHostEntropy: String?
    ): SignMessageResult

    abstract fun signTransaction(
        network: Network,
        hwInteraction: HardwareWalletInteraction?,
        transaction: JsonElement,
        inputs: List<InputOutput?>,
        outputs: List<InputOutput?>,
        transactions: Map<String, String>?,
        useAeProtocol: Boolean
    ): SignTransactionResult

    abstract fun getBlindingFactors(
        hwInteraction: HardwareWalletInteraction?,
        inputs: List<InputOutput?>?,
        outputs: List<InputOutput?>?
    ): BlindingFactorsResult

    abstract fun getMasterBlindingKey(
        hwInteraction: HardwareWalletInteraction?,
    ): String

    abstract fun getBlindingNonce(
        hwInteraction: HardwareWalletInteraction?,
        pubkey: String, scriptHex: String): String

    abstract fun getBlindingKey(
        hwInteraction: HardwareWalletInteraction?,
        scriptHex: String): String

    @Throws(Exception::class)
    abstract fun getGreenAddress(
        network: Network,
        hwInteraction: HardwareWalletInteraction?,
        account: Account,
        path: List<Long>,
        csvBlocks: Long
    ): String

    abstract fun disconnect()
}