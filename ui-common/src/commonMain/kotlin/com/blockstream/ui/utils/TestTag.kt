package com.blockstream.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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