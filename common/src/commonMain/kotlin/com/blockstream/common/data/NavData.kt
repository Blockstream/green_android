package com.blockstream.common.data

import androidx.compose.runtime.Immutable
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class NavData(
    val title: String? = null,
    val subtitle: String? = null,
    val isVisible: Boolean = true,
    val onBackPressed: () -> Boolean = { true },
    val actions: List<NavAction> = listOf()
) : GreenJson<NavData>() {
    override fun kSerializer() = serializer()
}


@Serializable
data class NavAction(
    val title: String,
    val icon: String? = null,
    val isMenuEntry: Boolean = false,
    val onClick: () -> Unit = { }
) : GreenJson<NavAction>() {
    override fun kSerializer() = serializer()
}