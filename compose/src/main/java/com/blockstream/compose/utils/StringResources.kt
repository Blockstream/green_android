package com.blockstream.compose.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

@Composable
@ReadOnlyComposable
fun stringResourceIdOrNull(id: String?): String? {
    if (id?.startsWith("id_") == true) {
        return stringResourceId(LocalContext.current, id)
    }
    return null
}

@Composable
@ReadOnlyComposable
fun stringResourceId(id: String): String {
    return stringResourceIdOrNull(id = id) ?: id
}

fun stringResourceId(context: Context, id: String): String {
    return stringResourceIdOrNull(context = context, id = id) ?: id
}

fun stringResourceIdOrNull(context: Context, id: String?): String? {
    if (id?.startsWith("id_") == true) {
        val res = id.substring(0, id.indexOf("|").takeIf { it != -1 } ?: id.length)
        val formatArgs = id.split("|").drop(1).toTypedArray()

        val resources = context.resources

        val intRes = resources.getIdentifier(res, "string", context.packageName)
        if (intRes > 0) {
            return context.getString(intRes, *formatArgs)
        }
    }
    return null
}