package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteMedium

@Composable
fun GreenDataLayout(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    badge: String? = null,
    border: BorderStroke? = null,
    helperText: String? = null,
    helperContainerColor: Color? = null,
    withPadding: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        title?.also {
            Text(
                text = it,
                style = bodyLarge,
                color = whiteMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Box {

            GreenCard(
                padding = (if (withPadding) 16 else 0),
                onClick = onClick,
                testTag = testTag,
                enabled = enabled,
                border = border,
                helperText = helperText,
                helperContainerColor = helperContainerColor,
                content = content
            )

            badge?.also {
                var offset by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                Badge(
                    containerColor = green,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp)
                        .onGloballyPositioned { coordinates ->
                            offset = with(density) {
                                -(coordinates.size.height / 2).toDp()
                            }
                        }
                        .offset(y = offset)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }

        subtitle?.also {
            Text(
                text = it,
                style = bodyLarge,
                color = whiteMedium,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}