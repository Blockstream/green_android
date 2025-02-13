package com.blockstream.green.utils

import co.touchlab.kermit.Logger

abstract class Loggable {
    protected val logger by lazy {
        Logger.withTag(
            this::class.qualifiedName?.removeSuffix(".Companion")?.splitToSequence('.')?.lastOrNull() ?: "Loggable"
        )
    }
}