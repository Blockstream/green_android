// What the JVM the app runs on reports about itself, from standard system properties and Runtime, for the
// Developer settings. Both targets (Android and desktop) are JVMs, so common code can read them.
package com.blockstream.compose.utils

private val RuntimeProperties = listOf(
    "java.runtime.name",
    "java.runtime.version",
    "java.vendor",
    "java.vm.name",
    "java.vm.version",
    "java.home",
    "os.name",
    "os.version",
    "os.arch",
)

private const val BYTES_PER_MIB = 1024L * 1024L

/** The runtime properties that are set, as key and value, then the processor count and maximum heap. */
fun runtimeInfo(
    property: (String) -> String? = System::getProperty,
    availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
    maxMemoryBytes: Long = Runtime.getRuntime().maxMemory(),
): List<Pair<String, String>> =
    RuntimeProperties.mapNotNull { key -> property(key)?.let { key to it } } + listOf(
        "availableProcessors" to availableProcessors.toString(),
        "maxMemory" to "${maxMemoryBytes / BYTES_PER_MIB} MiB",
    )
