package com.blockstream.designsystem.components

import com.blockstream.designsystem.AppRadius
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.fill.Info
import com.adamglin.phosphoricons.fill.Warning
import com.adamglin.phosphoricons.fill.WarningDiamond
import com.adamglin.phosphoricons.light.XCircle
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.AppTypography
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.foundation.ProvideAppContentColorTextStyle
import com.blockstream.designsystem.generated.resources.Res
import com.blockstream.designsystem.generated.resources.textfield_clear
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    label: String? = null,
    message: AppTextFieldMessage? = null,
    supporting: String? = null,
    clearable: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    style: AppTextFieldStyle = AppTextFieldDefaults.style(),
    interactionSource: MutableInteractionSource? = null
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    AppTextFieldFrame(
        enabled = enabled,
        readOnly = readOnly,
        isEmpty = value.isEmpty(),
        placeholder = placeholder,
        label = label,
        message = message,
        supporting = supporting,
        clearable = clearable,
        minLines = minLines,
        onTextLayout = onTextLayout,
        leading = leading,
        trailing = trailing,
        prefix = prefix,
        suffix = suffix,
        style = style,
        interactionSource = interactions,
        onClear = { onValueChange("") }
    ) { textStyle, cursorBrush, fieldModifier, onLayout, decorationBox ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.then(fieldModifier),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            onTextLayout = onLayout,
            interactionSource = interactions,
            cursorBrush = cursorBrush,
            decorationBox = decorationBox
        )
    }
}

@Composable
fun AppTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    label: String? = null,
    message: AppTextFieldMessage? = null,
    supporting: String? = null,
    clearable: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    style: AppTextFieldStyle = AppTextFieldDefaults.style(),
    interactionSource: MutableInteractionSource? = null
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    AppTextFieldFrame(
        enabled = enabled,
        readOnly = readOnly,
        isEmpty = value.text.isEmpty(),
        placeholder = placeholder,
        label = label,
        message = message,
        supporting = supporting,
        clearable = clearable,
        minLines = minLines,
        onTextLayout = onTextLayout,
        leading = leading,
        trailing = trailing,
        prefix = prefix,
        suffix = suffix,
        style = style,
        interactionSource = interactions,
        onClear = { onValueChange(TextFieldValue()) }
    ) { textStyle, cursorBrush, fieldModifier, onLayout, decorationBox ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.then(fieldModifier),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            onTextLayout = onLayout,
            interactionSource = interactions,
            cursorBrush = cursorBrush,
            decorationBox = decorationBox
        )
    }
}

@Composable
private fun AppTextFieldFrame(
    enabled: Boolean,
    readOnly: Boolean,
    isEmpty: Boolean,
    placeholder: String?,
    label: String?,
    message: AppTextFieldMessage?,
    supporting: String?,
    clearable: Boolean,
    minLines: Int,
    onTextLayout: (TextLayoutResult) -> Unit,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    prefix: (@Composable () -> Unit)?,
    suffix: (@Composable () -> Unit)?,
    style: AppTextFieldStyle,
    interactionSource: MutableInteractionSource,
    onClear: () -> Unit,
    textField: @Composable (
        textStyle: TextStyle,
        cursorBrush: Brush,
        modifier: Modifier,
        onTextLayout: (TextLayoutResult) -> Unit,
        decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit
    ) -> Unit
) {
    var lineCount by remember { mutableIntStateOf(1) }
    val activeMessage = message?.takeIf { it.text.isNotBlank() }
    val focused by interactionSource.collectIsFocusedAsState()
    val state = AppTextFieldState.of(
        enabled = enabled,
        focused = focused,
        message = activeMessage?.type
    )
    val textStyle = style.textStyle.merge(color = style.textStyle.color.takeOrElse { style.colors.textColor(state) })
    val cursorBrush = remember(style.colors.cursorColor) { SolidColor(style.colors.cursorColor) }
    val clearButton: (@Composable () -> Unit)? = if (trailing == null && clearable && enabled && !readOnly && !isEmpty) {
        { AppTextFieldDefaults.ClearButton(onClick = onClear) }
    } else {
        null
    }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .semantics { if (activeMessage?.type == AppTextFieldMessageType.Error) error(activeMessage.text) }

    val onLayout: (TextLayoutResult) -> Unit = { layout ->
        lineCount = layout.lineCount
        onTextLayout(layout)
    }
    textField(textStyle, cursorBrush, fieldModifier, onLayout) { innerTextField ->
        AppTextFieldDecoration(
            state = state,
            style = style,
            minLines = minLines,
            lineCount = lineCount,
            isEmpty = isEmpty,
            placeholder = placeholder,
            label = label,
            message = activeMessage,
            supporting = supporting,
            leading = leading,
            trailing = trailing ?: clearButton,
            prefix = prefix,
            suffix = suffix,
            innerTextField = innerTextField
        )
    }
}

enum class AppTextFieldMessageType { Error, Warning, Info }

@Immutable
class AppTextFieldMessage(val type: AppTextFieldMessageType, val text: String)

enum class AppTextFieldState {
    Default,
    Focused,
    Error,
    Warning,
    Info,
    Disabled;

    companion object {
        fun of(enabled: Boolean, focused: Boolean, message: AppTextFieldMessageType?): AppTextFieldState = when {
            !enabled -> Disabled
            message == AppTextFieldMessageType.Error -> Error
            message == AppTextFieldMessageType.Warning -> Warning
            message == AppTextFieldMessageType.Info -> Info
            focused -> Focused
            else -> Default
        }
    }
}

@Immutable
class AppTextFieldColors(
    val containerColor: Color,
    val focusedContainerColor: Color,
    val errorContainerColor: Color,
    val warningContainerColor: Color,
    val infoContainerColor: Color,
    val disabledContainerColor: Color,
    val borderColor: Color,
    val focusedBorderColor: Color,
    val errorBorderColor: Color,
    val warningBorderColor: Color,
    val infoBorderColor: Color,
    val disabledBorderColor: Color,
    val textColor: Color,
    val focusedTextColor: Color,
    val errorTextColor: Color,
    val warningTextColor: Color,
    val infoTextColor: Color,
    val disabledTextColor: Color,
    val placeholderColor: Color,
    val contentColor: Color,
    val disabledContentColor: Color,
    val labelColor: Color,
    val supportingColor: Color,
    val cursorColor: Color,
    val errorMessageContainerColor: Color,
    val errorMessageIconColor: Color,
    val warningMessageContainerColor: Color,
    val warningMessageIconColor: Color,
    val infoMessageContainerColor: Color,
    val infoMessageIconColor: Color,
    val messageTextColor: Color
) {
    @Stable
    fun containerColor(state: AppTextFieldState): Color = when (state) {
        AppTextFieldState.Default -> containerColor
        AppTextFieldState.Focused -> focusedContainerColor
        AppTextFieldState.Error -> errorContainerColor
        AppTextFieldState.Warning -> warningContainerColor
        AppTextFieldState.Info -> infoContainerColor
        AppTextFieldState.Disabled -> disabledContainerColor
    }

    @Stable
    fun borderColor(state: AppTextFieldState): Color = when (state) {
        AppTextFieldState.Default -> borderColor
        AppTextFieldState.Focused -> focusedBorderColor
        AppTextFieldState.Error -> errorBorderColor
        AppTextFieldState.Warning -> warningBorderColor
        AppTextFieldState.Info -> infoBorderColor
        AppTextFieldState.Disabled -> disabledBorderColor
    }

    @Stable
    fun textColor(state: AppTextFieldState): Color = when (state) {
        AppTextFieldState.Default -> textColor
        AppTextFieldState.Focused -> focusedTextColor
        AppTextFieldState.Error -> errorTextColor
        AppTextFieldState.Warning -> warningTextColor
        AppTextFieldState.Info -> infoTextColor
        AppTextFieldState.Disabled -> disabledTextColor
    }

    @Stable
    fun messageContainerColor(type: AppTextFieldMessageType): Color = when (type) {
        AppTextFieldMessageType.Error -> errorMessageContainerColor
        AppTextFieldMessageType.Warning -> warningMessageContainerColor
        AppTextFieldMessageType.Info -> infoMessageContainerColor
    }

    @Stable
    fun messageIconColor(type: AppTextFieldMessageType): Color = when (type) {
        AppTextFieldMessageType.Error -> errorMessageIconColor
        AppTextFieldMessageType.Warning -> warningMessageIconColor
        AppTextFieldMessageType.Info -> infoMessageIconColor
    }

    @Stable
    fun contentColor(state: AppTextFieldState): Color =
        if (state == AppTextFieldState.Disabled) disabledContentColor else contentColor
}

@Immutable
class AppTextFieldStyle(
    val colors: AppTextFieldColors,
    val shape: Shape,
    val borderWidth: Dp,
    val minHeight: Dp,
    val multilineMinHeight: Dp,
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val affixSpacing: Dp,
    val labelSpacing: Dp,
    val supportingSpacing: Dp,
    val messagePadding: Dp,
    val messageSpacing: Dp,
    val messageIconSize: Dp,
    val textStyle: TextStyle,
    val labelStyle: TextStyle,
    val supportingStyle: TextStyle,
    val messageStyle: TextStyle
)

object AppTextFieldDefaults {
    val Shape: Shape = RoundedCornerShape(AppRadius.md)
    val BorderWidth = 1.dp
    val MinHeight = 60.dp
    val MultilineMinHeight = 120.dp
    val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val ContentSpacing = 8.dp
    val AffixSpacing = 4.dp
    val LabelSpacing = 8.dp
    val SupportingSpacing = 4.dp
    val MessagePadding = 12.dp
    val MessageSpacing = 4.dp
    val MessageIconSize = 16.dp
    val IconButtonSize = 44.dp
    val IconSize = 20.dp

    @Composable
    fun style(): AppTextFieldStyle {
        val colors = AppTheme.colors
        val typography = AppTheme.typography
        return remember(colors, typography) { style(colors, typography) }
    }

    fun style(colors: AppColors, typography: AppTypography): AppTextFieldStyle = AppTextFieldStyle(
        colors = colors(colors),
        shape = Shape,
        borderWidth = BorderWidth,
        minHeight = MinHeight,
        multilineMinHeight = MultilineMinHeight,
        contentPadding = ContentPadding,
        contentSpacing = ContentSpacing,
        affixSpacing = AffixSpacing,
        labelSpacing = LabelSpacing,
        supportingSpacing = SupportingSpacing,
        messagePadding = MessagePadding,
        messageSpacing = MessageSpacing,
        messageIconSize = MessageIconSize,
        textStyle = typography.label.m,
        labelStyle = typography.label.m,
        supportingStyle = typography.label.sm,
        messageStyle = typography.label.sm
    )

    fun colors(colors: AppColors): AppTextFieldColors = AppTextFieldColors(
        containerColor = colors.surface.base,
        focusedContainerColor = colors.surface.base,
        errorContainerColor = colors.surface.base,
        warningContainerColor = colors.surface.base,
        infoContainerColor = colors.surface.base,
        disabledContainerColor = colors.surface.base,
        borderColor = colors.border.subtle,
        focusedBorderColor = colors.border.brand,
        errorBorderColor = colors.feedback.negative.stroke,
        warningBorderColor = colors.feedback.warning.stroke,
        infoBorderColor = colors.feedback.info.stroke,
        disabledBorderColor = colors.border.subtle,
        textColor = colors.text.primary,
        focusedTextColor = colors.text.primary,
        errorTextColor = colors.text.primary,
        warningTextColor = colors.text.primary,
        infoTextColor = colors.text.primary,
        disabledTextColor = colors.text.disabled,
        placeholderColor = colors.text.subtle,
        contentColor = colors.icon.subtle,
        disabledContentColor = colors.icon.disabled,
        labelColor = colors.text.subtle,
        supportingColor = colors.text.subtle,
        cursorColor = colors.action.primary,
        errorMessageContainerColor = colors.feedback.negative.fill,
        errorMessageIconColor = colors.feedback.negative.accent,
        warningMessageContainerColor = colors.feedback.warning.fill,
        warningMessageIconColor = colors.feedback.warning.accent,
        infoMessageContainerColor = colors.feedback.info.fill,
        infoMessageIconColor = colors.feedback.info.accent,
        messageTextColor = colors.text.primary
    )

    @Composable
    fun ClearButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
        IconButton(
            imageVector = PhosphorIcons.Light.XCircle,
            contentDescription = stringResource(Res.string.textfield_clear),
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        )
    }

    @Composable
    fun IconButton(
        imageVector: ImageVector,
        contentDescription: String?,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        AppSurface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .size(IconButtonSize)
                .semantics { role = Role.Button },
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppIcon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(IconSize)
                )
            }
        }
    }
}

@Composable
private fun AppTextFieldDecoration(
    state: AppTextFieldState,
    style: AppTextFieldStyle,
    minLines: Int,
    lineCount: Int,
    isEmpty: Boolean,
    placeholder: String?,
    label: String?,
    message: AppTextFieldMessage?,
    supporting: String?,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    prefix: (@Composable () -> Unit)?,
    suffix: (@Composable () -> Unit)?,
    innerTextField: @Composable () -> Unit
) {
    val colors = style.colors
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = style.contentPadding.calculateStartPadding(layoutDirection)
    val endPadding = style.contentPadding.calculateEndPadding(layoutDirection)
    val topPadding = style.contentPadding.calculateTopPadding()
    val lineHeight = with(LocalDensity.current) { style.textStyle.lineHeight.toDp() }
    val topAligned = minLines > 1 || lineCount > 1
    val slotAlignment = if (topAligned) Alignment.Top else Alignment.CenterVertically
    val slotTopPadding = if (topAligned) topPadding else 0.dp
    val minHeight = if (minLines > 1) style.multilineMinHeight else style.minHeight
    val shownMessage = remember { AppTextFieldShownMessage() }
    if (message != null) {
        shownMessage.text = message.text
        shownMessage.type = message.type
    }
    val messageVisibility = remember { MutableTransitionState(message != null) }
    messageVisibility.targetState = message != null
    val messageShown = messageVisibility.currentState || messageVisibility.targetState

    Column {
        if (label != null) {
            AppText(
                text = label,
                style = style.labelStyle,
                color = colors.labelColor,
                modifier = Modifier.padding(bottom = style.labelSpacing)
            )
        }

        val outer = if (messageShown) {
            Modifier
                .clip(style.shape)
                .background(colors.messageContainerColor(shownMessage.type))
                .border(style.borderWidth, colors.borderColor(state), style.shape)
        } else {
            Modifier
        }

        Column(outer) {
            AppSurface(
                shape = style.shape,
                color = colors.containerColor(state),
                contentColor = colors.contentColor(state),
                border = BorderStroke(style.borderWidth, colors.borderColor(state)),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = minHeight)
            ) {
                Row(
                    verticalAlignment = slotAlignment,
                    horizontalArrangement = Arrangement.spacedBy(style.contentSpacing)
                ) {
                    if (leading != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = startPadding, top = slotTopPadding)
                                .heightIn(min = lineHeight),
                            contentAlignment = Alignment.Center
                        ) { leading() }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                start = if (leading == null) startPadding else 0.dp,
                                end = endPadding,
                                top = topPadding,
                                bottom = style.contentPadding.calculateBottomPadding()
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(style.affixSpacing)
                    ) {
                        if (prefix != null) {
                            ProvideAppContentColorTextStyle(colors.contentColor(state), style.textStyle) { prefix() }
                        }
                        Box(Modifier.weight(1f)) {
                            if (placeholder != null && isEmpty && state != AppTextFieldState.Disabled) {
                                AppText(text = placeholder, style = style.textStyle, color = colors.placeholderColor)
                            }
                            innerTextField()
                        }
                        if (suffix != null) {
                            ProvideAppContentColorTextStyle(colors.contentColor(state), style.textStyle) { suffix() }
                        }
                    }

                    if (trailing != null) {
                        Box(
                            modifier = Modifier.heightIn(min = lineHeight + topPadding * 2),
                            contentAlignment = Alignment.Center
                        ) { trailing() }
                    }
                }
            }

            AnimatedVisibility(
                visibleState = messageVisibility,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(style.messagePadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(style.messageSpacing)
                ) {
                    AppIcon(
                        imageVector = shownMessage.type.icon(),
                        contentDescription = null,
                        tint = colors.messageIconColor(shownMessage.type),
                        modifier = Modifier.size(style.messageIconSize)
                    )
                    AppText(text = shownMessage.text, style = style.messageStyle, color = colors.messageTextColor)
                }
            }
        }

        if (!messageShown && supporting != null) {
            AppText(
                text = supporting,
                style = style.supportingStyle,
                color = colors.supportingColor,
                modifier = Modifier.padding(top = style.supportingSpacing)
            )
        }
    }
}

private class AppTextFieldShownMessage {
    var text: String = ""
    var type: AppTextFieldMessageType = AppTextFieldMessageType.Error
}

private fun AppTextFieldMessageType.icon(): ImageVector = when (this) {
    AppTextFieldMessageType.Error -> PhosphorIcons.Fill.WarningDiamond
    AppTextFieldMessageType.Warning -> PhosphorIcons.Fill.Warning
    AppTextFieldMessageType.Info -> PhosphorIcons.Fill.Info
}

@Preview
@Composable
private fun TextFieldPreviewDark() = AppTextFieldPreview(isLight = false)

@Preview
@Composable
private fun TextFieldPreviewLight() = AppTextFieldPreview(isLight = true)

@Composable
private fun AppTextFieldPreview(isLight: Boolean) {
    ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppTextField(value = "", onValueChange = {}, placeholder = "Placeholder")
                AppTextField(value = "Filled", onValueChange = {})
                AppTextField(value = "Filled", onValueChange = {}, message = AppTextFieldMessage(AppTextFieldMessageType.Error, "Message"))
                AppTextField(value = "Filled", onValueChange = {}, message = AppTextFieldMessage(AppTextFieldMessageType.Warning, "Message"))
                AppTextField(value = "Filled", onValueChange = {}, message = AppTextFieldMessage(AppTextFieldMessageType.Info, "Message"))
                AppTextField(value = "", onValueChange = {}, placeholder = "Placeholder", enabled = false)
                AppTextField(value = "", onValueChange = {}, placeholder = "Placeholder", label = "Label", supporting = "Supporting text")
            }
        }
    }
}
