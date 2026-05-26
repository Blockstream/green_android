package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.warning_diamond
import com.blockstream.compose.theme.GreenSmallBottom
import com.blockstream.compose.theme.GreenSmallTop
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_errorContainer
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.stringResourceFromId
import org.jetbrains.compose.resources.painterResource

@Composable
fun GreenCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    enabled: Boolean = true,
    padding: Int = 16,
    border: BorderStroke? = null,
    helperText: String? = null,
    helperContent: (@Composable BoxScope.() -> Unit)? = null,
    helperContainerColor: Color? = null,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderStroke = border ?: CardDefaults.outlinedCardBorder(enabled)
    val hasHelper = helperText != null || helperContent != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        if (onClick == null) {
            OutlinedCard(
                modifier = Modifier.appTestTag(testTag),
                shape = if (!hasHelper) CardDefaults.shape else GreenSmallBottom,
                elevation = elevation,
                colors = colors,
                border = if (!hasHelper) borderStroke else BorderStroke(1.dp, helperContainerColor ?: md_theme_errorContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding.dp),
                    content = content
                )
            }
        } else {
            OutlinedCard(
                modifier = Modifier.appTestTag(testTag),
                onClick = onClick,
                enabled = enabled,
                shape = if (!hasHelper) CardDefaults.shape else GreenSmallBottom,
                elevation = elevation,
                colors = colors,
                border = if (!hasHelper) borderStroke else BorderStroke(1.dp, helperContainerColor ?: md_theme_errorContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding.dp),
                    content = content
                )
            }
        }

        AnimatedNullableVisibility(value = helperText ?: if (helperContent != null) "" else null) { textValue ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = GreenSmallTop,
                colors = CardDefaults.cardColors(
                    contentColor = whiteHigh,
                    containerColor = helperContainerColor ?: md_theme_errorContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (helperContent != null) {
                        helperContent()
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (textValue.isNotBlank()) {
                                Icon(
                                    painter = painterResource(Res.drawable.warning_diamond),
                                    contentDescription = null,
                                    tint = red,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(text = stringResourceFromId(textValue), style = bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}