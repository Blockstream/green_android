package com.blockstream.common.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private fun processId(id: String): Pair<String, Array<String>> {
    if (id.startsWith("id_")) {
        val res = id.substring(0, id.indexOf("|").takeIf { it != -1 } ?: id.length)
        val formatArgs = id.split("|").drop(1).toTypedArray()

        return res to formatArgs
    } else if (id.contains("Breez SDK error", ignoreCase = true)) {
        val message = try {
            id.substring(id.indexOf("message: "))
        } catch (e: Exception) {
            id.replace("Breez SDK error:", "")
        }

        return "id_an_unidentified_error_occurred" to arrayOf(message)
    }

    return id to arrayOf()
}

@Composable
fun stringResourceFromId(id: String): String {
    return processId(id).let {
        StringResourcesMap.strings[it.first]?.let { stringRes ->
            stringResource(stringRes, *it.second)
        } ?: id
    }
}

@Composable
fun stringResourceFromIdOrNull(id: String?): String? = id?.let { stringResourceFromId(it) }

suspend fun getStringFromId(id: String): String {
    return processId(id).let {
        StringResourcesMap.strings[it.first]?.let { stringRes ->
            getString(stringRes, *it.second)
        } ?: id
    }
}

suspend fun getStringFromIdOrNull(id: String?): String? = id?.let { getStringFromId(it) }