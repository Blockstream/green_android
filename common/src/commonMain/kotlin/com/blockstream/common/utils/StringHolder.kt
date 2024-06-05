package com.blockstream.common.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class StringHolder(
    val stringResource: StringResource? = null,
    val id: String? = null,
    val string: String? = null
) {
    @Composable
    fun stringOrNull(): String? = stringResource?.let { stringResource(stringResource) }
        ?: id?.let { stringResourceFromId(id) } ?: string

    @Composable
    fun string(): String = stringOrNull() ?: ""


    suspend fun getStringOrNull(): String? = stringResource?.let { org.jetbrains.compose.resources.getString(stringResource) }
        ?: id?.let { getStringFromId(id) } ?: string

    suspend fun getString(): String = getStringOrNull() ?: ""

    fun fallbackString() = string ?: id ?: stringResource?.key ?: ""

    override fun toString(): String = fallbackString()

    companion object {
        fun create(data: Any?): StringHolder {
            return if (data is StringResource) {
                StringHolder(stringResource = data)
            } else if (data is String) {
                if (data.startsWith("id_")) {
                    StringHolder(id = data)
                } else {
                    StringHolder(string = data)
                }
            } else { // Int / Long etc
                StringHolder(string = data.toString())
            }
        }
    }
}