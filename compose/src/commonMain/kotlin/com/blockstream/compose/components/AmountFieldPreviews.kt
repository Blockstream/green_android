package com.blockstream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.check_circle
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.StringHolder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun previewState(
    value: String = "0.015",
    interaction: AmountFieldInteraction = AmountFieldInteraction.Editable,
    secondaryValue: AmountFieldSecondaryValue = AmountFieldSecondaryValue.Value("~ 1,234 EUR"),
    feedback: AmountFieldFeedback? = null,
) = AmountFieldState(
    value = value,
    denominationLabel = "BTC",
    secondaryValue = secondaryValue,
    interaction = interaction,
    canSelectDenomination = true,
    feedback = feedback,
)

@Preview(name = "Core states")
@Composable
fun AmountFieldCoreStatesPreview() {
    GreenPreview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AmountField(
                state = previewState(),
                onValueChange = {},
                onAction = {},
                label = "Amount",
            )
            AmountField(
                state = previewState(
                    value = "",
                    secondaryValue = AmountFieldSecondaryValue.Loading,
                ),
                onValueChange = {},
                onAction = {},
                label = "Amount",
            )
            AmountField(
                state = previewState(interaction = AmountFieldInteraction.Locked),
                onValueChange = {},
                onAction = {},
                label = "Amount",
            )
            AmountField(
                state = previewState(interaction = AmountFieldInteraction.ReadOnly),
                onValueChange = {},
                onAction = {},
                label = "Amount",
            )
        }
    }
}

@Preview(name = "Interactive clear")
@Composable
fun AmountFieldInteractiveClearPreview() {
    GreenPreview {
        var amount by remember { mutableStateOf("0.015") }

        AmountField(
            state = previewState(value = amount),
            onValueChange = { amount = it },
            onAction = { action ->
                when (action) {
                    AmountFieldAction.Clear -> amount = ""
                    AmountFieldAction.Edit,
                    AmountFieldAction.SelectDenomination -> Unit
                }
            },
            label = "Amount",
        )
    }
}

@Preview(name = "Feedback severities")
@Composable
fun AmountFieldFeedbackSeveritiesPreview() {
    GreenPreview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AmountFieldFeedbackSeverity.entries.forEach { severity ->
                AmountField(
                    state = previewState(
                        feedback = AmountFieldFeedback(
                            severity = severity,
                            text = StringHolder.create("${severity.name} feedback"),
                        ),
                    ),
                    onValueChange = {},
                    onAction = {},
                    label = "Amount",
                )
            }
        }
    }
}

@Preview(name = "Custom style and feedback content")
@Composable
fun AmountFieldCustomStylePreview() {
    GreenPreview {
        val baseStyle = AmountFieldDefaults.style
        val customStyle = baseStyle.copy(
            colors = baseStyle.colors.copy(
                feedback = baseStyle.colors.feedback.copy(
                    warning = baseStyle.colors.feedback.warning.copy(
                        icon = Res.drawable.check_circle,
                    ),
                ),
            ),
        )

        AmountField(
            state = previewState(
                feedback = AmountFieldFeedback(severity = AmountFieldFeedbackSeverity.Warning),
            ),
            onValueChange = {},
            onAction = {},
            label = "Amount",
            style = customStyle,
            feedbackContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(style.icon),
                        contentDescription = null,
                        tint = style.iconTint,
                    )
                    Text(
                        text = "Custom feedback content",
                        style = bodyMedium,
                        color = style.contentColor,
                    )
                }
            },
        )
    }
}

@Preview(name = "Send composite")
@Composable
fun SendAmountFieldPreview() {
    GreenPreview {
        SendAmountField(
            state = SendAmountFieldState(
                field = previewState(),
                supporting = SendAmountSupportingState(
                    availableBalance = "0.025 BTC",
                    isMaxPayable = true,
                    showSendAll = true,
                ),
            ),
            onValueChange = {},
            onAction = {},
            onSendAllClick = {},
            label = "Amount",
            supportingContent = {
                Text(
                    text = "3 coins selected",
                    style = bodyMedium,
                    color = whiteLow,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            },
        )
    }
}
