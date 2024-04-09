package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
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
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.monospaceFont
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.getClipboard
import com.blockstream.compose.utils.ifTrue

@Composable
fun GreenAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    denomination: Denomination,
    assetId: String? = null,
    session: GdkSession? = null,
    sendAll: Boolean? = null,
    enabled: Boolean = true,
    isAmountLocked: Boolean = false,
    error: String? = null,
    footerContent: @Composable (() -> Unit)? = null,
    onSendAllClick: () -> Unit = {},
    onDenominationClick: () -> Unit = {}
) {
    Column {
        val isEditable = enabled && !isAmountLocked
        // var errorColor by remember { mutableStateOf<Color?>(md_theme_outline) }
        val errorColor by remember { mutableStateOf<Color?>(null) }

        GreenDataLayout(
            title = stringResource(id = R.string.id_amount),
            withPadding = false,
            error = error,
            errorColor = errorColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = if (isAmountLocked || sendAll != null) 0.dp else 16.dp)
                    .padding(vertical = 8.dp)
            ) {
                val colors = TextFieldDefaults.colors()

                val textStyle = LocalTextStyle.current.merge(
                    TextStyle(
                        fontFamily = monospaceFont,
                        color = whiteHigh,
                        textAlign = TextAlign.End
                    )
                ).merge(titleSmall)

                val formatter = remember {
                    DecimalFormatter(
                        decimalSeparator = DecimalFormat.DecimalSeparator.first(),
                        groupingSeparator = DecimalFormat.GroupingSeparator.first()
                    )
                }

                if (!isAmountLocked && sendAll != null) {
                    GreenIconButton(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(R.string.id_send_all),
                        icon = painterResource(id = R.drawable.empty),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        color = if(sendAll == true) green else whiteMedium
                    ) {
                        onSendAllClick()
                    }
                }

                AnimatedVisibility(visible = isAmountLocked) {
                    IconButton(onClick = { }, enabled = false) {
                        Icon(
                            painter = painterResource(id = R.drawable.lock_simple),
                            contentDescription = null,
                            tint = green
                        )
                    }
                }

                BasicTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(formatter.cleanup(it))
                    },
                    enabled = isEditable,
                    textStyle = textStyle,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal
                    ),
                    cursorBrush = SolidColor(colors.cursorColor),
                    modifier = Modifier
                        .weight(1f)
                )

                Text(
                    text = session?.let {
                        denomination.assetTicker(
                            session = it,
                            assetId = assetId
                        )
                    } ?: denomination.denomination,
                    style = if (isEditable && session?.let { assetId.isPolicyAsset(session = it) } != false) labelLarge.merge(
                        TextStyle(textDecoration = TextDecoration.Underline)
                    ) else labelLarge,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 6.dp)
                        .ifTrue(!isAmountLocked) {
                            clickable {
                                onDenominationClick()
                            }
                        }
                )

                if (value.isEmpty()) {
                    val context = LocalContext.current
                    IconButton(
                        onClick = { onValueChange(getClipboard(context) ?: "") },
                        enabled = !isAmountLocked
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.clipboard),
                            contentDescription = "Edit"
                        )
                    }
                } else {
                    IconButton(onClick = { onValueChange("") }, enabled = !isAmountLocked) {
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
fun GreenAmountFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            var amount by remember {
                mutableStateOf("123")
            }
            GreenAmountField(amount, {
                amount = it
            }, footerContent = {
                Text(
                    text = "~ 1.131.00 EUR",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = bodyMedium,
                    color = whiteLow
                )
            }, denomination = Denomination.BTC)

            GreenAmountField(amount, {
                amount = it
            }, footerContent = {
                Text(
                    text = "~ 1.131.00 EUR",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = bodyMedium,
                    color = whiteLow
                )
            }, error = "id_invalid_amount", denomination = Denomination.SATOSHI)

            GreenAmountField(amount, {
                amount = it
            }, isAmountLocked = true, denomination = Denomination.MBTC)

            var isSendAll  by remember {
                mutableStateOf(false)
            }
            GreenAmountField(amount, {
                amount = it
            }, sendAll = isSendAll, onSendAllClick = {
                isSendAll = !isSendAll
            }, denomination = Denomination.MBTC)
        }
    }
}