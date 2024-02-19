package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.getClipboard

@Composable
fun GreenTextField(
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.id_amount),
            style = labelMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Card {
            GreenRow(
                padding = 0, space = 10,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .padding(vertical = 16.dp)
            ) {

                var value by remember { mutableStateOf("123") }

                val textStyle = LocalTextStyle.current.merge(
                    TextStyle(
                        color = whiteHigh,
                        textAlign = TextAlign.End
                    )
                ).merge(bodyLarge)

                val isError = false
                val colors = TextFieldDefaults.colors()

                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = textStyle,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.cursorColor),
                    modifier = Modifier
                        .weight(1f)
                )

                Text(
                    text = "BTC",
                    style = labelLarge.merge(TextStyle(textDecoration = TextDecoration.Underline))
                )

//                Image(
//                    painter = painterResource(id = R.drawable.arrows_counter_clockwise),
//                    contentDescription = "Edit",
//                )

                if (value.isEmpty()) {
                    val context = LocalContext.current
                    Image(
                        painter = painterResource(id = R.drawable.clipboard_text),
                        contentDescription = "Edit",
                        modifier = Modifier.clickable {
                            value = getClipboard(context) ?: ""
                        }
                    )
                } else {
                    Image(painter = painterResource(id = R.drawable.x_circle),
                        contentDescription = "Clear",
                        modifier = Modifier
                            .clickable {
                                value = ""
                            })
                }

                Image(
                    painter = painterResource(id = R.drawable.pencil_simple_line),
                    contentDescription = "Edit",
                    modifier = Modifier.clickable {
                        
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun GreenTextFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenTextField()
        }
    }
}