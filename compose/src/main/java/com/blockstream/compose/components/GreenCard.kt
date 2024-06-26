package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.GreenSmallBottom
import com.blockstream.compose.theme.GreenSmallTop
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_errorContainer
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.stringResourceId

@Composable
fun GreenCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    enabled: Boolean = true,
    padding: Int = 16,
    border: BorderStroke? = null,
    error: String? = null,
    errorColor: Color? = null,
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
                shape = if (error == null) CardDefaults.shape else GreenSmallBottom,
                elevation = elevation,
                colors = colors,
                border = if (error == null) border else BorderStroke(1.dp, errorColor ?: md_theme_errorContainer)
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
                shape = if (error == null) CardDefaults.shape else GreenSmallBottom,
                elevation = elevation,
                colors = colors,
                border = if (error == null) border else BorderStroke(1.dp, errorColor ?: md_theme_errorContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding.dp),
                    content = content
                )
            }
        }

        AnimatedNullableVisibility(value = error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = GreenSmallTop,
                colors = CardDefaults.cardColors(
                    contentColor = whiteHigh,
                    containerColor = errorColor ?: md_theme_errorContainer
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
                        Text(text = stringResourceId(it), style = bodyMedium)
                    }
                }
            }
        }

    }
}

@Composable
@Preview()
fun GreenCardPreview() {
    GreenThemePreview {
        GreenColumn(
            Modifier
                .padding(24.dp)
                .padding(24.dp)
        ) {

            GreenCard(padding = 0) {
                Text(text = "No Padding", modifier = Modifier.fillMaxWidth())
            }

            GreenCard {
                Text(text = "Simple Green Card")
            }

            GreenCard(onClick = {

            }) {
                Text(text = "OnClick Green Card")
            }

            GreenCard(onClick = {

            }, enabled = false) {
                Text(text = "OnClick Green Card (disabled)")
            }

            GreenCard {
                Text(
                    text = "This is a GreenCard", modifier = Modifier.align(Alignment.Center)
                )
            }

            var error by remember {
                mutableStateOf<String?>("Error")
            }

            GreenCard(error = error, modifier = Modifier.clickable {
                if (error == null) {
                    error = "This is an error"
                } else {
                    error = null
                }
            }) {
                Text(
                    text = "This is a GreenCard", modifier = Modifier.align(Alignment.Center)
                )
            }

            GreenCard(error = error, contentError = {
                Text(it)
                GreenButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    text = "Clear Error",
                    type = GreenButtonType.TEXT,
                    color = GreenButtonColor.WHITE,
                    size = GreenButtonSize.TINY
                ) {
                    if (error == null) {
                        error = "This is an error"
                    } else {
                        error = null
                    }
                }
            }) {
                Text(
                    text = "This is a GreenCard", modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}