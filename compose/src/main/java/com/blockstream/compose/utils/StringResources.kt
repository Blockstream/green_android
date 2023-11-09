package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
@ReadOnlyComposable
fun stringResourceIdOrNull(id: String): String? {
    if (id.startsWith("id_")) {
        val res = id.substring(0, id.indexOf("|").takeIf { it != -1 } ?: id.length)
        val formatArgs = (id.split("|").filterIndexed { index, _ -> index != 0 }.toTypedArray())

        // Emulate compose resources() method
        LocalConfiguration.current
        val resources = LocalContext.current.resources

        val intRes = resources.getIdentifier(res, "string", LocalContext.current.packageName)
        if (intRes > 0) {
            return stringResource(id = intRes, formatArgs = formatArgs)
        }
    }
    return null
}

@Composable
@ReadOnlyComposable
fun stringResourceId(id: String): String {
    return stringResourceIdOrNull(id = id) ?: id
}