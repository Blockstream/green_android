package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import blockstream_green.common.generated.resources.caret_down
import blockstream_green.common.generated.resources.check_circle
import blockstream_green.common.generated.resources.id_clear
import blockstream_green.common.generated.resources.id_edit
import blockstream_green.common.generated.resources.info
import blockstream_green.common.generated.resources.lock_simple
import blockstream_green.common.generated.resources.pencil_simple_line
import blockstream_green.common.generated.resources.warning_diamond
import blockstream_green.common.generated.resources.x_circle
import com.blockstream.compose.theme.GreenSmallBottom
import com.blockstream.compose.theme.GreenSmallTop
import com.blockstream.compose.theme.blueSurface
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.md_theme_errorContainer
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.md_theme_surface
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.orangeSurface
import com.blockstream.compose.theme.positiveAccent
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.StringHolder
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.ifTrue
import com.blockstream.data.utils.DecimalFormat
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

sealed interface AmountFieldInteraction {
    data object Editable : AmountFieldInteraction
    data object Disabled : AmountFieldInteraction
    data object Locked : AmountFieldInteraction
    data object ReadOnly : AmountFieldInteraction
}

sealed interface AmountFieldSecondaryValue {
    data object Hidden : AmountFieldSecondaryValue
    data object Loading : AmountFieldSecondaryValue
    data class Value(val text: String) : AmountFieldSecondaryValue
}

enum class AmountFieldAction {
    Clear,
    Edit,
    SelectDenomination,
}

enum class AmountFieldFeedbackSeverity {
    Error,
    Warning,
    Info,
    Success,
}

@androidx.compose.runtime.Immutable
data class AmountFieldFeedback(
    val severity: AmountFieldFeedbackSeverity,
    val text: StringHolder? = null,
    val icon: DrawableResource? = null,
)

@androidx.compose.runtime.Immutable
data class AmountFieldState(
    val value: String,
    val denominationLabel: String,
    val secondaryValue: AmountFieldSecondaryValue = AmountFieldSecondaryValue.Hidden,
    val interaction: AmountFieldInteraction = AmountFieldInteraction.Editable,
    val canSelectDenomination: Boolean = false,
    val feedback: AmountFieldFeedback? = null,
    val maxFractionDigits: Int = Int.MAX_VALUE,
)

@androidx.compose.runtime.Immutable
data class AmountFieldFeedbackStyle(
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color,
    val iconTint: Color,
    val icon: DrawableResource,
)

@androidx.compose.runtime.Immutable
data class AmountFieldFeedbackStyles(
    val error: AmountFieldFeedbackStyle,
    val warning: AmountFieldFeedbackStyle,
    val info: AmountFieldFeedbackStyle,
    val success: AmountFieldFeedbackStyle,
) {
    operator fun get(severity: AmountFieldFeedbackSeverity): AmountFieldFeedbackStyle =
        when (severity) {
            AmountFieldFeedbackSeverity.Error -> error
            AmountFieldFeedbackSeverity.Warning -> warning
            AmountFieldFeedbackSeverity.Info -> info
            AmountFieldFeedbackSeverity.Success -> success
        }
}

@androidx.compose.runtime.Immutable
data class AmountFieldColors(
    val fieldContainer: Color,
    val fieldBorder: Color,
    val input: Color,
    val secondary: Color,
    val denomination: Color,
    val accent: Color,
    val feedback: AmountFieldFeedbackStyles,
)

@androidx.compose.runtime.Immutable
data class AmountFieldTypography(
    val amount: TextStyle,
    val secondary: TextStyle,
    val denomination: TextStyle,
    val supporting: TextStyle,
    val supportingAction: TextStyle,
    val feedback: TextStyle,
)

@androidx.compose.runtime.Immutable
data class AmountFieldIcons(
    val clear: DrawableResource,
    val edit: DrawableResource,
    val lock: DrawableResource,
    val denominationChevron: DrawableResource,
)

@androidx.compose.runtime.Immutable
data class AmountFieldStyle(
    val colors: AmountFieldColors,
    val typography: AmountFieldTypography,
    val icons: AmountFieldIcons,
)

object AmountFieldDefaults {
    val style: AmountFieldStyle
        @Composable get() = AmountFieldStyle(
            colors = AmountFieldColors(
                fieldContainer = md_theme_surface,
                fieldBorder = md_theme_outline,
                input = whiteHigh,
                secondary = whiteMedium,
                denomination = whiteHigh,
                accent = green,
                feedback = AmountFieldFeedbackStyles(
                    error = AmountFieldFeedbackStyle(
                        containerColor = md_theme_errorContainer,
                        borderColor = md_theme_errorContainer,
                        contentColor = whiteHigh,
                        iconTint = red,
                        icon = Res.drawable.warning_diamond,
                    ),
                    warning = AmountFieldFeedbackStyle(
                        containerColor = orangeSurface,
                        borderColor = orangeSurface,
                        contentColor = Color.White,
                        iconTint = orange,
                        icon = Res.drawable.warning_diamond,
                    ),
                    info = AmountFieldFeedbackStyle(
                        containerColor = blueSurface,
                        borderColor = blueSurface,
                        contentColor = Color.White,
                        iconTint = Color.White,
                        icon = Res.drawable.info,
                    ),
                    success = AmountFieldFeedbackStyle(
                        containerColor = green20,
                        borderColor = green20,
                        contentColor = Color.White,
                        iconTint = positiveAccent,
                        icon = Res.drawable.check_circle,
                    ),
                ),
            ),
            typography = AmountFieldTypography(
                amount = TextStyle(textAlign = TextAlign.Center, fontSize = 26.sp),
                secondary = bodySmall,
                denomination = bodyMedium,
                supporting = bodyMedium,
                supportingAction = titleSmall,
                feedback = bodyMedium,
            ),
            icons = AmountFieldIcons(
                clear = Res.drawable.x_circle,
                edit = Res.drawable.pencil_simple_line,
                lock = Res.drawable.lock_simple,
                denominationChevron = Res.drawable.caret_down,
            ),
        )

    @Composable
    fun LeadingActions(
        state: AmountFieldState,
        style: AmountFieldStyle,
        onAction: (AmountFieldAction) -> Unit,
    ) {
        val interaction = state.interaction
        val showsClear = interaction !is AmountFieldInteraction.Locked &&
            interaction !is AmountFieldInteraction.ReadOnly &&
            state.value.isNotEmpty()

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showsClear) {
                IconButton(
                    onClick = { onAction(AmountFieldAction.Clear) },
                    enabled = interaction.allowsFieldAction(),
                ) {
                    Icon(
                        painter = painterResource(style.icons.clear),
                        contentDescription = stringResource(Res.string.id_clear),
                        modifier = Modifier
                            .size(ActionIconSize)
                            .appTestTag("clear"),
                    )
                }
            }

            AnimatedVisibility(visible = interaction is AmountFieldInteraction.Locked) {
                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        painter = painterResource(style.icons.lock),
                        contentDescription = null,
                        tint = style.colors.accent,
                        modifier = Modifier.size(LockIconSize),
                    )
                }
            }
        }
    }

    @Composable
    fun TrailingActions(
        state: AmountFieldState,
        style: AmountFieldStyle,
        onAction: (AmountFieldAction) -> Unit,
    ) {
        val interaction = state.interaction
        val canSelectDenomination = state.canSelectDenomination && interaction.allowsFieldAction()

        Row(
            modifier = Modifier.ifTrue(interaction !is AmountFieldInteraction.ReadOnly) {
                it.padding(end = 8.dp)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .ifTrue(canSelectDenomination) {
                        it.clickable { onAction(AmountFieldAction.SelectDenomination) }
                    }
                    .appTestTag("amount_denomination"),
            ) {
                Text(
                    text = state.denominationLabel,
                    style = style.typography.denomination,
                    color = style.colors.denomination,
                )

                if (canSelectDenomination) {
                    Icon(
                        painter = painterResource(style.icons.denominationChevron),
                        contentDescription = null,
                        modifier = Modifier
                            .size(DenominationIconSize)
                            .padding(start = 2.dp),
                    )
                } else {
                    Spacer(Modifier.size(DenominationIconSize))
                }
            }

            if (interaction is AmountFieldInteraction.ReadOnly) {
                IconButton(onClick = { onAction(AmountFieldAction.Edit) }) {
                    Icon(
                        painter = painterResource(style.icons.edit),
                        contentDescription = stringResource(Res.string.id_edit),
                        modifier = Modifier.size(ActionIconSize),
                    )
                }
            }
        }
    }
}

val LocalAmountFieldStyle = staticCompositionLocalOf<AmountFieldStyle?> { null }

class AmountFieldFeedbackScope internal constructor(
    val severity: AmountFieldFeedbackSeverity,
    val style: AmountFieldFeedbackStyle,
)

internal fun AmountFieldInteraction.allowsTextInput(): Boolean =
    this is AmountFieldInteraction.Editable

internal fun AmountFieldInteraction.allowsTextSelection(): Boolean =
    this is AmountFieldInteraction.Editable || this is AmountFieldInteraction.ReadOnly

internal fun AmountFieldInteraction.allowsFieldAction(): Boolean =
    this is AmountFieldInteraction.Editable

internal fun AmountFieldState.visibleFeedback(hasFeedbackContent: Boolean): AmountFieldFeedback? =
    feedback?.takeIf { it.text != null || hasFeedbackContent }

internal fun syncAmountTextFieldValue(
    current: TextFieldValue,
    externalValue: String,
): TextFieldValue = current.copy(
    text = externalValue,
    selection = if (current.text.length != externalValue.length) {
        TextRange(externalValue.length)
    } else {
        current.selection
    },
)

private val ActionIconSize = 24.dp
private val LockIconSize = 16.dp
private val DenominationIconSize = 12.dp
private val FieldPadding = 16.dp

@Composable
fun AmountField(
    state: AmountFieldState,
    onValueChange: (String) -> Unit,
    onAction: (AmountFieldAction) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    focusRequester: FocusRequester? = null,
    style: AmountFieldStyle = LocalAmountFieldStyle.current ?: AmountFieldDefaults.style,
    leadingContent: @Composable () -> Unit = {
        AmountFieldDefaults.LeadingActions(state = state, style = style, onAction = onAction)
    },
    trailingContent: @Composable () -> Unit = {
        AmountFieldDefaults.TrailingActions(state = state, style = style, onAction = onAction)
    },
    feedbackContent: (@Composable AmountFieldFeedbackScope.() -> Unit)? = null,
) {
    // Nothing to render means no panel and no recolored border.
    val feedback = state.visibleFeedback(hasFeedbackContent = feedbackContent != null)
    val feedbackStyle = feedback?.let { style.colors.feedback[it.severity] }
    val density = LocalDensity.current
    var startRowWidth by remember { mutableStateOf(0.dp) }
    var endRowWidth by remember { mutableStateOf(0.dp) }
    val edgeWidth = max(startRowWidth, endRowWidth)

    val internalFocusRequester = remember { FocusRequester() }
    val fieldFocusRequester = focusRequester ?: internalFocusRequester
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = bodyLarge,
                color = whiteMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }

        CardSurface(
            colors = CardDefaults.outlinedCardColors(containerColor = style.colors.fieldContainer),
            border = BorderStroke(1.dp, feedbackStyle?.borderColor ?: style.colors.fieldBorder),
            shape = if (feedback != null) GreenSmallBottom else CardDefaults.shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier.ifTrue(state.interaction.allowsTextInput()) {
                    it.clickable(
                        interactionSource = fieldInteractionSource,
                        indication = null,
                    ) {
                        runCatching { fieldFocusRequester.requestFocus() }
                        keyboardController?.show()
                    }
                },
            ) {
                GradientEdgeBox(startSolidWidth = edgeWidth, endSolidWidth = edgeWidth) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = FieldPadding)
                            .padding(horizontal = edgeWidth),
                    ) {
                        AmountTextInput(
                            value = state.value,
                            onValueChange = onValueChange,
                            interaction = state.interaction,
                            focusRequester = fieldFocusRequester,
                            maxFractionDigits = state.maxFractionDigits,
                            style = style,
                        )

                        AmountSecondaryValue(
                            secondaryValue = state.secondaryValue,
                            style = style,
                        )
                    }
                }

                MeasuredEdge(
                    alignment = Alignment.CenterStart,
                    onWidthChanged = { startRowWidth = with(density) { it.toDp() } },
                    content = leadingContent,
                )

                MeasuredEdge(
                    alignment = Alignment.CenterEnd,
                    onWidthChanged = { endRowWidth = with(density) { it.toDp() } },
                    content = trailingContent,
                )
            }
        }

        AnimatedNullableVisibility(value = feedback) { currentFeedback ->
            val currentStyle = style.colors.feedback[currentFeedback.severity]
            SupportingPanel(
                colors = CardDefaults.cardColors(
                    contentColor = currentStyle.contentColor,
                    containerColor = currentStyle.containerColor,
                ),
                contentPadding = PaddingValues(12.dp),
                shape = GreenSmallTop,
            ) {
                AmountFieldFeedbackRow(
                    feedback = currentFeedback,
                    feedbackStyle = currentStyle,
                    textStyle = style.typography.feedback,
                    customContent = feedbackContent,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MeasuredEdge(
    alignment: Alignment,
    onWidthChanged: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .onGloballyPositioned { onWidthChanged(it.size.width) },
    ) {
        content()
    }
}

@Composable
private fun AmountTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    interaction: AmountFieldInteraction,
    focusRequester: FocusRequester,
    maxFractionDigits: Int,
    style: AmountFieldStyle,
) {
    val formatter = remember(maxFractionDigits) {
        DecimalFormatter(
            decimalSeparator = DecimalFormat.DecimalSeparator.first(),
            groupingSeparator = DecimalFormat.GroupingSeparator.first(),
            maxFractionDigits = maxFractionDigits,
        )
    }
    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val textFieldValue = syncAmountTextFieldValue(textFieldValueState, value)

    SideEffect {
        if (textFieldValue != textFieldValueState) {
            textFieldValueState = textFieldValue
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValueState = formatter.cleanup(it).also { cleaned ->
                onValueChange(cleaned.text)
            }
        },
        enabled = interaction.allowsTextSelection(),
        readOnly = interaction is AmountFieldInteraction.ReadOnly,
        textStyle = LocalTextStyle.current
            .merge(style.typography.amount)
            .copy(color = style.colors.input),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        cursorBrush = SolidColor(style.colors.accent),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .appTestTag("amount"),
    )
}

@Composable
private fun AmountSecondaryValue(
    secondaryValue: AmountFieldSecondaryValue,
    style: AmountFieldStyle,
) {
    AnimatedVisibility(visible = secondaryValue !is AmountFieldSecondaryValue.Hidden) {
        when (secondaryValue) {
            AmountFieldSecondaryValue.Hidden -> Unit

            AmountFieldSecondaryValue.Loading -> {
                Box {
                    CircularProgressIndicator(
                        strokeWidth = 1.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(8.dp),
                    )
                }
            }

            is AmountFieldSecondaryValue.Value -> {
                Text(
                    text = secondaryValue.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .appTestTag("amount_converted"),
                    maxLines = 1,
                    style = style.typography.secondary,
                    textAlign = TextAlign.Center,
                    color = style.colors.secondary,
                )
            }
        }
    }
}

@Composable
private fun AmountFieldFeedbackRow(
    feedback: AmountFieldFeedback,
    feedbackStyle: AmountFieldFeedbackStyle,
    textStyle: TextStyle,
    customContent: (@Composable AmountFieldFeedbackScope.() -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (customContent != null) {
            AmountFieldFeedbackScope(
                severity = feedback.severity,
                style = feedbackStyle,
            ).customContent()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(feedback.icon ?: feedbackStyle.icon),
                    contentDescription = null,
                    tint = feedbackStyle.iconTint,
                    modifier = Modifier.size(16.dp),
                )
                feedback.text?.let {
                    Text(
                        text = it.string(),
                        style = textStyle,
                        color = feedbackStyle.contentColor,
                    )
                }
            }
        }
    }
}
