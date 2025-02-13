package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.empty
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_send_all
import blockstream_green.common.generated.resources.lock_simple
import blockstream_green.common.generated.resources.pencil_simple_line
import blockstream_green.common.generated.resources.x_circle
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    denomination: Denomination,
    title: String? = null,
    assetId: String? = null,
    session: GdkSession? = null,
    sendAll: Boolean = false,
    supportsSendAll: Boolean = false,
    enabled: Boolean = true,
    isAmountLocked: Boolean = false,
    helperText: String? = null,
    helperContainerColor: Color? = null,
    focusRequester: FocusRequester? = null,
    footerContent: @Composable (() -> Unit)? = null,
    isReadyOnly: Boolean = false,
    onEditClick: () -> Unit = {},
    onSendAllClick: () -> Unit = {},
    onDenominationClick: () -> Unit = {}
) {
    val platformManager = LocalPlatformManager.current

    Column {
        val isEditable = enabled && !isAmountLocked

        GreenDataLayout(
            title = title ?: stringResource(Res.string.id_amount),
            withPadding = false,
            helperText = helperText,
            helperContainerColor = helperContainerColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = if (isAmountLocked || supportsSendAll) 0.dp else 16.dp)
                    .padding(vertical = 8.dp)
                    .ifTrue(isAmountLocked){
                        it.padding(end = 4.dp)
                    }
            ) {
                val colors = TextFieldDefaults.colors()

                val textStyle = LocalTextStyle.current.merge(
                    TextStyle(
                        fontFamily = MonospaceFont(),
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

                if (!isAmountLocked && supportsSendAll) {
                    GreenIconButton(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(Res.string.id_send_all),
                        icon = painterResource(Res.drawable.empty),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        color = if(sendAll) green else whiteMedium
                    ) {
                        onSendAllClick()
                    }
                }

                AnimatedVisibility(visible = isAmountLocked) {
                    IconButton(onClick = { }, enabled = false) {
                        Icon(
                            painter = painterResource(Res.drawable.lock_simple),
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
                    readOnly = isReadyOnly,
                    textStyle = textStyle,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal
                    ),
                    cursorBrush = SolidColor(colors.cursorColor),
                    modifier = Modifier
                        .weight(1f).ifTrue(focusRequester != null) {
                            it.focusRequester(focusRequester!!)
                        }
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
                        .ifTrue(isEditable && !isReadyOnly) {
                            it.clickable {
                                onDenominationClick()
                            }
                        }
                )

               if (!isAmountLocked) {
                   if (isReadyOnly) {
                       IconButton(onClick = { onEditClick() }) {
                           Icon(
                               painter = painterResource(Res.drawable.pencil_simple_line),
                               contentDescription = "Edit"
                           )
                       }
                   } else {
                       if (value.isEmpty()) {
                           IconButton(
                               onClick = { onValueChange(platformManager.getClipboard() ?: "") },
                               enabled = isEditable
                           ) {
                               Icon(
                                   painter = painterResource(Res.drawable.clipboard),
                                   contentDescription = "Edit"
                               )
                           }
                       } else {
                           IconButton(onClick = { onValueChange("") }, enabled = isEditable) {
                               Icon(
                                   painter = painterResource(Res.drawable.x_circle),
                                   contentDescription = "Clear"
                               )
                           }
                       }
                   }
               }
            }
        }

        footerContent?.invoke()
    }
}
