@file:OptIn(ExperimentalMaterialApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.GreenAndroidPreview


@Preview
@Composable
private fun Preview() {
    var enabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    GreenAndroidPreview {
        val spacing = 88.dp
        Column(
            verticalArrangement = spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(spacing))

            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Normal")
                    Spacer(modifier = Modifier.weight(1f))
                    Thumb(isLoading = false)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Loading")
                    Spacer(modifier = Modifier.widthIn(min = 16.dp))
                    Thumb(isLoading = true)
                }


            }

            Spacer(modifier = Modifier.height(spacing))

            Text(text = "Inactive")
            Track(
                swipeState = SwipeableState(Anchor.Start),
                swipeFraction = 0f,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Active")
            Track(
                swipeState = SwipeableState(Anchor.Start),
                swipeFraction = 1f,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )

            Spacer(modifier = Modifier.height(spacing))

            SlideToUnlock(
                isLoading = isLoading,
                enabled = enabled,
                onSlideComplete = { isLoading = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            GreenButton("Cancel loading") {
                isLoading = false
            }

            GreenButton("Toggle Enabled") {
                enabled = !enabled
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}