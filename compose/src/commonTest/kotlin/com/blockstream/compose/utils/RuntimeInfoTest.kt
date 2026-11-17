package com.blockstream.compose.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeInfoTest {

    @Test
    fun listsTheRuntimePropertiesThatAreSetInOrderThenTheProcessorsAndMemory() {
        val properties = mapOf(
            "os.arch" to "aarch64",
            "java.vendor" to "JetBrains s.r.o.",
            "java.runtime.version" to "25.0.4.1+1-b329.142",
            "user.home" to "/Users/someone",
        )

        val info = runtimeInfo(property = properties::get, availableProcessors = 8, maxMemoryBytes = 4L * 1024 * 1024 * 1024)

        assertEquals(
            listOf(
                "java.runtime.version" to "25.0.4.1+1-b329.142",
                "java.vendor" to "JetBrains s.r.o.",
                "os.arch" to "aarch64",
                "availableProcessors" to "8",
                "maxMemory" to "4096 MiB",
            ),
            info
        )
    }

    @Test
    fun readsTheRuntimeTheTestsRunOn() {
        val info = runtimeInfo().toMap()

        assertEquals(System.getProperty("java.vm.name"), info["java.vm.name"])
        assertEquals(Runtime.getRuntime().availableProcessors().toString(), info["availableProcessors"])
        assertTrue(info.keys.none { it.startsWith("user.") })
    }
}
