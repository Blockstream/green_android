package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_available
import blockstream_green.common.generated.resources.id_max_payable
import blockstream_green.common.generated.resources.id_send_all
import org.jetbrains.compose.resources.stringResource

@androidx.compose.runtime.Immutable
data class SendAmountSupportingState(
    val availableBalance: String? = null,
    val isMaxPayable: Boolean = false,
    val showSendAll: Boolean = false,
)

@Composable
internal fun SendAmountSupporting(
    state: SendAmountSupportingState,
    interaction: AmountFieldInteraction,
    onSendAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AmountFieldStyle = LocalAmountFieldStyle.current ?: AmountFieldDefaults.style,
) {
    val showSendAll = state.showSendAll && interaction !is AmountFieldInteraction.Locked

    if (state.availableBalance == null && !showSendAll) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.availableBalance?.let { availableBalance ->
            val prefix = stringResource(
                if (state.isMaxPayable) Res.string.id_max_payable else Res.string.id_available,
            )

            Text(
                text = "$prefix $availableBalance",
                style = style.typography.supporting,
                color = style.colors.secondary,
            )
        }

        if (showSendAll) {
            TextButton(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                enabled = interaction.allowsFieldAction(),
                onClick = onSendAllClick,
            ) {
                Text(
                    text = stringResource(Res.string.id_send_all),
                    style = style.typography.supportingAction,
                    color = style.colors.accent,
                )
            }
        }
    }
}
