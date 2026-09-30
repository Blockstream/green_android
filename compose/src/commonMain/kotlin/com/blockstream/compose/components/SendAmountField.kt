package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@androidx.compose.runtime.Immutable
data class SendAmountFieldState(
    val field: AmountFieldState,
    val supporting: SendAmountSupportingState = SendAmountSupportingState(),
)

@Composable
fun SendAmountField(
    state: SendAmountFieldState,
    onValueChange: (String) -> Unit,
    onAction: (AmountFieldAction) -> Unit,
    onSendAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    focusRequester: FocusRequester? = null,
    style: AmountFieldStyle = LocalAmountFieldStyle.current ?: AmountFieldDefaults.style,
    feedbackContent: (@Composable AmountFieldFeedbackScope.() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountField(
            state = state.field,
            onValueChange = onValueChange,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth(),
            label = label,
            focusRequester = focusRequester,
            style = style,
            feedbackContent = feedbackContent,
        )

        SendAmountSupporting(
            state = state.supporting,
            interaction = state.field.interaction,
            onSendAllClick = onSendAllClick,
            style = style,
        )

        supportingContent?.invoke()
    }
}
