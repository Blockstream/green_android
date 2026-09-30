package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.GreenSmallTop
import com.blockstream.compose.utils.appTestTag

@Composable
internal fun CardSurface(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    border: BorderStroke? = null,
    shape: Shape = CardDefaults.shape,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .appTestTag(testTag)
    val cardBorder = border ?: CardDefaults.outlinedCardBorder(enabled)

    if (onClick == null) {
        OutlinedCard(
            modifier = cardModifier,
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = cardBorder,
        ) {
            CardSurfaceContent(contentPadding, content)
        }
    } else {
        OutlinedCard(
            modifier = cardModifier,
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = cardBorder,
        ) {
            CardSurfaceContent(contentPadding, content)
        }
    }
}

@Composable
private fun CardSurfaceContent(
    contentPadding: PaddingValues,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        content = content,
    )
}

@Composable
internal fun SupportingPanel(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    shape: Shape = GreenSmallTop,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = colors,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}
