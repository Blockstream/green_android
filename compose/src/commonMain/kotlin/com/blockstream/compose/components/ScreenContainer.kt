package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.compose.theme.titleMedium
import com.blockstream.ui.models.IOnProgress
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.utils.excludeBottom
import com.blockstream.compose.utils.ifTrue

sealed class OnProgressStyle {
    data object Disabled : OnProgressStyle()
    data object Top : OnProgressStyle()
    data class Full(
        val bluBackground: Boolean = true,
        val riveAnimation: RiveAnimation? = null
    ) : OnProgressStyle()
}

@Composable
fun ScreenContainer(
    viewModel: IOnProgress,
    scrollable: Boolean = false,
    withPadding: Boolean = true,
    withImePadding: Boolean = false,
    withInsets: Boolean = true,
    withBottomInsets: Boolean = true,
    withBottomNavBarPadding: Boolean = false,
    onProgressStyle: OnProgressStyle = OnProgressStyle.Top,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(innerPadding: PaddingValues) -> Unit
) {
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val onProgressDescription by viewModel.onProgressDescription.collectAsStateWithLifecycle()

    val innerPadding = LocalInnerPadding.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .ifTrue(scrollable) {
                it.verticalScroll(rememberScrollState())
            }.ifTrue(withPadding) {
                it.padding(16.dp)
            }.ifTrue(withInsets) {
                if (withBottomInsets) {
                    it.consumeWindowInsets(innerPadding)
                        .padding(innerPadding)
                } else {
                    innerPadding.excludeBottom().let { excluded ->
                        it.consumeWindowInsets(excluded)
                            .padding(excluded)
                    }
                }
            }.ifTrue(withBottomNavBarPadding) {
                it.padding(bottom = 80.dp)
            }.ifTrue(withImePadding) {
                it.imePadding()
            }.then(modifier),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = {
            content(innerPadding)
        }
    )

    when (onProgressStyle) {
        is OnProgressStyle.Full -> {
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
                            if (onProgressStyle.bluBackground) it.copy(
                                alpha = 0.75f
                            ) else it
                        })
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {

                        if (onProgressStyle.riveAnimation != null) {
                            Rive(riveAnimation = onProgressStyle.riveAnimation)
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(120.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }

                        onProgressDescription?.also {
                            Text(text = it, style = titleMedium)
                        }
                    }
                }
            }
        }

        OnProgressStyle.Top -> {
            AnimatedVisibility(
                visible = onProgress,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding() - 64.dp)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                )
            }
        }

        OnProgressStyle.Disabled -> {

        }
    }
}