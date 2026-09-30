package com.blockstream.compose.screens.lightning

import androidx.compose.runtime.Composable
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.AmountFieldSecondaryValue
import com.blockstream.compose.components.AmountFieldState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(name = "LNURL amount wrapper")
@Composable
internal fun LnUrlWithdrawAmountFieldPreview() {
    GreenPreview {
        LnUrlWithdrawAmountField(
            state = AmountFieldState(
                value = "1200",
                denominationLabel = "sats",
                secondaryValue = AmountFieldSecondaryValue.Value("~ 0.42 EUR"),
                canSelectDenomination = true,
            ),
            withdrawalLimits = "Minimum 1,000 sats, maximum 50,000 sats",
            onValueChange = {},
            onAction = {},
            label = "Amount to receive",
        )
    }
}
