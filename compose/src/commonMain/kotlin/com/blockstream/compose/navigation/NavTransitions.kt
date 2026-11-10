package com.blockstream.compose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

interface NavTransitionSpec {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition
        get() = { popEnter() }
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition
        get() = { popExit() }
}

object NavTransitions : NavTransitionSpec {
    override val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 })
    }

    override val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut() + slideOutHorizontally()
    }

    override val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn() + slideInHorizontally()
    }

    override val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    }
}
