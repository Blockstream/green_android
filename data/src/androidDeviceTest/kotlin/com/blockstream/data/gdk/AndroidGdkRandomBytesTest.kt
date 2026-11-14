/**
 * Instrumented tests for AndroidGdk.getRandomBytes. The method forwards to the native
 * GDK.get_random_bytes, whose library only ships as Android .so files, so the assertions
 * here run on a device against the real native call.
 *
 * These are not a randomness test. The quality of the random source is GDK's
 * responsibility and is tested there. The draw-based checks below only confirm that the
 * binding reaches the native function and hands back what it produced, not a fixed or
 * truncated buffer.
 */
package com.blockstream.data.gdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.touchlab.kermit.Logger
import com.blockstream.data.gdk.params.InitConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalStdlibApi::class)
@RunWith(AndroidJUnit4::class)
class AndroidGdkRandomBytesTest {

    @Test
    fun returnsRequestedNumberOfBytes() {
        listOf(1, 16, 32).forEach { size ->
            assertEquals(size, gdk.getRandomBytes(size).size)
        }
    }

    @Test
    fun returnsBytesThatAreNotAllZero() {
        assertTrue(gdk.getRandomBytes(32).any { it != 0.toByte() })
    }

    @Test
    fun consecutiveCallsReturnDifferentBytes() {
        val first = gdk.getRandomBytes(32)
        val second = gdk.getRandomBytes(32)

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun rejectsSizesAbove32Bytes() {
        val error = assertThrows(RuntimeException::class.java) { gdk.getRandomBytes(33) }

        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("GA_get_random_bytes"))
    }

    @Test
    fun manyDrawsNeverRepeat() {
        val draws = 10_000
        val seen = HashSet<String>(draws)

        repeat(draws) {
            assertTrue(seen.add(gdk.getRandomBytes(16).toHexString()))
        }
    }

    @Test
    fun singleByteDrawsCollide() {
        val draws = 1_000
        val seen = HashSet<Byte>()

        repeat(draws) { seen.add(gdk.getRandomBytes(1).single()) }

        // 1,000 uniform draws from 256 values yield about 251 distinct values (sd ~2).
        assertTrue("distinct values: ${seen.size}", seen.size in 240..256)
    }

    @Test
    fun everyByteValueOccursAcrossManyDraws() {
        val counts = IntArray(256)

        repeat(10_000) {
            gdk.getRandomBytes(32).forEach { counts[it.toInt() and 0xff]++ }
        }

        assertEquals(emptyList<Int>(), counts.withIndex().filter { it.value == 0 }.map { it.index })
    }

    companion object {
        // GDK.init may only run once per process, so every test shares a single binding.
        private val gdk: GdkBinding by lazy {
            val dataDir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "gdk-test")
                .apply { mkdirs() }
            AndroidGdk(
                printGdkMessages = false,
                config = InitConfig(datadir = dataDir.absolutePath),
                logger = Logger.withTag("GDK")
            )
        }
    }
}
