package com.blockstream.compose.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

fun Modifier.appTestTag(
    tag: String?,
): Modifier = then(ifNotNull(tag) {
    val commonizeTestTag = commonizeTestTag(this)
    it.testTag(commonizeTestTag)
})

private fun commonizeTestTag(tag: String): String {
    return tag
        .lowercase()
        .replace(' ', '_')
}