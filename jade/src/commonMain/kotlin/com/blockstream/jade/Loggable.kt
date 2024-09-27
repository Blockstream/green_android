package com.blockstream.jade

import co.touchlab.kermit.Logger

abstract class Loggable {
    protected val logger by lazy {
        Logger.withTag(
            this::class.qualifiedName?.removeSuffix(".Companion")?.splitToSequence('.')?.lastOrNull() ?: "GreenLoggable"
        )
    }
}