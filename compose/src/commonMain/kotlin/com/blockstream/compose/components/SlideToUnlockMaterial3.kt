@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.components

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import co.touchlab.kermit.Logger
import com.blockstream.green.utils.Loggable
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_backgroundVariant
import com.blockstream.compose.theme.whiteHigh
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
//
//@Composable
//fun SlideToUnlock(
//    isLoading: Boolean,
//    onSlideComplete: () -> Unit,
//    modifier: Modifier = Modifier,
//    enabled: Boolean = true,
//) {
//    val density = LocalDensity.current
//    val hapticFeedback = LocalHapticFeedback.current
//    var lastAnchor by remember { mutableStateOf(Anchor.Start) }
//    var lastIsLoading by remember { mutableStateOf(isLoading) }
//
//    LaunchedEffect(isLoading) {
//        lastIsLoading = isLoading
//    }
//    val swipeState = remember {
//        AnchoredDraggableState(
//            initialValue = if (isLoading) Anchor.End else Anchor.Start,
//            positionalThreshold = { distance: Float ->
//                (distance * 0.95f)
//            },
//            velocityThreshold = { with(density) { Track.VelocityThreshold.toDp().toPx() } },
//            snapAnimationSpec = tween(),
//            decayAnimationSpec = exponentialDecay(),
//            confirmValueChange = { anchor ->
//                Logger.d { "confirmValueChange: $anchor" }
//                if (!lastIsLoading && lastAnchor == Anchor.Start && anchor == Anchor.End) {
//                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
//                    onSlideComplete()
//                }
//                lastAnchor = anchor
//                true
//            }
//        )
//    }
//
//    LaunchedEffect(isLoading) {
//       swipeState.animateTo(if (isLoading) Anchor.End else Anchor.Start)
//    }
//
//    val swipeFraction by remember {
//        derivedStateOf {
//            calculateSwipeFraction(
//                swipeState
//            )
//        }
//    }
//
//
//    Track(
//        swipeState = swipeState,
//        swipeFraction = swipeFraction,
//        enabled = !isLoading && enabled,
//        modifier = modifier,
//    ) {
//        Hint(
//            text = stringResource(Res.string.id_slide_to_send),
//            swipeFraction = swipeFraction,
//            modifier = Modifier
//                .align(Alignment.Center)
//                .padding(PaddingValues(horizontal = Thumb.Size + 8.dp)),
//            enabled = enabled
//        )
//
//        Thumb(
//            isLoading = isLoading,
//            enabled = enabled,
//            modifier = Modifier.offset {
//                if(swipeState.offset.isNaN()){
//                    IntOffset(0, 0)
//                }else{
//                    IntOffset(swipeState.offset.roundToInt(), 0)
//                }
//            }
//        )
//    }
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//fun calculateSwipeFraction(
//    state: AnchoredDraggableState<Anchor>
//): Float {
//    val progress: Float = state.progress(state.settledValue, Anchor.End)
//    val current: Anchor = state.currentValue
//    val target: Anchor = state.targetValue
//    val atAnchor = current == target && (progress == 0.0f || progress == 1f)
//    val fromStart = current == Anchor.Start
//    return if (atAnchor) {
//        if (fromStart) 0f else 1f
//    } else {
//        if (fromStart) progress else 1f - progress
//    }
//}
//
//fun calculateSwipeFraction(
//    progress: Float,
//    current: Anchor,
//    target: Anchor
//): Float {
//    Logger.d { "progress: $progress, current: $current, target: $target" }
//    val atAnchor = current == target && (progress == 0.0f || progress == 1f)
//    val fromStart = current == Anchor.Start
//    return if (atAnchor) {
//        if (fromStart) 0f else 1f
//    } else {
//        if (fromStart) progress else 1f - progress
//    }
//}
//
//enum class Anchor { Start, End }
//
//@Composable
//fun Track(
//    swipeState: AnchoredDraggableState<Anchor>,
//    swipeFraction: Float,
//    enabled: Boolean,
//    modifier: Modifier = Modifier,
//    content: @Composable (BoxScope.() -> Unit),
//) {
//    val density = LocalDensity.current
//    var fullWidth by remember { mutableIntStateOf(0) }
//
//    val horizontalPadding = 10.dp
//
//    val endOfTrackPx = remember(fullWidth) {
//        with(density) { fullWidth - (2 * horizontalPadding + Thumb.Size).toPx() }
//    }
//
//    val anchors = DraggableAnchors {
//        Anchor.Start at 0f
//        Anchor.End at endOfTrackPx
//    }
//
//    val backgroundColor by remember(swipeFraction) {
//        derivedStateOf { calculateTrackColor(swipeFraction) }
//    }
//
//    LaunchedEffect(anchors) {
//        swipeState.updateAnchors(anchors)
//    }
//
//    Box(
//        modifier = modifier
//            .onSizeChanged { fullWidth = it.width }
//            .height(56.dp)
//            .fillMaxWidth()
//            .anchoredDraggable(
//                state = swipeState,
//                orientation = Orientation.Horizontal,
//                enabled = enabled
//            )
//            .background(
//                color = backgroundColor,
//                shape = RoundedCornerShape(percent = 50),
//            )
//            .padding(
//                PaddingValues(
//                    horizontal = horizontalPadding,
//                    vertical = 8.dp,
//                )
//            ),
//        content = content,
//    )
//}
//
//fun calculateTrackColor(swipeFraction: Float): Color {
//    val endOfColorChangeFraction = 1.0f
//    val fraction = (swipeFraction / endOfColorChangeFraction).coerceIn(0f..1f)
//    return lerp(md_theme_backgroundVariant, green, fraction)
//}
//
//@Composable
//fun Thumb(
//    isLoading: Boolean,
//    enabled: Boolean,
//    modifier: Modifier = Modifier,
//) {
//    Box(
//        modifier = modifier
//            .size(Thumb.Size)
//            .background(
//                color = if (enabled) green else ButtonDefaults.buttonColors().disabledContainerColor,
//                shape = CircleShape
//            )
//            .padding(8.dp),
//    ) {
//        if (isLoading) {
//            CircularProgressIndicator(
//                modifier = Modifier.padding(2.dp),
//                color = whiteHigh,
//                strokeWidth = 2.dp
//            )
//        } else {
//            Image(
//                painter = painterResource(Res.drawable.arrow_fat_lines_right),
//                contentDescription = null,
//                colorFilter = ColorFilter.tint(if(enabled) whiteHigh else ButtonDefaults.buttonColors().disabledContentColor)
//            )
//        }
//    }
//}
//
//@Composable
//fun Hint(
//    text: String,
//    swipeFraction: Float,
//    enabled: Boolean,
//    modifier: Modifier = Modifier,
//) {
////    val hintTextColor by remember(swipeFraction) {
////        derivedStateOf { calculateHintTextColor(swipeFraction) }
////    }
//
//    Text(
//        text = text,
//        color = if(enabled) whiteHigh else ButtonDefaults.buttonColors().disabledContentColor,
//        style = labelLarge,
//        modifier = modifier
//    )
//}
//
////fun calculateHintTextColor(swipeFraction: Float): Color {
////    val endOfFadeFraction = 0.9f
////    val fraction = (swipeFraction / endOfFadeFraction).coerceIn(0f..1f)
////    return lerp(Color.White, Color.White.copy(alpha = 0f), fraction)
////}
//
//
//private object Thumb {
//    val Size = 40.dp
//}
//
//internal object Track {
//    val VelocityThreshold = 1000f * 10
//}
//
//@Preview
//@Composable
//fun preview(){
//    SlideToUnlock(
//        isLoading = false,
//        onSlideComplete = {
//
//        },
//    )
//}