package com.blockstream.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.blockstream.compose.theme.GreenChromePreview


@Composable
@Preview
fun GreenCardPreview() {
    GreenChromePreview {
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

            GreenCard(helperText = error, modifier = Modifier.clickable {
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

            GreenCard(helperText = error, contentError = {
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