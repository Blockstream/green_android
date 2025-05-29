@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi

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