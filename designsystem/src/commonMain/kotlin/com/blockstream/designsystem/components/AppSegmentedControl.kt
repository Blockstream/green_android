package com.blockstream.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppRadius
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.AppTypography
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.foundation.appRipple
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: AppSegmentedControlStyle = AppSegmentedControlDefaults.style()
) {
    Row(
        modifier = modifier.selectableGroup().width(IntrinsicSize.Max).height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(-style.borderWidth)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interactions = remember { MutableInteractionSource() }
            val shape = AppSegmentedControlDefaults.shape(index, labels.size, style.cornerRadius)
            val fill = animateColorAsState(
                targetValue = style.colors.containerColor(selected),
                animationSpec = tween(style.animationDurationMillis, easing = EaseOut)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = selected,
                        interactionSource = interactions,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) }
                    )
                    .padding(vertical = style.touchPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(min = style.minHeight)
                        .clip(shape)
                        .indication(interactions, appRipple())
                        .border(style.borderWidth, style.colors.borderColor, shape)
                        .drawBehind { drawRect(fill.value) }
                        .padding(style.contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = label,
                        style = style.textStyle,
                        color = style.colors.contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Immutable
class AppSegmentedControlColors(
    val containerColor: Color,
    val selectedContainerColor: Color,
    val borderColor: Color,
    val contentColor: Color
) {
    @Stable
    fun containerColor(selected: Boolean): Color = if (selected) selectedContainerColor else containerColor
}

@Immutable
class AppSegmentedControlStyle(
    val colors: AppSegmentedControlColors,
    val cornerRadius: Dp,
    val borderWidth: Dp,
    val minHeight: Dp,
    val touchPadding: Dp,
    val contentPadding: PaddingValues,
    val textStyle: TextStyle,
    val animationDurationMillis: Int
)

object AppSegmentedControlDefaults {
    val CornerRadius = AppRadius.lg
    val BorderWidth = 1.dp
    val MinHeight = 40.dp
    val TouchPadding = 4.dp
    val ContentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    const val AnimationDurationMillis = 300

    @Composable
    fun style(): AppSegmentedControlStyle {
        val colors = AppTheme.colors
        val typography = AppTheme.typography
        return remember(colors, typography) { style(colors, typography) }
    }

    fun style(colors: AppColors, typography: AppTypography): AppSegmentedControlStyle = AppSegmentedControlStyle(
        colors = colors(colors),
        cornerRadius = CornerRadius,
        borderWidth = BorderWidth,
        minHeight = MinHeight,
        touchPadding = TouchPadding,
        contentPadding = ContentPadding,
        textStyle = typography.label.mSemiBold,
        animationDurationMillis = AnimationDurationMillis
    )

    fun colors(colors: AppColors): AppSegmentedControlColors = AppSegmentedControlColors(
        containerColor = colors.surface.base,
        selectedContainerColor = colors.surface.segmented.selected,
        borderColor = colors.border.subtle,
        contentColor = colors.text.primary
    )

    fun shape(index: Int, count: Int, cornerRadius: Dp): Shape {
        val start = if (index == 0) cornerRadius else 0.dp
        val end = if (index == count - 1) cornerRadius else 0.dp
        return RoundedCornerShape(topStart = start, topEnd = end, bottomEnd = end, bottomStart = start)
    }
}

@Preview
@Composable
private fun SegmentedControlPreviewDark() = AppSegmentedControlPreview(isLight = false)

@Preview
@Composable
private fun SegmentedControlPreviewLight() = AppSegmentedControlPreview(isLight = true)

@Composable
private fun AppSegmentedControlPreview(isLight: Boolean) {
    ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppSegmentedControl(
                    labels = listOf("Mobile", "Hardware"),
                    selectedIndex = 0,
                    onSelect = {},
                    modifier = Modifier.fillMaxWidth()
                )
                AppSegmentedControl(labels = listOf("12", "24", "27"), selectedIndex = 1, onSelect = {})
                AppSegmentedControl(labels = listOf("Buy"), selectedIndex = 0, onSelect = {})
            }
        }
    }
}
