package com.blockstream.compose.components

import android.view.LayoutInflater
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rive.runtime.kotlin.RiveAnimationView
import co.touchlab.kermit.Logger
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.utils.ifTrue
import com.blockstream.compose.utils.stringResourceId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScreenContainer(
    viewModel: GreenViewModel,
    showRiveAnimation: Boolean = false,
    blurBackground: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
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
                if (showRiveAnimation) {
                    AndroidView({
                        LayoutInflater.from(it)
                            .inflate(R.layout.rive, null)
                            .apply {
                                val animationView: RiveAnimationView = findViewById(R.id.rive)
                                animationView.setRiveResource(
                                    R.raw.rocket,
                                    autoplay = true
                                )
                            }
                    })
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(120.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                val onProgressDescription by viewModel.onProgressDescription.collectAsStateWithLifecycle()
                onProgressDescription?.also {
                    Text(text = stringResourceId(it), style = labelLarge)
                }
            }
        }
    }
}

@Composable
@Preview
fun ScreenContainerPreview() {
    GreenTheme {
        val viewModel = remember {
            object : GreenViewModel() {
                init {
                    onProgress.value = true
                    onProgressDescription.value = "On Progress Description..."
                }

                fun progress() {
                    onProgress.value = true
                    applicationScope.launch {
                        delay(1000L)
                        onProgress.value = false
                    }
                }
            }
        }

        ScreenContainer(viewModel = viewModel) {
            GreenColumn {
                Text(text = "Text")
                Text(text = "Text")
                Text(text = "Text")

                GreenButton(text = "Progress") {
                    Logger.d { "ON CLICK" }
                    viewModel.progress()
                }
            }
        }
    }
}