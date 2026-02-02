@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.data.lightning

import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.glsdk.Handle
import com.blockstream.glsdk.Network
import com.blockstream.glsdk.Node
import com.blockstream.glsdk.Scheduler
import com.blockstream.glsdk.Signer
import com.blockstream.utils.Loggable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GreenlightSdk(private val nodeRpc: Node, private val signerHandle: Handle) {

    suspend fun createInvoice(
        satoshi: Long,
        description: String
    ): LightningReceivePayment {
        val response = nodeRpc.receive(
            label = Uuid.generateV7().toString(),
            description = description,
            amountMsat = satoshi.milliSatoshi(),
        )

        logger.d { "createInvoice: $response" }

        return LightningReceivePayment(
            invoice = ((breez_sdk.parseInput(response.bolt11).toLightningInputType() as LightningInputType.Bolt11).invoice),
            openingFeeSatoshi = response.openingFeeMsat.satoshi()
        )
    }

    fun disconnect() {
        tryCatchNull {
            signerHandle.stop()
        }
    }

    companion object : Loggable() {
        fun scheduler(greenlightKeys: GreenlightKeys) =
            Scheduler(Network.BITCOIN).withDeveloperCert(greenlightKeys.developerCert ?: throw Exception("No developer cert provided"))

        suspend fun restoreCredentials(mnemonic: String, greenlightKeys: GreenlightKeys): ByteArray {
            val signer = Signer(mnemonic)
            val tempHandle = signer.start()
            val credentials = scheduler(greenlightKeys = greenlightKeys).recover(signer)
            try {
                tempHandle.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return credentials.save()
        }

        suspend fun registerNode(mnemonic: String, greenlightKeys: GreenlightKeys): ByteArray {
            val signer = Signer(mnemonic)
            val handle = signer.start()
            return scheduler(greenlightKeys = greenlightKeys)
                .register(signer = signer, code = null)
                .save().also {
                    try {
                        handle.stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }

        suspend fun restoreOrRegisterNode(mnemonic: String, greenlightKeys: GreenlightKeys): ByteArray {
            return try {
                restoreCredentials(mnemonic = mnemonic, greenlightKeys = greenlightKeys)
            } catch (e: Exception) {
                e.printStackTrace()
                registerNode(mnemonic, greenlightKeys)
            }
        }
    }
}