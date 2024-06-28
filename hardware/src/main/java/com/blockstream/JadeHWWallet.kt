package com.blockstream

import android.util.Log
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Device
import com.blockstream.common.gdk.data.InputOutput
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.BlindingFactorsResult
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.gdk.device.SignMessageResult
import com.blockstream.common.gdk.device.SignTransactionResult
import com.blockstream.common.utils.Loggable
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.api.TxInput
import com.blockstream.jade.api.VersionInfo
import com.blockstream.jade.data.ChangeOutput
import com.blockstream.jade.data.JadeError
import com.blockstream.jade.data.JadeNetworks
import com.blockstream.jade.data.JadeState
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okio.Buffer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
class JadeHWWallet constructor(
    val gdk: Gdk,
    val wally: Wally,
    val jade: JadeAPI,
    override val device: Device
) : GdkHardwareWallet() {

    override val firmwareVersion: String
        get() = getVersionInfo(useCache = true).jadeVersion

    override val model: String
        get() = getVersionInfo(useCache = true).boardType

    val isMainnet: Boolean
        get() = getVersionInfo().jadeNetworks.let { it == JadeNetworks.MAIN || it == JadeNetworks.ALL }

    val isUninitializedOrUnsaved: Boolean
        get() = getVersionInfo().jadeState.let { it == JadeState.UNINIT || it == JadeState.UNSAVED }

    fun getVersionInfo(useCache: Boolean = false): VersionInfo {
        return jade.getVersionInfo(useCache)
    }

    // Authenticate Jade with pinserver and check firmware version with fw-server
    suspend fun authenticate(hwWalletLogin: HwWalletLogin,
                             jadeFirmwareManager: JadeFirmwareManager): Boolean {
        /*
         * 1. check firmware (and maybe OTA) any completely uninitialised device (ie no keys/pin set - no unlocking needed)
         * 2. authenticate the user (see above)
         * 3. check the firmware (and maybe OTA)
         * 4. authenticate the user *if required* - as we may have OTA'd and rebooted the hww.  Should be a no-op if not needed.
         */

        jadeFirmwareManager.checkFirmware(jade = jade, checkIfUninitialized = true)

        // authenticate the user (see above)
        authUser(hwWalletLogin)

        // check the firmware (and maybe OTA) for devices that are set-up and are below minimum firmware version (and hence needed unlocking first)
        val fwValid = jadeFirmwareManager.checkFirmware(jade = jade)

        if (fwValid) {
            return authUser(hwWalletLogin) // re-auth if required
        } else {
            throw JadeError(
                JadeError.UNSUPPORTED_FIRMWARE_VERSION,
                "Insufficient/invalid firmware version", null
            )
        }
    }

    @Synchronized
    override fun disconnect() {
        runBlocking {
            jade.disconnect()
        }
    }

    // Helper to push entropy into jade, and then call 'authUser()' in a loop
    // (until correctly setup and user authenticated etc).
    @Throws(Exception::class)
    fun authUser(hwLoginBridge: HwWalletLogin?): Boolean {
        // Push some extra entropy into Jade

        jade.addEntropy(gdk.getRandomBytes(32))

        val info = jade.getVersionInfo(false)
        val state = info.jadeState
        val networks = info.jadeNetworks

        val network: String
        if (state == JadeState.TEMP || state == JadeState.UNSAVED || state == JadeState.UNINIT || networks == JadeNetworks.ALL) {
            // Ask network from user
            network = hwLoginBridge?.let {
                it.requestNetwork()?.canonicalNetworkId ?: throw Exception("id_action_canceled")
            } ?: "mainnet"
        } else {
            network = if (networks == JadeNetworks.MAIN) {
                "mainnet"
            } else {
                "testnet"
            }
        }

        // JADE_STATE => READY  (device unlocked / ready to use)
        // anything else ( LOCKED | UNSAVED | UNINIT | TEMP) will need an authUser first to unlock
        if (state != JadeState.READY) {
            val completable: CompletableDeferred<Boolean> = CompletableDeferred()

            // JADE_STATE => TEMP no need for PIN entry
            if (hwLoginBridge != null && state != JadeState.TEMP) {
                if (state != JadeState.UNINIT) {
                    hwLoginBridge.interactionRequest(this, completable, "id_enter_pin_on_jade")
                }
            }

            try {
                // Authenticate with pinserver (loop/retry on failure)
                // Note: this should be a no-op if the user is already authenticated on the device.
                while (!jade.authUser(network)) {
                    Log.w(TAG, "Jade authentication failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                completable.complete(true)
            }
        }

        return true
    }

    @Synchronized
    override fun getXpubs(
        network: Network,
        paths: List<List<Int>>,
        hwInteraction: HardwareWalletInteraction?
    ): List<String> {
        logger.d { "getXpubs(network=${network.id}, paths=$paths)" }

        val canonicalNetworkId = network.canonicalNetworkId
        try {
            return paths.map { path ->
                jade.getXpub(canonicalNetworkId,  getUnsignedPath(path)).also {
                    logger.d { "Got xPub for $path: $it" }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e.message)
        }
    }

    @Synchronized
    override fun signMessage(
        path: List<Int>,
        message: String,
        useAeProtocol: Boolean,
        aeHostCommitment: String?,
        aeHostEntropy: String?,
        hwInteraction: HardwareWalletInteraction?
    ): SignMessageResult {
        logger.d { "signMessage(path=$path, message=$message, useAeProtocol=$useAeProtocol, aeHostCommitment=$aeHostCommitment, aeHostEntropy=$aeHostEntropy)" }

        val isSilent = path.size == 1 && path[0] == 1195487518 && message.startsWith("greenaddress.it      login ")

        val completable: CompletableDeferred<Boolean> = CompletableDeferred()

        try {

            hwInteraction?.takeIf { !isSilent }?.also {
                it.interactionRequest(this, completable, "id_check_your_device")
            }

            val result = jade.signMessage(
                path = getUnsignedPath(path),
                message = message,
                useAeProtocol = useAeProtocol,
                aeHostCommitment = aeHostCommitment?.hexToByteArray() ?: byteArrayOf(),
                aeHostEntropy = aeHostEntropy?.hexToByteArray() ?: byteArrayOf(),
            )

            // Convert the signature from Base64 into into DER hex for GDK
            var sigDecoded = Base64.decode(result.signature)

            // Need to truncate lead byte if recoverable signature
            if (sigDecoded.size == wally.ecSignatureRecoverableLen) {
                sigDecoded = sigDecoded.sliceArray(1 until sigDecoded.size)
            }

            val sigDerHex = wally.ecSigToDer(sigDecoded)

            logger.d { "signMessage() returning: $sigDerHex" }

            return SignMessageResult(sigDerHex, result.signerCommitment)
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            completable.complete(true)
        }
    }

    @Synchronized
    override fun signTransaction(
        network: Network,
        transaction: String,
        inputs: List<InputOutput>,
        outputs: List<InputOutput>,
        transactions: Map<String, String>?,
        useAeProtocol: Boolean,
        hwInteraction: HardwareWalletInteraction?
    ): SignTransactionResult {
        logger.d { "signTransaction(network=${network.id}, inputs=${inputs.size}, outputs=${outputs.size})" }

        val txBytes = transaction.hexToByteArray()

        try {
            if (network.isLiquid) {

                val txInputs = inputs.map { input ->
                    // Get the input in the form Jade expects
                    val script = input.prevoutScript?.hexToByteArray()
                    val commitment = input.commitment?.hexToByteArray()

                    TxInput(
                        isWitness = input.isSegwit(),
                        script = script,
                        valueCommitment = commitment,
                        path = input.userPath!!,
                        aeHostCommitment = input.aeHostCommitment?.hexToByteArray(),
                        aeHostEntropy = input.aeHostEntropy?.hexToByteArray()
                    )
                }

                // Get blinding factors and unblinding data per output - null for unblinded outputs
                // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
                val trustedCommitments = outputs.map { output ->
                    // Add a 'null' commitment for unblinded output
                    output.blindingKey?.let {
                        com.blockstream.jade.api.Commitment(
                            assetId = output.getAssetIdBytes(),
                            value = output.satoshi,
                            abf = output.getAbfs(),
                            vbf = output.getVbfs(),
                            blindingKey = output.getPublicKeyBytes()
                        )
                    }
                }

                // Get the change outputs and paths
                val change = getChangeData(outputs)

                // Make jade-api call to sign the txn
                val result = jade.signLiquidTx(
                    network.canonicalNetworkId,
                    txBytes,
                    txInputs,
                    trustedCommitments,
                    change
                )

                // Pivot data into return structure
                logger.d { "signLiquidTransaction() returning ${result.signatures.size}  signatures" }

                return SignTransactionResult(signatures = result.signatures, signerCommitments = result.signerCommitments)

            } else {

                if (transactions.isNullOrEmpty()) {
                    throw Exception("Input transactions missing")
                }

                // Get the inputs in the form Jade expects
                val txInputs = inputs.map { input ->

                    val swInput = input.isSegwit()
                    val script = input.prevoutScript?.hexToByteArray()

                    if (swInput && inputs.size == 1) {
                        // Single SegWit input - can skip sending entire tx and just send the sats amount
                        TxInput(
                            isWitness = true,
                            script = script,
                            satoshi = input.satoshi,
                            path = input.userPath,
                            aeHostCommitment = input.aeHostCommitment?.hexToByteArray(),
                            aeHostEntropy = input.aeHostEntropy?.hexToByteArray()
                        )
                    } else {
                        // Non-SegWit input or there are several inputs - in which case we always send
                        // the entire prior transaction up to Jade (so it can verify the spend amounts).
                        val txhex = transactions[input.txHash]
                            ?: throw Exception("Required input transaction not found: ${input.txHash}")
                        val inputTx = txhex.hexToByteArray()

                        TxInput(
                            isWitness = swInput,
                            inputTx = inputTx,
                            script = script,
                            path = input.userPath,
                            aeHostCommitment = input.aeHostCommitment?.hexToByteArray(),
                            aeHostEntropy = input.aeHostEntropy?.hexToByteArray()
                        )
                    }
                }

                // Get the change outputs and paths
                val change = getChangeData(outputs)

                // Make jade-api call
                val result = jade.signTx(network.canonicalNetworkId, txBytes, txInputs, change)

                logger.d { "signTransaction() returning ${result.signatures.size} signatures" }

                return SignTransactionResult(signatures = result.signatures, signerCommitments = result.signerCommitments)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    @Synchronized
    override fun getMasterBlindingKey(hwInteraction: HardwareWalletInteraction?): String {
        logger.d { "getMasterBlindingKey()" }

        val completable: CompletableDeferred<Boolean> = CompletableDeferred(null)

        try {
            val masterBlindingKey = (try {
                // Try to get master blinding key silently
                jade.getMasterBlindingKey(true)
            } catch (e: Exception) {
                // Re-try
                null
            } ?: run {
                hwInteraction?.interactionRequest(this, completable, "get_master_blinding_key")
                // Ask user for master blinding key export
                jade.getMasterBlindingKey(false)
            }).toHexString().also {
                logger.d { "getMasterBlindingKey() returning $it" }
            }

            return masterBlindingKey
        } catch (e: JadeError) {
            if (e.code == JadeError.CBOR_RPC_USER_CANCELLED) {
                // User cancelled on the device - return as empty string (rather than error)
                return ""
            }
            throw RuntimeException(e.message)
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        } finally {
            completable.complete(true)
        }
    }

    @Synchronized
    override fun getBlindingKey(
        scriptHex: String,
        hwInteraction: HardwareWalletInteraction?
    ): String {
        logger.d { "getBlindingKey(scriptHex=$scriptHex)" }

        return try {
            jade.getBlindingKey(scriptHex.hexToByteArray()).toHexString().also {
                logger.d { "getBlindingKey() returning $it" }
            }
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @Synchronized
    override fun getBlindingNonce(
        pubKey: String,
        scriptHex: String,
        hwInteraction: HardwareWalletInteraction?
    ): String {
        logger.d { "getBlindingNonce(pubkey=$pubKey, scriptHex=$scriptHex)" }

        return try {
            jade.getSharedNonce(scriptHex.hexToByteArray(), pubKey.hexToByteArray()).toHexString().also {
                logger.d { "getBlindingNonce() returning $it" }
            }
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @Synchronized
    override fun getBlindingFactors(
        inputs: List<InputOutput>,
        outputs: List<InputOutput>,
        hwInteraction: HardwareWalletInteraction?
    ): BlindingFactorsResult {
        logger.d { "getBlindingFactors(inputs=$inputs, outputs=$outputs)" }

        try {

            // Compute hashPrevouts to derive deterministic blinding factors from
            val txhashes = Buffer()

            val outputIdxs = inputs.map { input ->
                input.getPtIdxInt().also {
                    txhashes.write(input.getTxid())
                }
            }

            val hashPrevouts = wally.hashPrevouts(txhashes.readByteArray(), outputIdxs)

            // Enumerate the outputs and provide blinding factors as needed
            // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
            val assetBlinders: MutableList<String> = mutableListOf()
            val amountBlinders: MutableList<String> = mutableListOf()

            outputs.forEachIndexed { index, output ->
                if (output.blindingKey != null) {

                    jade.getBlindingFactor(hashPrevouts, index, "ASSET_AND_VALUE").also {
                        assetBlinders.add(
                            sliceReversed(
                                it,
                                0,
                                wally.blindingFactorLen
                            ).toHexString()
                        )

                        amountBlinders.add(
                            sliceReversed(
                                it,
                                wally.blindingFactorLen,
                                wally.blindingFactorLen
                            ).toHexString()
                        )
                    }.also {
                        logger.d { "getBlindingFactors() for output $index: ${assetBlinders[index]} /  ${amountBlinders[index]}" }
                    }
                } else {
                    // Empty string placeholders
                    assetBlinders.add("")
                    amountBlinders.add("")
                }
            }

            return BlindingFactorsResult(assetBlinders, amountBlinders).also {
                logger.d { "getBlindingFactors() returning for ${outputs.size} outputs" }
            }
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    override fun getGreenAddress(
        network: Network,
        account: Account,
        path: List<Long>,
        csvBlocks: Long,
        hwInteraction: HardwareWalletInteraction?
    ): String {
        try {
            val canonicalNetworkId = network.canonicalNetworkId
            if (network.isMultisig) {
                // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
                // In any case the last two entries are 'branch' and 'pointer'
                val pathlen = path.size
                val branch = path[pathlen - 2]
                val pointer = path[pathlen - 1]
                var recoveryXpub: String? = null

                Log.d(
                    TAG,
                    "getGreenAddress() (multisig shield) for subaccount: " + account.pointer + ", branch: "
                            + branch + ", pointer " + pointer
                )

                // Jade expects any 'recoveryxpub' to be at the subact/branch level, consistent with tx outputs - but gdk
                // subaccount data has the base subaccount chain code and pubkey - so we apply the branch derivation here.
                account.recoveryXpub.takeIf { it.isNotBlank() }?.also {
                    recoveryXpub = wally.recoveryXpubBranchDerivation(
                        recoveryXpub = it,
                        branch = branch
                    )
                }

                // Get receive address from Jade for the path elements given
                val address = jade.getReceiveAddress(
                    canonicalNetworkId,
                    account.pointer, branch, pointer,
                    recoveryXpub, csvBlocks
                )
                Log.d(TAG, "Got green address for branch: $branch, pointer: $pointer: $address")
                return address
            } else {
                // Green Electrum Singlesig
                Log.d(TAG, "getGreenAddress() (singlesig) for path: $path")
                val variant = mapAddressType(account.type.gdkType)
                val address =
                    jade.getReceiveAddress(canonicalNetworkId, variant!!, path)
                Log.d(TAG, "Got green address for path: $path, type: $variant: $address")
                return address
            }
        } catch (e: JadeError) {
            if (e.code == JadeError.CBOR_RPC_USER_CANCELLED) {
                // User cancelled on the device - treat as mismatch (rather than error)
                return ""
            }
            throw RuntimeException(e.message)
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    override val disconnectEvent: StateFlow<Boolean>?
        get() = jade.disconnectEvent

    companion object : Loggable(){
        private const val TAG = "JadeHWWallet"

        // Helper to map a [single-sig] address type into a jade descriptor variant string
        private fun mapAddressType(addrType: String?): String? {
            return when (addrType) {
                "p2pkh" -> "pkh(k)"
                "p2wpkh" -> "wpkh(k)"
                "p2sh-p2wpkh" -> "sh(wpkh(k))"
                else -> null
            }
        }

        // Helper to turn the BIP32 paths back into a list of Longs, rather than a list of Integers
        // (which may well be expressed as negative [for hardened paths]).
        private fun getUnsignedPath(signed: List<Int>): List<Long> {
            return signed.map {
                if (it < 0) {
                    1L + Int.MAX_VALUE + (it - Int.MIN_VALUE)
                } else {
                    it.toLong()
                }
            }
        }

        // Helper to get the change paths for auto-validation
        private fun getChangeData(outputs: List<InputOutput>): List<ChangeOutput?> {
            // Get the change outputs and paths
            return outputs.map { output ->
                if (output.isChange == true) {
                    // change - get path
                    ChangeOutput(
                        path = output.userPath,
                        recoveryXpub = output.recoveryXpub,
                        csvBlocks = if ("csv" == output.addressType) output.subtype else 0,
                        variant = mapAddressType(output.addressType)
                    )
                } else {
                    // Not change, put null place holder
                    null
                }
            }
        }

        private fun sliceReversed(data: ByteArray, offset: Int, len: Int): ByteArray {
            val result = ByteArray(len)
            for (i in 0 until len) {
                result[i] = data[offset + len - i - 1]
            }
            return result
        }
    }
}