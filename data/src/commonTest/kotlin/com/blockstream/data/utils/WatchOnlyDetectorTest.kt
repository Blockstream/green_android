package com.blockstream.data.utils

import com.blockstream.data.gdk.Wally
import com.blockstream.data.gdk.data.Network
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchOnlyDetectorTest {
    private lateinit var detector: WatchOnlyDetector
    private lateinit var mockWally: MockWally

    // Mock Wally implementation for testing
    private class MockWally : Wally {
        override val aesBlockLen: Int = 16
        override val hmacSha256Len: Int = 32
        override val ecPrivateKeyLen: Int = 32
        override val ecSignatureRecoverableLen: Int = 65
        override val bip39TotalWords: Int = 2048
        override val blindingFactorLen: Int = 32

        override fun ecPrivateKeyVerify(privateKey: ByteArray): Boolean = true
        override fun ecSigToDer(signature: ByteArray): String = ""
        override fun bip39GetWord(index: Int): String = ""
        override fun bip39MnemonicValidate(mnemonic: String): Boolean = true

        override fun isXpubValid(xpub: String): Boolean {
            // Basic validation for testing - accept known prefixes with proper length
            // and ensure it doesn't contain invalid characters like commas or newlines
            val validPrefixes = listOf("xpub", "ypub", "zpub", "tpub", "upub", "vpub")
            return validPrefixes.any { xpub.startsWith(it) } &&
                   xpub.length in 100..120 &&
                   !xpub.contains(",") &&
                   !xpub.contains("\n") &&
                   !xpub.contains("|")
        }

        override fun bip32Fingerprint(bip32xPub: String): String? = null
        override fun hashPrevouts(txHashes: ByteArray, utxoIndexes: List<Int>): ByteArray = ByteArray(0)
        override fun recoveryXpubBranchDerivation(recoveryXpub: String, branch: Long): String = ""
        override fun bip85FromMnemonic(
            mnemonic: String,
            passphrase: String?,
            isTestnet: Boolean,
            index: Long,
            numOfWords: Long
        ): String? = null
        override fun bip85FromJade(
            privateKey: ByteArray,
            publicKey: ByteArray,
            label: String,
            payload: ByteArray
        ): String? = null

        override fun psbtIsBase64(psbt: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun psbtIsBinary(psbt: ByteArray): Boolean {
            TODO("Not yet implemented")
        }

        override fun psbtToV0(psbt: String): String {
            TODO("Not yet implemented")
        }
    }

    @BeforeTest
    fun setup() {
        mockWally = MockWally()
        detector = WatchOnlyDetector(mockWally)
    }

    // Test XPub validation
    @Test
    fun testIsValidXpub_ValidMainnetXpubs() {
        // Valid mainnet xpubs (using realistic but fake xpubs)
        val validMainnetXpub = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"
        val validSegwitXpub = "ypub6QqdH2c5z79681jUgdxjGJzGW9zpL4ryPCPufLLTgNQbaRfncsQiF8fWTDPivTGUFHWe2yUgBGZfhwJZfbNQjLPqmNPQiPPCHRmBe8bEHKK"
        val validNativeSegwitXpub = "zpub6jftahH18ngZxLmXaKw3GSZzZsszmt9WqedkyZdezFtWRFBZqsQH5hyUmb4pCEeZGmVfQuP5bedXTB8is6fTv19U1GQRyQUKQGUTzyHACMF"

        assertTrue(detector.isValidXpub(validMainnetXpub))
        assertTrue(detector.isValidXpub(validSegwitXpub))
        assertTrue(detector.isValidXpub(validNativeSegwitXpub))
    }

    @Test
    fun testIsValidXpub_ValidTestnetXpubs() {
        // Valid testnet xpubs
        val validTestnetXpub = "tpubD6NzVbkrYhZ4WaWSyoBvQwbpLkojyoTZPRsgXELWz3Popb3qkjcJyJUGLnL4qHHoQvao8ESaAstxYSnhyswJ76uZPStJRJCTKvosUCJZL5B"
        val validSegwitTestnetXpub = "upub57Wa4MvRPNyAhxr578mQUdPr6MHwpg3Su875hj8K75AeUVZLXtFeiP52BrhePqP93Kqw3TKWAUMT3vwWFZxcL8Y8oaHLPuC9KQpSihcp3XM"
        val validNativeSegwitTestnetXpub = "vpub5SLqN2bLY4WeYBNj6HGtt4fYVgkUxMXCjfj7DfLDRghpcctqzF1mGRQbQLqAfhJdGtGPTYQqhR8m1M8YjZLGFfxLEoPSz2BHDJhLLaVqBWG"

        assertTrue(detector.isValidXpub(validTestnetXpub))
        assertTrue(detector.isValidXpub(validSegwitTestnetXpub))
        assertTrue(detector.isValidXpub(validNativeSegwitTestnetXpub))
    }

    @Test
    fun testIsValidXpub_InvalidXpubs() {
        assertFalse(detector.isValidXpub(""))
        assertFalse(detector.isValidXpub("invalid"))
        assertFalse(detector.isValidXpub("xpub123")) // Too short
        assertFalse(detector.isValidXpub("notanxpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"))
    }

    // Test network detection
    @Test
    fun testDetectNetwork_MainnetXpubPrefixes() {
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"))
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("ypub6QqdH2c5z79681jUgdxjGJzGW9zpL4ryPCPufLLTgNQbaRfncsQiF8fWTDPivTGUFHWe2yUgBGZfhwJZfbNQjLPqmNPQiPPCHRmBe8bEHKK"))
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("zpub6jftahH18ngZxLmXaKw3GSZzZsszmt9WqedkyZdezFtWRFBZqsQH5hyUmb4pCEeZGmVfQuP5bedXTB8is6fTv19U1GQRyQUKQGUTzyHACMF"))
    }

    @Test
    fun testDetectNetwork_TestnetXpubPrefixes() {
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("tpubD6NzVbkrYhZ4WaWSyoBvQwbpLkojyoTZPRsgXELWz3Popb3qkjcJyJUGLnL4qHHoQvao8ESaAstxYSnhyswJ76uZPStJRJCTKvosUCJZL5B"))
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("upub57Wa4MvRPNyAhxr578mQUdPr6MHwpg3Su875hj8K75AeUVZLXtFeiP52BrhePqP93Kqw3TKWAUMT3vwWFZxcL8Y8oaHLPuC9KQpSihcp3XM"))
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("vpub5SLqN2bLY4WeYBNj6HGtt4fYVgkUxMXCjfj7DfLDRghpcctqzF1mGRQbQLqAfhJdGtGPTYQqhR8m1M8YjZLGFfxLEoPSz2BHDJhLLaVqBWG"))
    }

    @Test
    fun testDetectNetwork_MainnetDescriptorPaths() {
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("wpkh([73c5da0a/84'/0'/0']xpub6CatWdiZiodmUeTDp8LT5or8nmbKNcuyvz7WyksVFkKB4RHwCD3XyuvPEbvqAQY3rAPshWcMLoP2fMFMKHPJ4ZeZXYVUhLv1VMrjPC7PW6V/0/*)"))
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("sh(wpkh([73c5da0a/49'/0'/0']ypub6XiUmHaFB5pJmZmLsM5oPZvfRNrMJ9LFVvD8Lg96iBHDQ2sSx5SEZMcaYuGHJZPmVQYTPFCUm7Qnfjai9GdjqaMGF3orWUPaKGTb2eTrS8W/0/*))"))
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("pkh([73c5da0a/44'/0'/0']xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*)"))
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("tr([73c5da0a/86'/0'/0']xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*)"))
    }

    @Test
    fun testDetectNetwork_TestnetDescriptorPaths() {
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("wpkh([73c5da0a/84'/1'/0']tpubDC5FSnBiZDMmhiuCmWAYsLwgLYrrT9rAqvTySfuCCrgsWz8wxMXUS9Tb9iVMvcRbvFcAHGkMD5Kx8koh4GquNGNTfohfk7pgjhaPCdXpoba/0/*)"))
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("sh(wpkh([73c5da0a/49'/1'/0']upub5EFU65HtV5TeiSHmZZm7FUffBGy8UKeqp7vw43jYbvZPpoVsgU93oac7Wk3u6moKegAEWtGNF8DehrnHtv21XXEMYRUocHqguyjknFHYfgY/0/*))"))
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("pkh([73c5da0a/44'/1'/0']tpubDDgEAMpHn8tX5Bs19WWJLZBeFzbpE7BYuP3Qo71abZnQ7FmN3idRPg4oPWt2Q6Uf9huGv7AGMTu8M2BaCxAdThQArjLWLDLpxVX2gYfh2YJ/0/*)"))
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("tr([73c5da0a/86'/1'/0']tpubDC5FSnBiZDMmhiuCmWAYsLwgLYrrT9rAqvTySfuCCrgsWz8wxMXUS9Tb9iVMvcRbvFcAHGkMD5Kx8koh4GquNGNTfohfk7pgjhaPCdXpoba/0/*)"))
    }

    @Test
    fun testDetectNetwork_LiquidDescriptors() {
        assertEquals(Network.ElectrumLiquid, detector.detectNetwork("slip77([73c5da0a/0']0217e1deb7e0fe5046a95021877a678e9f99b4c5c58d38c45fb9f79e5f6e2ad479)"))
        assertEquals(Network.ElectrumLiquid, detector.detectNetwork("blinded(slip77([73c5da0a/0']0217e1deb7e0fe5046a95021877a678e9f99b4c5c58d38c45fb9f79e5f6e2ad479))"))
        assertEquals(Network.ElectrumLiquid, detector.detectNetwork("wpkh([73c5da0a/84'/1776'/0']xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*)"))
    }

    @Test
    fun testDetectNetwork_InvalidInputs() {
        assertNull(detector.detectNetwork(""))
        assertNull(detector.detectNetwork("invalid"))
        assertNull(detector.detectNetwork("randomstring"))
    }

    // Test descriptor detection
    @Test
    fun testIsDescriptor_ValidDescriptors() {
        assertTrue(detector.isDescriptor("wpkh(xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*)"))
        assertTrue(detector.isDescriptor("sh(wpkh(xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*))"))
        assertTrue(detector.isDescriptor("pkh(xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*)"))
        assertTrue(detector.isDescriptor("tr(xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz/0/*)"))
        assertTrue(detector.isDescriptor("slip77(0217e1deb7e0fe5046a95021877a678e9f99b4c5c58d38c45fb9f79e5f6e2ad479)"))
        assertTrue(detector.isDescriptor("multi(2,xpub1,xpub2,xpub3)"))
        assertTrue(detector.isDescriptor("sortedmulti(2,xpub1,xpub2,xpub3)"))
    }

    @Test
    fun testIsDescriptor_InvalidDescriptors() {
        assertFalse(detector.isDescriptor(""))
        assertFalse(detector.isDescriptor("xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"))
        assertFalse(detector.isDescriptor("invalid"))
        assertFalse(detector.isDescriptor("(")) // Just parentheses without descriptor function
    }

    @Test
    fun testIsLiquidDescriptor() {
        assertTrue(detector.isLiquidDescriptor("slip77(0217e1deb7e0fe5046a95021877a678e9f99b4c5c58d38c45fb9f79e5f6e2ad479)"))
        assertTrue(detector.isLiquidDescriptor("blinded(wpkh(xpub...))"))
        assertTrue(detector.isLiquidDescriptor("wpkh([73c5da0a/84'/1776'/0']xpub.../0/*)"))

        assertFalse(detector.isLiquidDescriptor("wpkh(xpub.../0/*)"))
        assertFalse(detector.isLiquidDescriptor("sh(wpkh(xpub.../0/*))"))
    }

    // Test input type detection
    @Test
    fun testDetectInputType() {
        assertEquals(InputType.INVALID, detector.detectInputType(""))
        assertEquals(InputType.BCUR, detector.detectInputType("ur:crypto-output/1-2"))
        assertEquals(InputType.DESCRIPTOR, detector.detectInputType("wpkh(xpub.../0/*)"))
        assertEquals(InputType.XPUB, detector.detectInputType("xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"))
        assertEquals(InputType.MULTIPLE, detector.detectInputType("xpub1,xpub2"))
        assertEquals(InputType.MULTIPLE, detector.detectInputType("xpub1\nxpub2"))
        assertEquals(InputType.MULTIPLE, detector.detectInputType("xpub1|xpub2"))
        assertEquals(InputType.INVALID, detector.detectInputType("invalid"))
    }

    // Test multiple input parsing
    @Test
    fun testParseMultipleInputs() {
        val commaResult = detector.parseMultipleInputs("xpub1,xpub2,xpub3")
        assertEquals(3, commaResult.size)
        assertEquals("xpub1", commaResult[0])
        assertEquals("xpub2", commaResult[1])
        assertEquals("xpub3", commaResult[2])

        val newlineResult = detector.parseMultipleInputs("xpub1\nxpub2\nxpub3")
        assertEquals(3, newlineResult.size)

        val pipeResult = detector.parseMultipleInputs("xpub1|xpub2|xpub3")
        assertEquals(3, pipeResult.size)

        val mixedResult = detector.parseMultipleInputs("xpub1,xpub2\nxpub3|xpub4")
        assertEquals(4, mixedResult.size)

        val withSpacesResult = detector.parseMultipleInputs(" xpub1 , xpub2 \n xpub3 ")
        assertEquals(3, withSpacesResult.size)
        assertEquals("xpub1", withSpacesResult[0])

        val withEmptyResult = detector.parseMultipleInputs("xpub1,,xpub2\n\nxpub3")
        assertEquals(3, withEmptyResult.size)
    }

    // Test full detection flow
    @Test
    fun testDetect_Xpub() {
        val xpub = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"
        val result = detector.detect(xpub)

        assertEquals(InputType.XPUB, result.inputType)
        assertEquals(Network.ElectrumMainnet, result.network)
        assertEquals(WatchOnlyCredentialType.SLIP132_EXTENDED_PUBKEYS, result.credentialType)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun testDetect_Descriptor() {
        val descriptor = "wpkh([73c5da0a/84'/0'/0']xpub6CatWdiZiodmUeTDp8LT5or8nmbKNcuyvz7WyksVFkKB4RHwCD3XyuvPEbvqAQY3rAPshWcMLoP2fMFMKHPJ4ZeZXYVUhLv1VMrjPC7PW6V/0/*)"
        val result = detector.detect(descriptor)

        assertEquals(InputType.DESCRIPTOR, result.inputType)
        assertEquals(Network.ElectrumMainnet, result.network)
        assertEquals(WatchOnlyCredentialType.CORE_DESCRIPTORS, result.credentialType)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun testDetect_LiquidDescriptor() {
        val descriptor = "slip77([73c5da0a/0']0217e1deb7e0fe5046a95021877a678e9f99b4c5c58d38c45fb9f79e5f6e2ad479)"
        val result = detector.detect(descriptor)

        assertEquals(InputType.DESCRIPTOR, result.inputType)
        assertEquals(Network.ElectrumLiquid, result.network)
        assertEquals(WatchOnlyCredentialType.CORE_DESCRIPTORS, result.credentialType)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun testDetect_MultipleInputs() {
        val multipleXpubs = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8,ypub6QqdH2c5z79681jUgdxjGJzGW9zpL4ryPCPufLLTgNQbaRfncsQiF8fWTDPivTGUFHWe2yUgBGZfhwJZfbNQjLPqmNPQiPPCHRmBe8bEHKK"
        val result = detector.detect(multipleXpubs)

        assertEquals(InputType.MULTIPLE, result.inputType)
        assertEquals(Network.ElectrumMainnet, result.network)
        assertEquals(WatchOnlyCredentialType.SLIP132_EXTENDED_PUBKEYS, result.credentialType)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun testDetect_Invalid() {
        val result = detector.detect("invalid input")

        assertEquals(InputType.INVALID, result.inputType)
        assertNull(result.network)
        assertNull(result.credentialType)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testDetect_BCUR() {
        val result = detector.detect("ur:crypto-output/1-2")

        assertEquals(InputType.BCUR, result.inputType)
        assertFalse(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun testDetect_EmptyMultiple() {
        val result = detector.detect(",,\n\n")

        assertEquals(InputType.MULTIPLE, result.inputType)
        assertFalse(result.isValid)
        assertEquals("No valid inputs found", result.errorMessage)
    }


    // Test edge cases for network detection
    @Test
    fun testDetectNetwork_EdgeCases() {
        // Test with whitespace
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("  xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8  "))

        // Test descriptor with fingerprint
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("[73c5da0a/84'/0'/0']xpub..."))
        assertEquals(Network.ElectrumTestnet, detector.detectNetwork("[73c5da0a/84'/1'/0']tpub..."))

        // Test nested descriptors
        assertEquals(Network.ElectrumMainnet, detector.detectNetwork("sh(wpkh([73c5da0a/49'/0'/0']xpub...))"))

        // Test Liquid with blinded descriptor
        assertEquals(Network.ElectrumLiquid, detector.detectNetwork("blinded(slip77([73c5da0a/0']...))"))
    }

    // Test improved input type detection
    @Test
    fun testDetectInputType_ComplexCases() {
        // Test with complex separators
        assertEquals(InputType.MULTIPLE, detector.detectInputType("xpub1, xpub2 | xpub3\nxpub4"))

        // Test BCUR with different formats
        assertEquals(InputType.BCUR, detector.detectInputType("ur:crypto-psbt/1-2/lpad..."))
        assertEquals(InputType.BCUR, detector.detectInputType("UR:CRYPTO-OUTPUT/1OF3/..."))

        // Test descriptor with nested functions
        assertEquals(InputType.DESCRIPTOR, detector.detectInputType("sh(wpkh(xpub...))"))
        assertEquals(InputType.DESCRIPTOR, detector.detectInputType("wsh(multi(2,xpub1,xpub2,xpub3))"))
    }

    // Test BCUR detection (format only, not decoding)
    @Test
    fun testDetect_BCUR_FormatDetection() {
        val bcurInput = "ur:crypto-output/1-2/lpadascesahahaohdcxmngthplerflsnyabdmotpsslnhtlahboshdtghcerkpfnepemylfgmecyjkjshd"
        val result = detector.detect(bcurInput)

        assertEquals(InputType.BCUR, result.inputType)
        assertFalse(result.isValid)
        assertNull(result.errorMessage)
    }

    // Test detection result completeness
    @Test
    fun testDetectionResult_AllFields() {
        val xpub = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"
        val result = detector.detect(xpub)

        assertNotNull(result.inputType)
        assertNotNull(result.network)
        assertNotNull(result.credentialType)
        assertNotNull(result.isValid)
        assertNull(result.errorMessage) // Should be null for valid input
    }
}