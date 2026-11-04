/**
 * Instrumented tests for GreenlightSdk.isLightningAvailable. glsdk ships its native library only
 * as Android .so files, so these run on a device and ask the live Greenlight scheduler.
 *
 * The answer for an unregistered node is advisory and depends on the Lightning provider's
 * capacity, so the live test only checks that Greenlight answers, not what it answers.
 */
package com.blockstream.data.lightning

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class GreenlightAvailabilityTest {

    @Test
    fun greenlightAnswersForAValidMnemonic() = runTest {
        GreenlightSdk.isLightningAvailable(mnemonic = BIP39_TEST_VECTOR)
    }

    @Test
    fun rejectsAnInvalidMnemonic() = runTest {
        assertFailsWith<Exception> {
            GreenlightSdk.isLightningAvailable(mnemonic = "not a mnemonic")
        }
    }

    companion object {
        // The public all-zero-entropy BIP39 test vector, not a wallet anyone uses
        private const val BIP39_TEST_VECTOR =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    }
}
