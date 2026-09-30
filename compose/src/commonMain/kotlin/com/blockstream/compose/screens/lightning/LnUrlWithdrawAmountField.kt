package com.blockstream.compose.screens.lightning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockstream.compose.components.AmountField
import com.blockstream.compose.components.AmountFieldAction
import com.blockstream.compose.components.AmountFieldState
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteLow

@Composable
internal fun LnUrlWithdrawAmountField(
    state: AmountFieldState,
    withdrawalLimits: String,
    onValueChange: (String) -> Unit,
    onAction: (AmountFieldAction) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    focusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountField(
            state = state,
            onValueChange = onValueChange,
            onAction = onAction,
            label = label,
            focusRequester = focusRequester,
        )

        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = withdrawalLimits,
                textAlign = TextAlign.Start,
                style = bodyMedium,
                color = whiteLow,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
