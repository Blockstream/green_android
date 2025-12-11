package com.blockstream.compose.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.blockstream.ui.navigation.Route
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val LocalNavigator: ProvidableCompositionLocal<NavHostController> =
    staticCompositionLocalOf { error("LocalNavigator not initialized") }

val LocalNavBackStackEntry: ProvidableCompositionLocal<NavBackStackEntry?> =
    compositionLocalOf { null }

val LocalInnerPadding: ProvidableCompositionLocal<PaddingValues> =
    compositionLocalOf { PaddingValues() }


@Serializable
data class Dialog @OptIn(ExperimentalUuidApi::class) constructor(
    override val uniqueId: String = Uuid.random().toHexString(),
    val title: String? = null,
    val message: String? = null,
    val confirmButtonText: String? = null,
    val dismissButtonText: String? = null,
) : Route {
    override val unique: Boolean = false
    override val makeItRoot: Boolean = false
    override val isBottomNavigation: Boolean = false
}

