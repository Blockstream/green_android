package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.monospaceFont
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.getClipboard

@Composable
fun GreenTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    footerContent: @Composable (() -> Unit)? = null,
    onQrClick: (() -> Unit)? = null
) {
    Column {
        GreenDataLayout(title = title, withPadding = false, error = error) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .padding(vertical = 4.dp)
            ) {

                val textStyle = LocalTextStyle.current.merge(
                    TextStyle(
                        fontFamily = monospaceFont,
                        color = whiteHigh,
                        textAlign = TextAlign.Start
                    )
                ).merge(bodyLarge)

                val colors = TextFieldDefaults.colors()

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = if (singleLine) 1 else maxLines,
                    cursorBrush = SolidColor(colors.cursorColor),
                    modifier = Modifier
                        .weight(1f)
                )

                if (value.isEmpty()) {
                    val context = LocalContext.current
                    IconButton(onClick = { onQrClick?.invoke() }, enabled = enabled) {
                        Icon(
                            painter = painterResource(id = R.drawable.qr_code),
                            contentDescription = "Scan QR"
                        )
                    }

                    IconButton(
                        onClick = { onValueChange(getClipboard(context) ?: "") },
                        enabled = enabled
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.clipboard),
                            contentDescription = "Edit"
                        )
                    }
                } else {
                    IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                        Icon(
                            painter = painterResource(id = R.drawable.x_circle),
                            contentDescription = "Clear"
                        )
                    }
                }
            }
        }

        footerContent?.invoke()
    }
}

@Preview
@Composable
fun GreenTextFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenTextField(stringResource(R.string.id_address), "123", {})
            GreenTextField(stringResource(R.string.id_private_key), "", {})
            GreenTextField(
                stringResource(R.string.id_private_key),
                "",
                {},
                error = "id_insufficient_funds"
            )
        }
    }
}