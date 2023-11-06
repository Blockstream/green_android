package com.blockstream.compose.utils

import androidx.compose.ui.Modifier

inline fun Modifier.ifTrue(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
): Modifier = if (condition) {
    then(ifTrue(Modifier))
} else {
    this
}

inline fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: Modifier.() -> Modifier = { this },
): Modifier = if (condition) {
    then(ifTrue(Modifier))
} else {
    then(ifFalse(Modifier))
}

inline fun <T> Modifier.nullConditional(
    argument: T?,
    ifNotNull: Modifier.(T) -> Modifier,
    ifNull: Modifier.() -> Modifier = { this },
): Modifier {
    return if (argument != null) {
        then(ifNotNull(Modifier, argument))
    } else {
        then(ifNull(Modifier))
    }
}