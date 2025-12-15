package com.blockstream.data.utils

import com.blockstream.data.gdk.Wally
import com.blockstream.data.gdk.data.Network
import kotlinx.serialization.Serializable

enum class WatchOnlyCredentialType {
    SLIP132_EXTENDED_PUBKEYS,
    CORE_DESCRIPTORS,
}

@Serializable
data class DetectionResult(
    val inputType: InputType,
    val network: String? = null,
    val credentialType: WatchOnlyCredentialType? = null,
    val isValid: Boolean = false,
    val errorMessage: String? = null
)

enum class InputType {
    XPUB,
    DESCRIPTOR,
    BCUR,
    MULTIPLE,
    INVALID
}

class WatchOnlyDetector(
    private val wally: Wally
) {

    // Validates if input is a properly formatted extended public key
    fun isValidXpub(input: String): Boolean {
        return try {
            wally.isXpubValid(input.trim())
        } catch (e: Exception) {
            false
        }
    }

    // Checks if input has characteristics of an xpub without full validation
    fun looksLikeXpub(input: String): Boolean {
        val trimmed = input.trim()
        val validPrefixes = listOf("xpub", "ypub", "zpub", "tpub", "upub", "vpub", "Ltub", "Mtub")
        return validPrefixes.any { trimmed.startsWith(it) } && trimmed.length in 100..120
    }

    // Detects the network (mainnet/testnet, bitcoin/liquid) from xpub prefixes or derivation paths
    fun detectNetwork(input: String): String? {
        val trimmed = input.trim()

        return when {
            // Bitcoin mainnet xpub prefixes
            trimmed.startsWith("xpub") -> Network.ElectrumMainnet
            trimmed.startsWith("ypub") -> Network.ElectrumMainnet
            trimmed.startsWith("zpub") -> Network.ElectrumMainnet

            // Bitcoin testnet xpub prefixes
            trimmed.startsWith("tpub") -> Network.ElectrumTestnet
            trimmed.startsWith("upub") -> Network.ElectrumTestnet
            trimmed.startsWith("vpub") -> Network.ElectrumTestnet

            // Bitcoin mainnet derivation paths
            trimmed.contains("m/44'/0'/") || trimmed.contains("m/49'/0'/") ||
            trimmed.contains("m/84'/0'/") || trimmed.contains("m/86'/0'/") ||
            trimmed.contains("/44'/0'/") || trimmed.contains("/49'/0'/") ||
            trimmed.contains("/84'/0'/") || trimmed.contains("/86'/0'/") -> Network.ElectrumMainnet

            // Bitcoin testnet derivation paths
            trimmed.contains("m/44'/1'/") || trimmed.contains("m/49'/1'/") ||
            trimmed.contains("m/84'/1'/") || trimmed.contains("m/86'/1'/") ||
            trimmed.contains("/44'/1'/") || trimmed.contains("/49'/1'/") ||
            trimmed.contains("/84'/1'/") || trimmed.contains("/86'/1'/") -> Network.ElectrumTestnet

            // Liquid network descriptor
            isLiquidDescriptor(trimmed) -> Network.ElectrumLiquid

            // Descriptors containing xpub prefixes
            trimmed.contains("xpub") && isDescriptor(trimmed) -> Network.ElectrumMainnet
            trimmed.contains("ypub") && isDescriptor(trimmed) -> Network.ElectrumMainnet
            trimmed.contains("zpub") && isDescriptor(trimmed) -> Network.ElectrumMainnet
            trimmed.contains("tpub") && isDescriptor(trimmed) -> Network.ElectrumTestnet
            trimmed.contains("upub") && isDescriptor(trimmed) -> Network.ElectrumTestnet
            trimmed.contains("vpub") && isDescriptor(trimmed) -> Network.ElectrumTestnet

            else -> null
        }
    }

    // Checks if input is a Bitcoin Core output descriptor format
    fun isDescriptor(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.contains("(") && (
            trimmed.contains("wpkh(") ||
            trimmed.contains("wsh(") ||
            trimmed.contains("pkh(") ||
            trimmed.contains("sh(") ||
            trimmed.contains("tr(") ||
            trimmed.contains("slip77(") ||
            trimmed.contains("combo(") ||
            trimmed.contains("raw(") ||
            trimmed.contains("addr(") ||
            trimmed.contains("multi(") ||
            trimmed.contains("sortedmulti(") ||
            trimmed.contains("musig(")
        )
    }

    // Checks if descriptor is specifically for Liquid network
    fun isLiquidDescriptor(input: String): Boolean {
        return input.contains("slip77(") ||
               (isDescriptor(input) && (
                   input.contains("blinded(") ||
                   input.contains("elip151(") ||
                   input.contains("/49'/1776'/") ||
                   input.contains("/84'/1776'/") ||
                   input.contains("/86'/1776'/")
               ))
    }

    // Determines the type of watch-only credential format
    fun detectInputType(input: String): InputType {
        val trimmed = input.trim()

        return when {
            trimmed.isEmpty() -> InputType.INVALID
            trimmed.startsWith("ur:crypto-", ignoreCase = true) -> InputType.BCUR
            isDescriptor(trimmed) -> InputType.DESCRIPTOR
            isValidXpub(trimmed) || looksLikeXpub(trimmed) -> InputType.XPUB
            trimmed.contains(",") || trimmed.contains("\n") || trimmed.contains("|") -> InputType.MULTIPLE
            else -> InputType.INVALID
        }
    }

    // Splits multiple credentials separated by commas, newlines, or pipes
    fun parseMultipleInputs(input: String): List<String> {
        return input.split(",", "\n", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    // Main detection method that analyzes input and returns comprehensive results
    fun detect(input: String): DetectionResult {
        val inputType = detectInputType(input)

        return when (inputType) {
            InputType.XPUB -> {
                DetectionResult(
                    inputType = InputType.XPUB,
                    network = detectNetwork(input),
                    credentialType = WatchOnlyCredentialType.SLIP132_EXTENDED_PUBKEYS,
                    isValid = isValidXpub(input)
                )
            }

            InputType.DESCRIPTOR -> {
                val credType = WatchOnlyCredentialType.CORE_DESCRIPTORS

                DetectionResult(
                    inputType = InputType.DESCRIPTOR,
                    network = detectNetwork(input),
                    credentialType = credType,
                    isValid = true
                )
            }

            InputType.BCUR -> {
                DetectionResult(
                    inputType = InputType.BCUR,
                    isValid = false
                )
            }

            InputType.MULTIPLE -> {
                val inputs = parseMultipleInputs(input)
                if (inputs.isNotEmpty()) {
                    // Analyze the first valid input to determine overall type
                    val firstValidInput = inputs.firstOrNull {
                        detectInputType(it) != InputType.INVALID
                    }

                    if (firstValidInput != null) {
                        // Detect the first valid input and keep MULTIPLE as the type
                        val firstInputResult = detect(firstValidInput)
                        return DetectionResult(
                            inputType = InputType.MULTIPLE,
                            network = firstInputResult.network,
                            credentialType = firstInputResult.credentialType,
                            isValid = firstInputResult.isValid,
                            errorMessage = firstInputResult.errorMessage
                        )
                    }
                }

                DetectionResult(
                    inputType = InputType.MULTIPLE,
                    isValid = false,
                    errorMessage = "No valid inputs found"
                )
            }

            else -> {
                DetectionResult(
                    inputType = InputType.INVALID,
                    isValid = false,
                    errorMessage = "Invalid or unsupported format"
                )
            }
        }
    }
}