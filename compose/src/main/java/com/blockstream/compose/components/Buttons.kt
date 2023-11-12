package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.whiteMedium

enum class GreenButtonSize {
    NORMAL, SMALL, TINY, BIG
}

enum class GreenButtonType {
    COLOR, OUTLINE, TEXT
}

enum class GreenButtonColor {
    GREEN, GREENER, WHITE, RED
}


@Composable
private fun GreenButtonText(text: String, textStyle: TextStyle){
    Text(text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun GreenButton(
    text: String,
    modifier: Modifier = Modifier,
    type: GreenButtonType = GreenButtonType.COLOR,
    color: GreenButtonColor = GreenButtonColor.GREEN,
    size: GreenButtonSize = GreenButtonSize.NORMAL,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {

    val sizedModifier = when (size) {
        GreenButtonSize.SMALL -> {
            Modifier
                .height(30.dp)
                .then(modifier)
        }
        GreenButtonSize.TINY -> {
            Modifier
                .height(20.dp)
                .then(modifier)
        }
        GreenButtonSize.BIG -> {
            Modifier
                .height(50.dp)
                .then(modifier)
        }
        else -> {
            Modifier
                .height(40.dp)
                .then(modifier)
        }
    }

    val contentPadding = when (size) {
        GreenButtonSize.SMALL, GreenButtonSize.TINY -> {
            PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        }
        else -> {
            ButtonDefaults.ContentPadding
        }
    }

    val textStyle = when (size) {
        GreenButtonSize.SMALL -> {
            labelMedium
        }
        GreenButtonSize.TINY -> {
            labelSmall
        }
        else -> {
            labelLarge
        }
    }

    when (type) {
        GreenButtonType.COLOR -> {
            val buttonColors = if (enabled && color == GreenButtonColor.RED) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else if (enabled && color == GreenButtonColor.WHITE) {
                ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            } else {
                ButtonDefaults.buttonColors()
            }

            Button(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                colors = buttonColors,
                contentPadding = contentPadding,
                shape = MaterialTheme.shapes.small,
            ) {
                GreenButtonText(text = text, textStyle = textStyle)
            }
        }

        GreenButtonType.OUTLINE -> {
            val buttonColors = if (enabled && color == GreenButtonColor.RED) {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else if (enabled && color == GreenButtonColor.WHITE) {
                ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            } else {
                ButtonDefaults.outlinedButtonColors()
            }

            val border = if (enabled && color == GreenButtonColor.RED) {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (enabled && color == GreenButtonColor.WHITE) {
                BorderStroke(
                    width = 1.dp,
                    color = Color.White,
                )
            } else if (enabled && color == GreenButtonColor.GREENER) {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                ButtonDefaults.outlinedButtonBorder
            }

            OutlinedButton(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                colors = buttonColors,
                shape = MaterialTheme.shapes.small,
                contentPadding = contentPadding,
                border = border
            ) {
                GreenButtonText(text = text, textStyle = textStyle)
            }
        }

        GreenButtonType.TEXT -> {
            val buttonColors = if (enabled && color == GreenButtonColor.RED) {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else if (enabled && color == GreenButtonColor.WHITE) {
                ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            } else {
                ButtonDefaults.outlinedButtonColors()
            }

            TextButton(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                colors = buttonColors,
                shape = MaterialTheme.shapes.small,
                contentPadding = contentPadding,
            ) {
                GreenButtonText(text = text, textStyle = textStyle)
            }
        }
    }
}

@Composable
fun HelpButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        GreenRow(padding = 0, space = 6) {
            Icon(
                painter = painterResource(id = R.drawable.question),
                contentDescription = null,
                tint = whiteMedium,
                modifier = Modifier.size(20.dp)
            )

            Text(text = stringResource(id = R.string.id_help), style = labelMedium, color = whiteMedium)
        }
    }
}

@Composable
fun PasteButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(modifier = Modifier.then(modifier), onClick = onClick) {
        GreenRow(padding = 0, space = 6) {
            Icon(
                painter = painterResource(id = R.drawable.clipboard_text),
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(20.dp)
            )

            Text(text = stringResource(id = R.string.id_paste), style = labelMedium, color = green)
        }
    }
}

@Composable
fun ScanQrButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(modifier = Modifier.then(modifier), onClick = onClick) {
        GreenRow(padding = 0, space = 6) {
            Icon(
                painter = painterResource(id = R.drawable.qr_code),
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(20.dp)
            )

            Text(text = stringResource(id = R.string.id_scan_qr_code), style = labelMedium, color = green)
        }
    }
}

@Composable
fun LearnMoreButton(
    color: Color = green,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        GreenRow(padding = 0, space = 8) {
            Text(
                text = stringResource(id = R.string.id_learn_more),
                style = labelMedium,
                color = color
            )

            Icon(
                painter = painterResource(id = R.drawable.arrow_square_out),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AboutButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        GreenRow(padding = 0, space = 6) {
            Icon(
                painter = painterResource(id = R.drawable.green_shield),
                contentDescription = null,
                tint = whiteMedium,
                modifier = Modifier.size(20.dp)
            )

            Text(text = stringResource(id = R.string.id_about), style = labelMedium, color = whiteMedium)
        }
    }
}

@Composable
fun BiometricsButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(id = R.string.id_biometrics),
            style = labelMedium,
            color = whiteMedium
        )
    }
}

@Composable
fun AppSettingsButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = stringResource(id = R.string.id_app_settings),
            style = labelMedium,
            color = whiteMedium
        )
    }
}

@Composable
@Preview
fun GreenButtonPreview() {
    GreenTheme {
        GreenColumn(
            space = 8,
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

            HorizontalDivider()
            Text("Normal")
            GreenRow(padding = 0) {
                GreenButton(text = "Normal Enabled") { }
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

