package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
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
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.getClipboard

@Composable
fun GreenTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onQrClick : (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        GreenDataLayout(title = title, withPadding = false, error = error) {
            GreenRow(
                padding = 0, space = 10,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .padding(vertical = 16.dp)
            ) {

                val textStyle = LocalTextStyle.current.merge(
                    TextStyle(
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
                    cursorBrush = SolidColor(colors.cursorColor),
                    modifier = Modifier
                        .weight(1f)
                )

                if (value.isEmpty()) {
                    val context = LocalContext.current
                    Image(
                        painter = painterResource(id = R.drawable.clipboard_text),
                        contentDescription = "Edit",
                        modifier = Modifier.clickable {
                            onValueChange(getClipboard(context) ?: "")
                        }
                    )

                    Image(
                        painter = painterResource(id = R.drawable.qr_code),
                        contentDescription = "QR",
                        modifier = Modifier.clickable {
                            onQrClick?.invoke()
                        }
                    )
                } else {
                    Image(painter = painterResource(id = R.drawable.x_circle),
                        contentDescription = "Clear",
                        modifier = Modifier
                            .clickable {
                                onValueChange("")
                            }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun GreenTextFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenTextField(stringResource(R.string.id_address), "123", {})
            GreenTextField(stringResource(R.string.id_private_key), "", {})
            GreenTextField(stringResource(R.string.id_private_key), "", {}, error = "id_insufficient_funds")
        }
    }
}