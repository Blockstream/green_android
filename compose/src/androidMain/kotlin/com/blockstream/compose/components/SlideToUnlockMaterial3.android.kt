@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi

//@OptIn(ExperimentalFoundationApi::class)
//@Preview
//@Composable
//private fun Preview() {
//    var isLoading by remember { mutableStateOf(false) }
//    GreenAndroidPreview {
//        val spacing = 88.dp
//        Column(
//            verticalArrangement = spacedBy(8.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(24.dp),
//        ) {
//
//            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
//                Row(
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    Text(text = "Normal")
//                    Spacer(modifier = Modifier.weight(1f))
//                    Thumb(isLoading = false, enabled = true)
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                Row(
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    Text(text = "Loading")
//                    Spacer(modifier = Modifier.widthIn(min = 16.dp))
//                    Thumb(isLoading = true, enabled = true)
//                }
//            }
//
//            Spacer(modifier = Modifier.height(spacing))
//
//            val density = LocalDensity.current
//            val swipeState = remember {
//                AnchoredDraggableState(
//                    initialValue = if (isLoading) Anchor.End else Anchor.Start,
//                    positionalThreshold = { distance: Float -> distance * 0.9f },
//                    velocityThreshold = { with(density) { Track.VelocityThreshold.toDp().toPx() } },
//                    snapAnimationSpec = tween(),
//                    decayAnimationSpec = exponentialDecay()
//                )
//            }
//
//            Text(text = "Inactive")
//            Track(
//                swipeState = swipeState,
//                swipeFraction = 0f,
//                enabled = true,
//                modifier = Modifier.fillMaxWidth(),
//                content = {},
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(text = "Active")
//            Track(
//                swipeState = swipeState,
//                swipeFraction = 1f,
//                enabled = true,
//                modifier = Modifier.fillMaxWidth(),
//                content = {},
//            )
//
//            Spacer(modifier = Modifier.height(spacing))
//
//
//            SlideToUnlock(
//                isLoading = isLoading,
//                onSlideComplete = { isLoading = true },
//            )
//            Spacer(modifier = Modifier.weight(1f))
//
//            GreenButton(text = "Load") {
//                isLoading = true
//            }
//
//            GreenButton(text = "Cancel Loading") {
//                isLoading = false
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//        }
//    }
//}