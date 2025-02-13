package com.blockstream.ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    return PaddingValues(
        start = calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        end = calculateStartPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        bottom = calculateBottomPadding() + other.calculateBottomPadding()
    )
}

@Composable
fun PaddingValues.excludeTop(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    return PaddingValues(
        top = 0.dp,
        start = calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding()
    )
}

@Composable
fun PaddingValues.excludeBottom(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    return PaddingValues(
        top = calculateTopPadding(),
        start = calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection),
        bottom = 0.dp
    )
}

@Composable
fun PaddingValues.bottom(): PaddingValues {
    return PaddingValues(
        bottom = calculateBottomPadding()
    )
}


