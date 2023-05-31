package com.blockstream.green.devices

import com.blockstream.gdk.data.DeviceRequiredData
import com.blockstream.gdk.data.DeviceResolvedData
import com.blockstream.gdk.data.Network
import com.blockstream.green.gdk.HardwareWalletResolver
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWallet.SignTxResult
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.rxjava3.core.Single

class DeviceResolver constructor(
    private val hwWallet: HWWallet?,
    private val hwWalletBridge: HWWalletBridge? = null
) : HardwareWalletResolver {

    override fun requestDataFromDevice(network: Network, requiredData: DeviceRequiredData): Single<String> {
        return Single.create { emitter ->
            try {
                val data = requestDataFromHardware(network, requiredData)
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


    @Synchronized
    private fun requestDataFromHardware(network: Network, requiredData: DeviceRequiredData): String? {

        if (hwWallet == null) {
            return null
        }

        return when (requiredData.action) {
            "get_xpubs" -> {
                hwWallet.getXpubs(
                    network,
                    hwWalletBridge,
                    requiredData.paths?.map { it.map { it.toInt() } }).let {
                    DeviceResolvedData(
                        xpubs = it
                    )
                }
            }

            "sign_message" -> {
                hwWallet.signMessage(
                    hwWalletBridge,
                    requiredData.path?.map { it.toInt() },
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
                if (network.isLiquid) {
                    result = hwWallet.signLiquidTransaction(
                        network,
                        hwWalletBridge,
                        requiredData.transaction,
                        requiredData.signingInputs,
                        requiredData.transactionOutputs,
                        requiredData.signingTransactions,
                        requiredData.useAeProtocol ?: false
                    )
                } else {
                    result = hwWallet.signTransaction(
                        network,
                        hwWalletBridge,
                        requiredData.transaction,
                        requiredData.signingInputs,
                        requiredData.transactionOutputs,
                        requiredData.signingTransactions,
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

                DeviceResolvedData(
                    signatures = signatures,
                    signerCommitments = result.signerCommitments
                )
            }

            "get_blinding_factors" -> {
                val allbfs = hwWallet.getBlindingFactors(hwWalletBridge, requiredData.usedUtxos, requiredData.transactionOutputs)

                DeviceResolvedData(
                    assetBlinders = allbfs.assetblinders,
                    amountBlinders = allbfs.amountblinders
                )
            }

            "get_master_blinding_key" -> {
                DeviceResolvedData(
                    masterBlindingKey = hwWallet.getMasterBlindingKey(hwWalletBridge)
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
                            hwWallet.getBlindingNonce(
                                hwWalletBridge,
                                publicKeys[i],
                                scripts[i]
                            )
                        )

                        if (requiredData.blindingKeysRequired == true) {
                            blindingPublicKeys.add(
                                hwWallet.getBlindingKey(
                                    hwWalletBridge,
                                    scripts[i]
                                )
                            );
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
