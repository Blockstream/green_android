package com.blockstream.compose.designv2

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.blockstream.compose.navigation.NavTransitionSpec
import com.blockstream.compose.navigation.TopLevelRoutes

object NavTransitionsV2 : NavTransitionSpec {
    private const val DURATION_MS = 300
    private const val DIMMED_ALPHA = 0.5f
    private val Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    private const val TAB_FADE_MS = 200

    override val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch()) fadeIn(tween(TAB_FADE_MS)) else slideInHorizontally(tween(DURATION_MS, easing = Easing)) { it }
    }

    override val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch()) {
            fadeOut(tween(TAB_FADE_MS))
        } else {
            slideOutHorizontally(tween(DURATION_MS, easing = Easing)) { -it / 4 } +
                fadeOut(tween(DURATION_MS, easing = Easing), targetAlpha = DIMMED_ALPHA)
        }
    }

    override val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch()) {
            fadeIn(tween(TAB_FADE_MS))
        } else {
            slideInHorizontally(tween(DURATION_MS, easing = Easing)) { -it / 4 } +
                fadeIn(tween(DURATION_MS, easing = Easing), initialAlpha = DIMMED_ALPHA)
        }
    }

    override val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch()) fadeOut(tween(TAB_FADE_MS)) else slideOutHorizontally(tween(DURATION_MS, easing = Easing)) { it }
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
        initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

    private fun NavDestination.isTopLevel(): Boolean = TopLevelRoutes.any { hasRoute(it.klass) }
}
