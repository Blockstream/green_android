package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_square_out
import blockstream_green.common.generated.resources.arrows_counter_clockwise
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.green_shield
import blockstream_green.common.generated.resources.id_about
import blockstream_green.common.generated.resources.id_app_settings
import blockstream_green.common.generated.resources.id_biometrics
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.id_increase_qr_size
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_paste
import blockstream_green.common.generated.resources.id_scan_qr_code
import blockstream_green.common.generated.resources.magnifying_glass_plus
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.question
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Spinner
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.textHigh
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.Rotating
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
private fun GreenButtonText(text: String, textStyle: TextStyle) {
    Text(text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun GreenButton(
    text: String,
    modifier: Modifier = Modifier,
    type: GreenButtonType = GreenButtonType.COLOR,
    color: GreenButtonColor = GreenButtonColor.GREEN,
    size: GreenButtonSize = GreenButtonSize.NORMAL,
    icon: Painter? = null,
    enabled: Boolean = true,
    onProgress: Boolean = false,
    onClick: () -> Unit,
) {

    val sizedModifier = when (size) {
        GreenButtonSize.SMALL -> {
            Modifier
                .then(modifier)
                .height(30.dp)

        }

        GreenButtonSize.TINY -> {
            Modifier
                .then(modifier)
                .height(20.dp)

        }

        GreenButtonSize.BIG -> {
            Modifier
                .then(modifier)
                .height(50.dp)
        }

        else -> {
            modifier
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

                if (icon != null || onProgress) {
                    Box(modifier = Modifier.padding(end = 6.dp)) {
                        Rotating(
                            duration = 2000,
                            enabled = onProgress,
                        ) {
                            if(onProgress) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Spinner,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                )
                            } else {
                                Icon(
                                    painter = icon.takeIf { !onProgress }
                                        ?: painterResource(Res.drawable.arrows_counter_clockwise),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                )
                            }
                        }
                    }
                }

                GreenButtonText(text = text, textStyle = textStyle)
            }
        }

        GreenButtonType.OUTLINE -> {
            val buttonColors = if (enabled && color == GreenButtonColor.RED) {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else if (enabled && color == GreenButtonColor.WHITE) {
                ButtonDefaults.outlinedButtonColors(contentColor = textHigh)
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
                    color = textHigh,
                )
            } else if (enabled && color == GreenButtonColor.GREENER) {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                ButtonDefaults.outlinedButtonBorder(enabled = enabled)
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
                icon?.also {
                    Icon(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(18.dp)
                    )
                }
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
                icon?.also {
                    Icon(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(18.dp)
                    )
                }
                GreenButtonText(text = text, textStyle = textStyle)
            }
        }
    }
}

@Composable
fun GreenIconButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    color: Color = green,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    onClick: () -> Unit
) {
    TextButton(modifier = Modifier.then(modifier), contentPadding = contentPadding, onClick = onClick) {
        GreenRow(padding = 0, space = 6) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )

            Text(text = text, style = labelMedium, color = color)
        }
    }
}

@Composable
fun HelpButton(onClick: () -> Unit) {
    GreenIconButton(
        text = stringResource(Res.string.id_help),
        icon = painterResource(Res.drawable.question),
        color = whiteMedium,
        onClick = onClick
    )
}

@Composable
fun PasteButton(onClick: () -> Unit) {
    GreenIconButton(
        text = stringResource(Res.string.id_paste),
        icon = painterResource(Res.drawable.clipboard),
        onClick = onClick
    )
}

@Composable
fun ScanQrButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    GreenIconButton(
        modifier = modifier,
        text = stringResource(Res.string.id_scan_qr_code),
        icon = painterResource(Res.drawable.qr_code),
        onClick = onClick
    )
}

@Composable
fun ZoomButton(onClick: () -> Unit) {
    GreenIconButton(
        text = stringResource(Res.string.id_increase_qr_size),
        icon = painterResource(Res.drawable.magnifying_glass_plus),
        color = textMedium,
        onClick = onClick
    )
}

@Composable
fun LearnMoreButton(
    color: Color = green,
    onClick: () -> Unit
) {
    GreenIconButton(
        text = stringResource(Res.string.id_learn_more),
        icon = painterResource(Res.drawable.arrow_square_out),
        color = color,
        onClick = onClick
    )
}

@Composable
fun AboutButton(onClick: () -> Unit) {
    GreenIconButton(
        text = stringResource(Res.string.id_about),
        icon = painterResource(Res.drawable.green_shield),
        color = whiteMedium,
        onClick = onClick
    )
}

@Composable
fun BiometricsButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(Res.string.id_biometrics),
            style = labelMedium,
            color = whiteMedium
        )
    }
}

@Composable
fun RichWatchOnlyButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = "RWO",
            style = labelMedium,
            color = whiteMedium
        )
    }
}

@Composable
fun AppSettingsButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = stringResource(Res.string.id_app_settings),
            style = labelMedium,
            color = whiteMedium
        )
    }
}
