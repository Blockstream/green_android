package com.blockstream.green.devices

import com.blockstream.gdk.HardwareWalletResolver
import com.blockstream.gdk.data.DeviceRequiredData
import com.blockstream.gdk.data.DeviceResolvedData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWallet.SignTxResult
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class DeviceResolver(private val hwWalletBridge: HWWalletBridge?, private val hwWallet: HWWallet?) :
    HardwareWalletResolver {
    private val objectMapper by lazy { ObjectMapper() }

    override fun requestDataFromDevice(requiredData: DeviceRequiredData): Single<String> {
        return Single.create { emitter ->
            try {
                val data = requestDataFromHardware(requiredData)
                if (data != null) {
                    emitter.onSuccess(data)
                } else {
                    emitter.tryOnError(Exception("Unknown error"))
                }
            } catch (e: Exception) {
                emitter.tryOnError(e)
            }
        }
    }


    private fun toObjectNode(jsonElement: JsonElement?): ObjectNode {
        return objectMapper.readTree(Json.encodeToString(jsonElement)) as ObjectNode
    }

    @Synchronized
    fun requestDataFromHardware(requiredData: DeviceRequiredData): String? {

        if(hwWallet == null){
            return null
        }

        return when (requiredData.action) {
            "get_xpubs" -> {
                hwWallet.getXpubs(hwWalletBridge, requiredData.paths).let {
                    DeviceResolvedData(
                        xpubs = it
                    )
                }
            }

            "sign_message" -> {
                hwWallet.signMessage(
                    hwWalletBridge,
                    requiredData.path,
                    requiredData.message,
                    requiredData.useAeProtocol ?: false,
                    requiredData.aeHostCommitment,
                    requiredData.aeHostEntropy
                ).let {

                    val signerCommitment = it.signerCommitment.let { signerCommitment ->
                        // Corrupt the commitments to emulate a corrupted wallet
                        if (hwWallet.hardwareEmulator != null && hwWallet.hardwareEmulator?.getAntiExfilCorruptionForMessageSign() == true) {
                            // Make it random to allow proceeding to a logged in state
                            signerCommitment.replace("0", "1")
                        } else {
                            signerCommitment
                        }
                    }

                    DeviceResolvedData(
                        signature = it.signature,
                        signerCommitment = signerCommitment
                    )

                }
            }

            "sign_tx" -> {
                val result: SignTxResult
                if (hwWallet.network.isLiquid) {
                    result = hwWallet.signLiquidTransaction(
                        hwWalletBridge,
                        toObjectNode(requiredData.transaction),
                        requiredData.getSigningInputsAsInputOutputData(),
                        requiredData.getTransactionOutputsAsInputOutputData(),
                        requiredData.signingTransactions,
                        requiredData.signingAddressTypes,
                        requiredData.useAeProtocol ?: false
                    )
                } else {
                    result = hwWallet.signTransaction(
                        hwWalletBridge,
                        toObjectNode(requiredData.transaction),
                        requiredData.getSigningInputsAsInputOutputData(),
                        requiredData.getTransactionOutputsAsInputOutputData(),
                        requiredData.signingTransactions,
                        requiredData.signingAddressTypes,
                        requiredData.useAeProtocol ?: false
                    )
                }

                val signatures = result.signatures.let { signatures ->
                    // Corrupt the commitments to emulate a corrupted wallet
                    if (hwWallet.hardwareEmulator != null && hwWallet.hardwareEmulator?.getAntiExfilCorruptionForTxSign() == true) {
                        signatures.map {
                            it.replace("0", "1")
                        }
                    } else {
                        signatures
                    }
                }


                if (hwWallet.network.isLiquid) {
                    DeviceResolvedData(
                        signatures = signatures,
                        signerCommitments = result.signerCommitments,
                        assetCommitments = result.assetCommitments,
                        valueCommitments = result.valueCommitments,
                        assetBlinders = result.assetBlinders,
                        amountBlinders = result.amountBlinders
                    )
                } else {
                    DeviceResolvedData(
                        signatures = signatures,
                        signerCommitments = result.signerCommitments
                    )
                }
            }

            "get_master_blinding_key" -> {
                DeviceResolvedData(
                    masterBlindingKey = hwWallet.getMasterBlindingKey(hwWalletBridge)
                )
            }

            "get_blinding_nonces" -> {

                val nonces = mutableListOf<String>()

                if (requiredData.scripts?.size == requiredData.publicKeys?.size) {

                    for (i in 0 until (requiredData.scripts?.size ?: 0)) {
                        nonces.add(
                            hwWallet.getBlindingNonce(
                                hwWalletBridge,
                                requiredData.publicKeys!![i],
                                requiredData.scripts!![i]
                            )
                        )
                    }
                }

                DeviceResolvedData(
                    nonces = nonces
                )
            }

            "get_blinding_public_keys" -> {
                val publicKeys = mutableListOf<String>()

                for (script in requiredData.scripts ?: listOf()) {
                    publicKeys.add(
                        hwWallet.getBlindingKey(hwWalletBridge, script)
                    )
                }

                DeviceResolvedData(
                    publicKeys = publicKeys
                )
            }

            else -> {
                null
            }
        }?.toJson()
    }
}