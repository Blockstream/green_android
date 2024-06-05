package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.utils.ifTrue

@Composable
fun ScreenContainer(
    onProgress: Boolean,
    onProgressDescription: String? = null,
    riveAnimation: RiveAnimation? = null,
    blurBackground: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier
            .fillMaxSize()
            .ifTrue(onProgress) {
                background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                    .blur(6.dp)
            }
    ) {
        content(this)
    }

    AnimatedVisibility(
        onProgress,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Catch all click events
                    }
                }
                .background(MaterialTheme.colorScheme.background.let {
                    if (blurBackground) it.copy(
                        alpha = 0.75f
                    ) else it
                })
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(space = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                if (riveAnimation != null) {
                    Rive(riveAnimation = riveAnimation)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(120.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                onProgressDescription?.also {
                    Text(text = it, style = labelLarge)
                }
            }
        }
    }
}