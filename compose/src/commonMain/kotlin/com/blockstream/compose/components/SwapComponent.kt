package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_available
import blockstream_green.common.generated.resources.id_from
import blockstream_green.common.generated.resources.id_to
import blockstream_green.common.generated.resources.warning_diamond
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewAccountAssetBalance
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_error
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.orangeSurface
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.textLow
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.ifTrue
import com.blockstream.compose.utils.stringResourceFromId
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.swap.SwapErrorSide
import com.blockstream.data.utils.DecimalFormat
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwapComponent(
    from: AccountAsset,
    fromBalance: String? = null,
    to: AccountAsset,
    toBalance: String? = null,
    fromAccounts: List<AccountAssetBalance>,
    toAccounts: List<AccountAssetBalance>,
    amountFrom: String,
    amountFromFiat: String,
    amountTo: String,
    amountToFiat: String,
    denomination: Denomination? = null,
    error: String? = null,
    errorSide: SwapErrorSide = SwapErrorSide.NONE,
    isPairSupported: Boolean = true,
    focusRequester: FocusRequester? = null,
    session: GdkSession? = null,
    onAmountChanged: (String, Boolean) -> Unit,
    onFromAccountClick: () -> Unit,
    onFromAssetClick: () -> Unit,
    onToAccountClick: () -> Unit,
    onToAssetClick: () -> Unit,
    onTogglePairsClick: () -> Unit,
    onDenominationClick: (Boolean) -> Unit
) {
    var isFromFocused by remember { mutableStateOf(false) }
    var isToFocused by remember { mutableStateOf(false) }

    // Amount errors (min/max/insufficient) only make sense once an amount is entered, but an
    // unsupported-pair error is a property of the selected pair, so surface it immediately.
    val upstreamError = error.takeIf { amountFrom.isNotBlank() || !isPairSupported }

    var error by remember { mutableStateOf<String?>(null) }

    // Debounce error display: show after 1 second delay, hide immediately
    LaunchedEffect(amountFrom, upstreamError, isFromFocused, isToFocused) {
        if (error == null && upstreamError != null && (isFromFocused || isToFocused)) {
            delay(600)
        }
        error = upstreamError
    }

    // An unsupported pair is a warning (orange), not a hard error - amount errors stay red.
    val warning = error?.takeIf { !isPairSupported }

    GreenCard(
        padding = 0,
        helperText = error,
        helperContainerColor = if (warning != null) orangeSurface else null,
        helperContent = warning?.let { warningText ->
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.warning_diamond),
                        contentDescription = null,
                        tint = orange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = stringResourceFromId(warningText), style = bodyMedium)
                }
            }
        },
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, if (error != null) md_theme_error else Color.Transparent)
    ) {
        Box {
            GreenColumn(
                modifier = Modifier.fillMaxWidth(), space = 16, padding = 0,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                var isFromEntry by remember { mutableStateOf(true) }

                SwapCard(
                    label = stringResource(Res.string.id_from) + ":",
                    accountAsset = from,
                    balance = fromBalance,
                    value = amountFrom,
                    denomination = denomination,
                    amountFiat = amountFromFiat,
                    showAccountSelector = fromAccounts.size > 1,
                    isAmountError = error != null && errorSide == SwapErrorSide.FROM,
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
                    onDenominationClick = {
                        onDenominationClick(true)
                    }
                )

                SwapCard(
                    label = stringResource(Res.string.id_to) + ":",
                    accountAsset = to,
                    balance = toBalance,
                    value = amountTo,
                    denomination = denomination,
                    amountFiat = amountToFiat,
                    showAccountSelector = toAccounts.size > 1,
                    isAmountError = error != null && errorSide == SwapErrorSide.TO,
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
                    onDenominationClick = {
                        onDenominationClick(false)
                    }
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .align(Alignment.Center)
                .appTestTag("swap_invert_button"),
            onClick = onTogglePairsClick
        ) {
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
    accountAsset: AccountAsset,
    balance: String? = null,
    session: GdkSession? = null,
    value: String,
    denomination: Denomination? = null,
    amountFiat: String,
    showAccountSelector: Boolean = true,
    isAmountError: Boolean = false,
    focusRequester: FocusRequester? = null,
    onValueChange: (String) -> Unit,
    onAccountClick: () -> Unit,
    onAssetClick: () -> Unit,
    onFocusChanged: (FocusState) -> Unit,
    onDenominationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TextFieldDefaults.colors()

    val maxFontSize = 22.sp
    val minFontSize = 10.sp

    val textStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = if (isAmountError) red else whiteHigh,
            textAlign = TextAlign.End,
            fontSize = maxFontSize
        )
    )

    val textMeasurer = rememberTextMeasurer()

    val formatter = remember {
        DecimalFormatter(
            decimalSeparator = DecimalFormat.DecimalSeparator.first(),
            groupingSeparator = DecimalFormat.GroupingSeparator.first()
        )
    }

    GreenCard(padding = 0) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = bodyMedium,
                    color = textLow
                )

                if (showAccountSelector) {
                    Text(
                        accountAsset.account.name,
                        style = bodyMedium,
                        color = green,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAccountClick)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
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
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAssetClick),
                    verticalAlignment = Alignment.CenterVertically,
                    padding = 0, space = 4
                ) {
                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        Image(
                            painter = (accountAsset.asset.assetId).assetIcon(
                                session = session,
                                isLightning = accountAsset.account.isLightning
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(vertical = 9.dp)
                                .padding(end = 9.dp)
                                .size(24.dp)
                        )

                        Image(
                            painter = painterResource(accountAsset.account.policyIcon()),
                            contentDescription = "Policy",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                        )
                    }

                    Text(
                        text = accountAsset.asset.name(session),
                        style = bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = whiteHigh,
                        modifier = Modifier.widthIn(max = 220.dp)
                    )

                    Icon(
                        PhosphorIcons.Regular.CaretRight,
                        contentDescription = null,
                        tint = whiteMedium,
                        modifier = Modifier.size(16.dp)
                    )
                }

                val fadeSolidWidth = 4.dp
                val fadeGradientWidth = 12.dp

                GradientEdgeBox(
                    modifier = Modifier.weight(1f),
                    startSolidWidth = fadeSolidWidth,
                    gradientWidth = fadeGradientWidth,
                    endSolidWidth = 0.dp
                ) {

                    // Amount
                    GreenRow(
                        padding = 0, space = 8,
                    ) {

                        // Shrink the amount to fit the width left of the start fade, so long values don't
                        // clip or slide under it.
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = fadeSolidWidth + fadeGradientWidth)
                        ) {
                            val amountFontSize = remember(value, constraints.maxWidth) {
                                var candidate = maxFontSize
                                while (candidate > minFontSize && value.isNotEmpty()) {
                                    val width = textMeasurer.measure(
                                        text = value,
                                        style = textStyle.copy(fontSize = candidate),
                                        maxLines = 1,
                                        softWrap = false
                                    ).size.width
                                    if (width <= constraints.maxWidth) break
                                    candidate = (candidate.value - 1f).sp
                                }
                                candidate
                            }

                            BasicTextField(
                                value = value,
                                onValueChange = {
                                    onValueChange(formatter.cleanup(it))
                                },
                                textStyle = textStyle.copy(fontSize = amountFontSize),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                cursorBrush = SolidColor(colors.cursorColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(onFocusChanged)
                                    .ifTrue(focusRequester != null) {
                                        it.focusRequester(focusRequester!!)
                                    }
                                    .appTestTag("amount")
                            )
                        }

                        val fallbackDenominationLabel = denomination?.denomination ?: accountAsset.asset.ticker ?: accountAsset.assetId
                        val denominationLabel by produceState(
                            initialValue = fallbackDenominationLabel,
                            denomination, session, accountAsset.assetId,
                        ) {
                            // `value` here would shadow the SwapCard `value: String` amount parameter, so target the receiver explicitly.
                            this.value = session?.let {
                                denomination?.assetTickerOrFallback(session = it, assetId = accountAsset.assetId)
                            } ?: fallbackDenominationLabel
                        }

                        Text(
                            text = denominationLabel,
                            style = textStyle.copy(color = green),
                            modifier = Modifier.clickable {
                                onDenominationClick()
                            }
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

                if (balance != null) {
                    Text(
                        text = stringResource(Res.string.id_available) + ": $balance",
                        style = bodyMedium,
                        color = whiteLow
                    )
                } else {
                    GreenSpacer(0)
                }

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
                from = previewAccountAsset(),
                to = previewAccountAsset(true),
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
                from = previewAccountAsset(),
                to = previewAccountAsset(true),
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
