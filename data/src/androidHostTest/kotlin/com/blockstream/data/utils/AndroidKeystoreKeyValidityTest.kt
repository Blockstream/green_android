/**
 * Tests for AndroidKeystore.keyRemainsValid: classification of exceptions raised while
 * probing a keystore key, deciding whether the shared biometrics key may be deleted and
 * recreated (invalid) or must be retained (valid).
 *
 * The android.security.keystore exception branches (KeyPermanentlyInvalidatedException,
 * UserNotAuthenticatedException) cannot be constructed in a host test (stub constructors),
 * so only the JVM-instantiable classifications are covered here.
 */
package com.blockstream.data.utils

import java.security.UnrecoverableKeyException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidKeystoreKeyValidityTest {

    @Test
    fun unrecoverableKeyIsInvalid() {
        assertFalse(AndroidKeystore.keyRemainsValid(UnrecoverableKeyException("cannot recover key")))
    }

    @Test
    fun unknownErrorRetainsKey() {
        assertTrue(AndroidKeystore.keyRemainsValid(RuntimeException("OEM keystore quirk")))
    }
}
