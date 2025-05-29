package com.blockstream.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class RandomTests {

    private val secureRandom = getSecureRandom()

    @Test
    fun test_randomBytes_randomness() {
        val checks = 10_000
        val random = ArrayList<String>(checks)

        (0..checks).forEach {
            secureRandom.randomBytes(8).toHexString().also {
                assertFalse(random.contains(it))
                random.add(it)
            }
        }
    }

    @Test
    fun test_randomBytes_size() {
        (0..1000).forEach {
            assertEquals(it, secureRandom.randomBytes(it).size)
        }
    }

    @Test
    fun test_unsecureRandomInt_randomness() {
        val checks = 10_000
        val random = ArrayList<Int>(checks)
        (0..checks).forEach {
            secureRandom.unsecureRandomInt().also {
                assertFalse(random.contains(it))
                random.add(it)
            }
        }
    }

    @Test
    fun test_unsecureRandomInt_until() {
        (1..1000).forEach { i ->
            secureRandom.unsecureRandomInt(i).also { random ->
                assertTrue(random < i)
            }
        }
    }

    @Test
    fun test_unsecureRandomInt_range() {
        (1..1000).forEach { i ->
            val range = i..(i + 1000)
            secureRandom.unsecureRandomInt(range.first, range.last).also { random ->
                println("$range -> $random")
                assertTrue(random in range)
            }
        }
    }
}