package com.blockstream.common.gdk.device

import com.blockstream.common.gdk.HardwareWalletResolver
import com.blockstream.common.gdk.data.DeviceRequiredData
import com.blockstream.common.gdk.data.DeviceResolvedData
import com.blockstream.common.gdk.data.Network
import kotlinx.coroutines.CompletableDeferred

class DeviceResolver constructor(
    private val gdkHardwareWallet: GdkHardwareWallet,
    private val hwInteraction: HardwareWalletInteraction? = null
) : HardwareWalletResolver {

    override fun requestDataFromDevice(network: Network, requiredData: DeviceRequiredData): CompletableDeferred<String> {
        return CompletableDeferred<String>().also { deferred ->
            try {
                deferred.complete(requestData(network, requiredData))
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            }
        }
    }

    private fun requestData(network: Network, requiredData: DeviceRequiredData): String {

        return when (requiredData.action) {
            "get_xpubs" -> {
                gdkHardwareWallet.getXpubs(
                    network = network,
                    hwInteraction = hwInteraction,
                    paths = requiredData.paths?.map {
                        it.map { it.toInt() }
                    } ?: listOf()
                ).let {
                    DeviceResolvedData(xpubs = it)
                }
            }

            "sign_message" -> {
                gdkHardwareWallet.signMessage(
                    hwInteraction = hwInteraction,
                    path = requiredData.path?.map { it.toInt() } ?: listOf(),
                    message = requiredData.message!!,
                    useAeProtocol = requiredData.useAeProtocol ?: false,
                    aeHostCommitment = requiredData.aeHostCommitment,
                    aeHostEntropy = requiredData.aeHostEntropy
                ).let {
                    DeviceResolvedData(
                        signature = it.signature,
                        signerCommitment = it.signerCommitment
                    )
                }
            }

            "sign_tx" -> {
                gdkHardwareWallet.signTransaction(
                    network = network,
                    hwInteraction = hwInteraction,
                    transaction = requiredData.transaction!!,
                    inputs = requiredData.transactionInputs!!,
                    outputs = requiredData.transactionOutputs!!,
                    transactions = requiredData.signingTransactions,
                    useAeProtocol = requiredData.useAeProtocol ?: false
                ).let {
                    DeviceResolvedData(
                        signatures = it.signatures,
                        signerCommitments = it.signerCommitments
                    )
                }
            }

            "get_blinding_factors" -> {
                gdkHardwareWallet.getBlindingFactors(
                    hwInteraction = hwInteraction,
                    inputs = requiredData.transactionInputs,
                    outputs = requiredData.transactionOutputs
                ).let {
                    DeviceResolvedData(
                        assetBlinders = it.assetblinders,
                        amountBlinders = it.amountblinders
                    )
                }
            }

            "get_master_blinding_key" -> {
                DeviceResolvedData(
                    masterBlindingKey = gdkHardwareWallet.getMasterBlindingKey(hwInteraction)
                )
            }

            "get_blinding_nonces" -> {
                val nonces = mutableListOf<String>()
                val blindingPublicKeys = mutableListOf<String>()

                val scripts = requiredData.scripts
                val publicKeys = requiredData.publicKeys

                if (scripts != null && publicKeys != null && scripts.size == publicKeys.size) {
                    for (i in 0 until (scripts.size)) {
                        nonces.add(
                            gdkHardwareWallet.getBlindingNonce(
                                hwInteraction = hwInteraction,
                                pubkey = publicKeys[i],
                                scriptHex = scripts[i]
                            )
                        )

                        if (requiredData.blindingKeysRequired == true) {
                            blindingPublicKeys.add(
                                gdkHardwareWallet.getBlindingKey(
                                    hwInteraction = hwInteraction,
                                    scriptHex = scripts[i]
                                )
                            )
                        }
                    }
                }

                DeviceResolvedData(
                    nonces = nonces,
                    publicKeys = blindingPublicKeys
                )
            }

            "get_blinding_public_keys" -> {
                val publicKeys = mutableListOf<String>()

                for (script in requiredData.scripts ?: listOf()) {
                    publicKeys.add(
                        gdkHardwareWallet.getBlindingKey(
                            hwInteraction = hwInteraction,
                            scriptHex = script
                        )
                    )
                }

                DeviceResolvedData(publicKeys = publicKeys)
            }
            else -> {
                throw RuntimeException("Unsupported action")
            }
        }.toJson()
    }

    companion object {
        fun createIfNeeded(
            gdkHardwareWallet: GdkHardwareWallet? = null,
            hwInteraction: HardwareWalletInteraction? = null
        ): DeviceResolver? {
            return gdkHardwareWallet?.let {
                DeviceResolver(gdkHardwareWallet = it, hwInteraction = hwInteraction)
            }
        }
    }
}
