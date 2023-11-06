package com.blockstream.compose.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref

/**
 * Wrapper around [AnimatedVisibility] that remembers the last state so that a nullable
 * state value can be used. Otherwise only the animate in works and the animate out is just
 * a quick flash.
 *
 * https://stackoverflow.com/a/77316971/914358
 */
@Composable
inline fun <T> AnimatedNullableVisibility(
    value: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    crossinline content: @Composable (scope: AnimatedVisibilityScope, T) -> Unit
) {
    val ref = remember {
        Ref<T>()
    }

    ref.value = value ?: ref.value

    AnimatedVisibility(
        modifier = modifier,
        visible = value != null,
        enter = enter,
        exit = exit,
        content = {
            ref.value?.let { value ->
                content(this, value)
            }
        }
    )
}