package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockstream.common.utils.stringResourceFromId
import com.blockstream.compose.theme.GreenSmallBottom
import com.blockstream.compose.theme.GreenSmallTop
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_errorContainer
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.AnimatedNullableVisibility

@Composable
fun GreenCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    enabled: Boolean = true,
    padding: Int = 16,
    border: BorderStroke? = null,
    helperText: String? = null,
    helperContainerColor: Color? = null,
    contentError: (@Composable BoxScope.(error: String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        if (onClick == null) {
            Card(
                shape = if (helperText == null) CardDefaults.shape else GreenSmallBottom,
                elevation = elevation,
                colors = colors,
                border = if (helperText == null) border else BorderStroke(1.dp, helperContainerColor ?: md_theme_errorContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding.dp),
                    content = content
                )
            }
        } else {
            Card(
                onClick = onClick,
                enabled = enabled,
                shape = if (helperText == null) CardDefaults.shape else GreenSmallBottom,
                elevation = elevation,
                colors = colors,
                border = if (helperText == null) border else BorderStroke(1.dp, helperContainerColor ?: md_theme_errorContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding.dp),
                    content = content
                )
            }
        }

        AnimatedNullableVisibility(value = helperText) {
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
                        .padding(horizontal = 8.dp)
                        .padding(top = 6.dp, bottom = 8.dp),
                ) {
                    if(contentError != null){
                        contentError(it)
                    }else{
                        Text(text = stringResourceFromId(it), style = bodyMedium)
                    }
                }
            }
        }
    }
}