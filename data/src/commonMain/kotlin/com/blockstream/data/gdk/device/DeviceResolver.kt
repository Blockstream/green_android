package com.blockstream.data.gdk.device

import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.data.DeviceRequiredData
import com.blockstream.data.gdk.data.DeviceResolvedData
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.jade.assetInfoForOutputs
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.jade.api.AssetInfo

class DeviceResolver(
    private val gdkHardwareWallet: GdkHardwareWallet,
    private val hwInteraction: HardwareWalletInteraction? = null,
    private val assetsProvider: AssetsProvider? = null
) : HardwareWalletResolver {

    // Registry metadata so the device can display tickers and precision-formatted amounts
    private suspend fun assetInfo(network: Network, outputs: List<InputOutput>): List<AssetInfo> {
        return assetsProvider?.let {
            assetInfoForOutputs(outputs = outputs, policyAsset = network.policyAsset, assetsProvider = it)
        } ?: listOf()
    }

    override suspend fun requestDataFromDevice(network: Network, requiredData: DeviceRequiredData): String {

        return when (requiredData.action) {
            "get_xpubs" -> {
                gdkHardwareWallet.getXpubs(
                    network = network,
                    paths = requiredData.paths?.map {
                        it.map { it.toInt() }
                    } ?: listOf(),
                    hwInteraction = hwInteraction
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
                val outputs = requiredData.transactionOutputs!!

                gdkHardwareWallet.signTransaction(
                    network = network,
                    transaction = requiredData.transaction!!,
                    inputs = requiredData.transactionInputs!!,
                    outputs = outputs,
                    transactions = requiredData.signingTransactions,
                    useAeProtocol = requiredData.useAeProtocol ?: false,
                    hwInteraction = hwInteraction,
                    assetInfo = assetInfo(network, outputs),
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
                    inputs = requiredData.transactionInputs!!,
                    outputs = requiredData.transactionOutputs!!
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
                                pubKey = publicKeys[i],
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
