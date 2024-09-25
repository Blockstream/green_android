@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeProgress
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_fat_lines_right
import blockstream_green.common.generated.resources.id_slide_to_send
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_backgroundVariant
import com.blockstream.compose.theme.whiteHigh
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SlideToUnlock(
    isLoading: Boolean,
    enabled: Boolean = true,
    onSlideComplete: () -> Unit,
    hint: StringHolder? = null,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val swipeState = rememberSwipeableState(
        initialValue = if (isLoading) Anchor.End else Anchor.Start,
        confirmStateChange = { anchor ->
            if (anchor == Anchor.End) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onSlideComplete()
            }
            true
        }
    )

    val swipeFraction by remember {
        derivedStateOf { calculateSwipeFraction(swipeState.progress) }
    }

    LaunchedEffect(isLoading) {
        swipeState.animateTo(if (isLoading) Anchor.End else Anchor.Start)
    }

    Track(
        swipeState = swipeState,
        swipeFraction = swipeFraction,
        enabled = !isLoading && enabled,
        modifier = modifier,
    ) {
        Hint(
            text = hint?.stringOrNull() ?: stringResource(Res.string.id_slide_to_send),
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PaddingValues(horizontal = Thumb.Size + 8.dp)),
        )

        Thumb(
            isLoading = isLoading,
            enabled = enabled,
            modifier = Modifier.offset {
                IntOffset(swipeState.offset.value.roundToInt(), 0)
            },
        )
    }
}

fun calculateSwipeFraction(progress: SwipeProgress<Anchor>): Float {
    val atAnchor = progress.from == progress.to
    val fromStart = progress.from == Anchor.Start
    return if (atAnchor) {
        if (fromStart) 0f else 1f
    } else {
        if (fromStart) progress.fraction else 1f - progress.fraction
    }
}

enum class Anchor { Start, End }

@Composable
fun Track(
    swipeState: SwipeableState<Anchor>,
    swipeFraction: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit),
) {
    val density = LocalDensity.current
    var fullWidth by remember { mutableIntStateOf(0) }

    val horizontalPadding = 10.dp

    val startOfTrackPx = 0f
    val endOfTrackPx = remember(fullWidth) {
        with(density) { fullWidth - (2 * horizontalPadding + Thumb.Size).toPx() }
    }

    val snapThreshold = 0.8f
    val thresholds = { from: Anchor, _: Anchor ->
        if (from == Anchor.Start) {
            FractionalThreshold(snapThreshold)
        } else {
            FractionalThreshold(1f - snapThreshold)
        }
    }

    val backgroundColor by remember(swipeFraction) {
        derivedStateOf { calculateTrackColor(swipeFraction) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { fullWidth = it.width }
            .height(56.dp)
            .fillMaxWidth()
            .swipeable(
                enabled = enabled,
                state = swipeState,
                orientation = Orientation.Horizontal,
                anchors = mapOf(
                    startOfTrackPx to Anchor.Start,
                    endOfTrackPx to Anchor.End,
                ),
                thresholds = thresholds,
                velocityThreshold = Track.VelocityThreshold,
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(
                PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = 8.dp,
                )
            ),
        content = content,
    )
}

fun calculateTrackColor(swipeFraction: Float): Color {
    val endOfColorChangeFraction = 1.0f
    val fraction = (swipeFraction / endOfColorChangeFraction).coerceIn(0f..1f)
    return lerp(md_theme_backgroundVariant, green, fraction)
}

@Composable
fun Thumb(
    isLoading: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Thumb.Size)
            .background(
                color = if (enabled) green else ButtonDefaults.buttonColors().disabledContainerColor,
                shape = CircleShape
            )
            .padding(8.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(2.dp),
                color = whiteHigh,
                strokeWidth = 2.dp
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.arrow_fat_lines_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if(enabled) whiteHigh else ButtonDefaults.buttonColors().disabledContentColor)
            )
        }
    }
}

@Composable
fun Hint(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = if(enabled) whiteHigh else ButtonDefaults.buttonColors().disabledContentColor,
        style = labelLarge,
        modifier = modifier
    )
}

private object Thumb {
    val Size = 40.dp
}

private object Track {
    val VelocityThreshold = SwipeableDefaults.VelocityThreshold * 10
}
