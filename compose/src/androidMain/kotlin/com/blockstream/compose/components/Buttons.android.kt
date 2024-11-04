package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.share_network
import com.blockstream.compose.theme.GreenThemePreview
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun GreenButtonPreview() {
    GreenThemePreview {
        GreenColumn(
            space = 6,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text("Specific")

            GreenRow(padding = 0) {
                ScanQrButton {

                }

                LearnMoreButton {

                }
            }

            GreenRow(padding = 0) {
                AboutButton { }
                AppSettingsButton { }
                HelpButton { }
            }

            GreenRow(padding = 0) {
                ZoomButton { }
            }

            HorizontalDivider()
            Text("Normal")
            GreenButton(
                text = "Normal Loading",
                enabled = false,
                onProgress = true,
                modifier = Modifier.fillMaxWidth()
            ) { }
            GreenRow(padding = 0) {
                GreenButton(text = "Normal Enabled", icon = painterResource(Res.drawable.share_network)) { }
                GreenButton(text = "Norma Disabled", enabled = false) { }
            }
            GreenRow(padding = 0) {
                GreenButton(text = "Big Enabled", size = GreenButtonSize.BIG) { }
                GreenButton(text = "Big Disabled", size = GreenButtonSize.BIG, enabled = false) { }
            }
            GreenRow(padding = 0) {
                GreenButton(text = "S", size = GreenButtonSize.SMALL) { }
                GreenButton(text = "Small Enabled", size = GreenButtonSize.SMALL) { }
                GreenButton(
                    text = "Small Disabled",
                    size = GreenButtonSize.SMALL,
                    enabled = false
                ) { }
            }
            GreenRow(padding = 0) {
                GreenButton(text = "T", size = GreenButtonSize.TINY) { }
                GreenButton(text = "Tiny Enabled", size = GreenButtonSize.TINY) { }
                GreenButton(
                    text = "Tiny Disabled",
                    size = GreenButtonSize.TINY,
                    enabled = false
                ) { }
            }
            GreenRow(padding = 0) {
                GreenButton(text = "Greener", color = GreenButtonColor.RED) { }
                GreenButton(text = "Red", color = GreenButtonColor.RED) { }
                GreenButton(text = "White", color = GreenButtonColor.WHITE) { }
            }


            HorizontalDivider()
            Text("Outline")
            GreenRow(padding = 0, space = 4) {
                GreenButton(text = "Normal Enabled", type = GreenButtonType.OUTLINE) { }
                GreenButton(
                    text = "Norma Disabled",
                    type = GreenButtonType.OUTLINE,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "Big Enabled",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.BIG
                ) { }
                GreenButton(
                    text = "Big Disabled",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.BIG,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "S",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.SMALL
                ) { }
                GreenButton(
                    text = "Small Enabled",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.SMALL
                ) { }
                GreenButton(
                    text = "Small Disabled",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.SMALL,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "T",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.TINY
                ) { }
                GreenButton(
                    text = "Tiny Enabled",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.TINY
                ) { }
                GreenButton(
                    text = "Tiny Disabled",
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.TINY,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "Green",
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.GREENER
                ) { }
                GreenButton(
                    text = "Green",
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.RED
                ) { }
                GreenButton(
                    text = "Green",
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE
                ) { }
            }

            HorizontalDivider()
            Text("Text")
            GreenRow(padding = 0, space = 4) {
                GreenButton(text = "Normal Enabled", type = GreenButtonType.TEXT) { }
                GreenButton(
                    text = "Norma Disabled",
                    type = GreenButtonType.TEXT,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "Big Enabled",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.BIG
                ) { }
                GreenButton(
                    text = "Big Disabled",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.BIG,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "S",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL
                ) { }
                GreenButton(
                    text = "Small Enabled",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL
                ) { }
                GreenButton(
                    text = "Small Disabled",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "T",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.TINY
                ) { }
                GreenButton(
                    text = "Tiny Enabled",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.TINY
                ) { }
                GreenButton(
                    text = "Tiny Disabled",
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.TINY,
                    enabled = false
                ) { }
            }

            GreenRow(padding = 0, space = 4) {
                GreenButton(
                    text = "Green",
                    type = GreenButtonType.TEXT,
                    color = GreenButtonColor.RED
                ) { }
                GreenButton(
                    text = "Green",
                    type = GreenButtonType.TEXT,
                    color = GreenButtonColor.WHITE
                ) { }
            }
        }
    }
}

