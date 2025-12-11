package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.empty
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_send_all
import blockstream_green.common.generated.resources.lock_simple
import blockstream_green.common.generated.resources.pencil_simple_line
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.CaretDown
import com.adamglin.phosphoricons.regular.XCircle
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    denomination: Denomination,
    secondaryValue: String? = "",
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
    onDenominationClick: (() -> Unit)? = null
) {
    val colors = TextFieldDefaults.colors()

    val textStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = whiteHigh,
            textAlign = TextAlign.Center,
            fontSize = 26.sp
        )
    )

    val formatter = remember {
        DecimalFormatter(
            decimalSeparator = DecimalFormat.DecimalSeparator.first(),
            groupingSeparator = DecimalFormat.GroupingSeparator.first()
        )
    }

    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = value, selection = TextRange(value.length)
            )
        )
    }

    // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
    // pass `TextFieldValue(text = value)` to the CoreTextField because we need to preserve the
    // composition.
    val textFieldValue = textFieldValueState.copy(
        text = value,
        selection = if (textFieldValueState.text.length != value.length) TextRange(value.length) else textFieldValueState.selection
    )

    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.text != textFieldValueState.text ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }

    Column {
        val isEditable = enabled && !isAmountLocked

        val density = LocalDensity.current
        var startRowWidth by remember { mutableStateOf(0.dp) }
        var endRowWidth by remember { mutableStateOf(0.dp) }

        GreenDataLayout(
            title = title ?: stringResource(Res.string.id_amount),
            withPadding = false,
            helperText = helperText,
            helperContainerColor = helperContainerColor
        ) {
            Box {

                GradientEdgeBox(
                    startSolidWidth = max(startRowWidth, endRowWidth),
                    endSolidWidth = max(startRowWidth, endRowWidth)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                            .padding(top = 12.dp)
                            .padding(bottom = 4.dp)
                            .padding(horizontal = max(startRowWidth, endRowWidth))
                    ) {
                        BasicTextField(
                            value = textFieldValueState,
                            onValueChange = {
                                textFieldValueState = formatter.cleanup(it).also {
                                    onValueChange(it.text)
                                }
                            },
                            enabled = isEditable,
                            readOnly = isReadyOnly,
                            textStyle = textStyle,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            cursorBrush = SolidColor(colors.cursorColor),
                            modifier = Modifier
                                .ifTrue(focusRequester != null) {
                                    it.focusRequester(focusRequester!!)
                                }
                                .appTestTag("amount")
                        )

                        Box {
                            if (secondaryValue == null) {
                                CircularProgressIndicator(
                                    strokeWidth = 1.dp,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(8.dp),
                                )
                            }

                            Text(
                                // hack to fix height adjustments
                                text = secondaryValue?.takeIf { it.isNotBlank() } ?: " ",
                                modifier = Modifier.fillMaxWidth()
                                    .appTestTag("amount_converted"),
                                maxLines = 1,
                                style = bodySmall,
                                textAlign = TextAlign.Center,
                                color = whiteMedium
                            )
                        }
                    }
                }

                // Start
                Row(
                    modifier = Modifier.align(Alignment.CenterStart)
                        .onGloballyPositioned { coordinates ->
                            // Convert pixels to dp
                            startRowWidth = with(density) {
                                coordinates.size.width.toDp()
                            }
                        }) {

                    if (!isAmountLocked && supportsSendAll) {
                        TextButton(
                            modifier = Modifier.padding(start = 4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            onClick = { onSendAllClick() }) {
                            GreenRow(padding = 0, space = 6) {
                                Icon(
                                    painter = painterResource(Res.drawable.empty),
                                    contentDescription = null,
                                    tint = if (sendAll) green else whiteMedium,
                                    modifier = Modifier.size(16.dp)
                                )

                                Text(
                                    text = stringResource(Res.string.id_send_all),
                                    style = bodySmall,
                                    color = if (sendAll) green else whiteMedium
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isAmountLocked) {
                        IconButton(onClick = { }, enabled = false) {
                            Icon(
                                painter = painterResource(Res.drawable.lock_simple),
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // End
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .onGloballyPositioned { coordinates ->
                            // Convert pixels to dp
                            endRowWidth = with(density) {
                                coordinates.size.width.toDp()
                            }
                        }
                        .ifTrue(isAmountLocked || !isReadyOnly) {
                            it.padding(end = 8.dp)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (!isAmountLocked && !isReadyOnly) {
                        if (value.isNotEmpty()) {
                            IconButton(onClick = { onValueChange("") }, enabled = isEditable) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.XCircle,
                                    contentDescription = "Clear",
                                    modifier = Modifier.appTestTag("clear")
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .ifTrue(isEditable && !isReadyOnly && onDenominationClick != null) {
                                it.clickable {
                                    onDenominationClick?.invoke()
                                }
                            }
                            .appTestTag("amount_denomination")
                    ) {
                        val canBeEdited =
                            isEditable && session?.let { assetId.isPolicyAsset(session = it) } != false && onDenominationClick != null
                        Text(
                            text = session?.let {
                                denomination.assetTicker(
                                    session = it,
                                    assetId = assetId
                                )
                            } ?: denomination.denomination,
                            style = bodyMedium
                        )

                        if (canBeEdited) {
                            Icon(
                                imageVector = PhosphorIcons.Fill.CaretDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(start = 2.dp)
                            )
                        } else {
                            GreenSpacer(12)
                        }
                    }

                    if (!isAmountLocked) {
                        if (isReadyOnly) {
                            IconButton(onClick = { onEditClick() }) {
                                Icon(
                                    painter = painterResource(Res.drawable.pencil_simple_line),
                                    contentDescription = "Edit"
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
