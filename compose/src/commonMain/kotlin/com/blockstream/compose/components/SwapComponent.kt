package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_available
import blockstream_green.common.generated.resources.id_from
import blockstream_green.common.generated.resources.id_to
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.extensions.previewAccount
import com.blockstream.compose.extensions.previewAccountAssetBalance
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.textLow
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.ifTrue
import com.blockstream.compose.utils.invisible
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.utils.DecimalFormat
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwapComponent(
    from: AccountAssetBalance,
    to: AccountAssetBalance,
    fromAccounts: List<AccountAssetBalance>,
    toAccounts: List<AccountAssetBalance>,
    amountFrom: String,
    amountFromFiat: String,
    amountTo: String,
    amountToFiat: String,
    denomination: Denomination? = null,
    error: String? = null,
    focusRequester: FocusRequester? = null,
    session: GdkSession? = null,
    onAmountChanged: (String, Boolean) -> Unit,
    onFromAccountClick: () -> Unit,
    onFromAssetClick: () -> Unit,
    onToAccountClick: () -> Unit,
    onToAssetClick: () -> Unit,
    onTogglePairsClick: () -> Unit,
    onDenominationClick: () -> Unit
) {
    var isFromFocused by remember { mutableStateOf(false) }
    var isToFocused by remember { mutableStateOf(false) }

    val upstreamError = error.takeIf { amountFrom.isNotBlank() }

    var error by remember { mutableStateOf<String?>(null) }

    // Debounce error display: show after 1 second delay, hide immediately
    LaunchedEffect(amountFrom, upstreamError, isFromFocused, isToFocused) {
        if (error == null && upstreamError != null && (isFromFocused || isToFocused)) {
            delay(600)
        }
        error = upstreamError
    }

    GreenCard(
        padding = 0,
        helperText = error,
        colors = CardDefaults.outlinedCardColors(containerColor = if (error == null) Color.Transparent else Color.Unspecified),
        border = if (error == null) BorderStroke(1.dp, Color.Transparent) else null
    ) {
        Box(modifier = Modifier.ifTrue(error != null) {
            it.background(md_theme_onError)
        }) {
            GreenColumn(
                modifier = Modifier.fillMaxWidth(), space = 16, padding = 0,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                var isFromEntry by remember { mutableStateOf(true) }

                SwapCard(
                    label = stringResource(Res.string.id_from) + ":",
                    accountAssetBalance = from,
                    value = amountFrom,
                    denomination = denomination,
                    amountFiat = amountFromFiat,
                    showAccountSelector = fromAccounts.size > 1,
                    focusRequester = focusRequester,
                    session = session,
                    onValueChange = {
                        if (isFromEntry) {
                            onAmountChanged(it, true)
                        }
                    },
                    onFocusChanged = {
                        if (it.isFocused) {
                            isFromEntry = true
                        }
                        isFromFocused = it.isFocused
                    },
                    onAccountClick = onFromAccountClick,
                    onAssetClick = onFromAssetClick,
                    onDenominationClick = onDenominationClick
                )

                SwapCard(
                    label = stringResource(Res.string.id_to) + ":",
                    accountAssetBalance = to,
                    value = amountTo,
                    denomination = denomination,
                    amountFiat = amountToFiat,
                    showAccountSelector = toAccounts.size > 1,
                    session = session,
                    onValueChange = {
                        if (!isFromEntry) {
                            onAmountChanged(it, false)
                        }
                    },
                    onFocusChanged = {
                        if (it.isFocused) {
                            isFromEntry = false
                        }
                        isToFocused = it.isFocused
                    },
                    onAccountClick = onToAccountClick,
                    onAssetClick = onToAssetClick,
                    onDenominationClick = onDenominationClick
                )
            }
        }

        OutlinedCard(modifier = Modifier.align(Alignment.Center), onClick = onTogglePairsClick) {
            Icon(
                PhosphorIcons.Regular.ArrowsDownUp,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun SwapCard(
    label: String,
    accountAssetBalance: AccountAssetBalance,
    session: GdkSession? = null,
    value: String,
    denomination: Denomination? = null,
    amountFiat: String,
    showAccountSelector: Boolean = true,
    focusRequester: FocusRequester? = null,
    onValueChange: (String) -> Unit,
    onAccountClick: () -> Unit,
    onAssetClick: () -> Unit,
    onFocusChanged: (FocusState) -> Unit,
    onDenominationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TextFieldDefaults.colors()

    val textStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = whiteHigh,
            textAlign = TextAlign.End,
            fontSize = 22.sp
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

    GreenCard(padding = 0) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = bodyMedium,
                    color = textLow
                )

                TextButton(onClick = onAccountClick, content = {
                    Text(accountAssetBalance.account.name, style = bodyMedium, color = green)
                }, modifier = Modifier.invisible(!showAccountSelector), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
            }

            // Currency and amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Currency selector
                GreenRow(
                    modifier = Modifier
                        .noRippleClickable(onClick = onAssetClick),
                    verticalAlignment = Alignment.CenterVertically,
                    padding = 0, space = 4
                ) {
                    Box {
                        Image(
                            painter = (accountAssetBalance.asset.assetId).assetIcon(
                                session = session,
                                isLightning = accountAssetBalance.account.isLightning
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )

                        Image(
                            painter = painterResource(accountAssetBalance.account.policyIcon()),
                            contentDescription = "Policy",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = 10.dp)
                                .size(18.dp)
                        )

                    }

                    Text(
                        text = accountAssetBalance.asset.name(session),
                        style = bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = whiteHigh,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }

                GradientEdgeBox(
                    startSolidWidth = 16.dp,
                    endSolidWidth = 0.dp
                ) {

                    // Amount
                    GreenRow(
                        padding = 0, space = 8,
                    ) {

                        BasicTextField(
                            value = textFieldValueState,
                            onValueChange = {
                                textFieldValueState = formatter.cleanup(it).also {
                                    onValueChange(it.text)
                                }
                            },
                            textStyle = textStyle,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            cursorBrush = SolidColor(colors.cursorColor),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged(onFocusChanged)
                                .ifTrue(focusRequester != null) {
                                    it.focusRequester(focusRequester!!)
                                }
                                .appTestTag("amount")
                        )

                        Text(
                            text = session?.let {
                                denomination?.assetTicker(
                                    session = it,
                                    assetId = accountAssetBalance.assetId
                                )
                            } ?: denomination?.denomination ?: accountAssetBalance.asset.ticker ?: accountAssetBalance.assetId,
                            style = textStyle,
                            modifier = Modifier.clickable {
                                onDenominationClick()
                            },
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Available and fiat value row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = stringResource(Res.string.id_available) + ": ${accountAssetBalance.balance}",
                    style = bodyMedium,
                    color = whiteLow
                )

                Text(
                    text = amountFiat,
                    style = bodySmall,
                    color = textLow
                )
            }
        }
    }
}

@Composable
@Preview
fun SwapScreenPreview() {
    GreenChromePreview {
        GreenColumn {
            SwapComponent(
                from = previewAccountAssetBalance(),
                to = previewAccountAssetBalance(previewAccount(true)),
                fromAccounts = listOf(previewAccountAssetBalance()),
                toAccounts = emptyList(),
                amountFrom = "12345678901234567890",
                amountFromFiat = "123 USD",
                amountTo = "12345678901234567890 L-BTC",
                amountToFiat = "123 USD",
                onAmountChanged = { _, _ ->

                },
                onFromAccountClick = {},
                onFromAssetClick = {},
                onToAccountClick = {},
                onToAssetClick = {},
                onTogglePairsClick = {},
                onDenominationClick = {}
            )

            SwapComponent(
                from = previewAccountAssetBalance(),
                to = previewAccountAssetBalance(previewAccount(true)),
                fromAccounts = emptyList(),
                toAccounts = emptyList(),
                amountFrom = "12345678901234567890",
                amountFromFiat = "123 USD",
                amountTo = "12345678901234567890 L-BTC",
                amountToFiat = "123 USD",
                error = "Error",
                onAmountChanged = { _, _ ->

                },
                onFromAccountClick = {},
                onFromAssetClick = {},
                onToAccountClick = {},
                onToAssetClick = {},
                onTogglePairsClick = {},
                onDenominationClick = {}
            )
        }
    }
}
