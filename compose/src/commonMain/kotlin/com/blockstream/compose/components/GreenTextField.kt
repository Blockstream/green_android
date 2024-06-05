package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.x_circle
import com.blockstream.common.extensions.isBlank
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource

@Composable
fun GreenTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    placeholder: String? = null,
    footerContent: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onQrClick: (() -> Unit)? = null
) {
    val platformManager = LocalPlatformManager.current
    Column(modifier = modifier) {
        GreenDataLayout(title = title, withPadding = false, error = error) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .padding(vertical = 4.dp)
            ) {

                val textStyle = LocalTextStyle.current.merge(
                    TextStyle(
                        fontFamily = MonospaceFont(),
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
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = {
                        if(placeholder != null && value.isBlank()){
                            Text(placeholder, color = whiteMedium)
                        }
                        it()
                    },
                    modifier = Modifier
                        .ifTrue(minLines > 1) {
                            padding(vertical = 8.dp)
                        }
                        .weight(1f)
                )

                if (value.isEmpty()) {
                    if(onQrClick != null){
                        IconButton(onClick = { onQrClick.invoke() }, enabled = enabled) {
                            Icon(
                                painter = painterResource(Res.drawable.qr_code),
                                contentDescription = "Scan QR"
                            )
                        }
                    }

                    IconButton(
                        onClick = { onValueChange(platformManager.getClipboard() ?: "") },
                        enabled = enabled
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.clipboard),
                            contentDescription = "Edit"
                        )
                    }
                } else {
                    IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                        Icon(
                            painter = painterResource(Res.drawable.x_circle),
                            contentDescription = "Clear"
                        )
                    }
                }
            }
        }

        footerContent?.invoke()
    }
}