package com.blockstream.designsystem.components

import androidx.compose.ui.layout.layout
import com.blockstream.designsystem.AppColors
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.light.Funnel
import com.adamglin.phosphoricons.light.ArrowLeft
import com.adamglin.phosphoricons.light.Info
import com.adamglin.phosphoricons.light.Scan
import com.adamglin.phosphoricons.light.Wallet
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.foundation.appRipple
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppNavBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    colors: AppNavBarColors = AppNavBarDefaults.colors(),
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.containerColor)
            .windowInsetsPadding(windowInsets)
            .height(AppNavBarDefaults.Height)
            .padding(start = AppNavBarDefaults.StartPadding, end = AppNavBarDefaults.EndPadding),
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppNavBarDefaults.LeadingSpacing),
                verticalAlignment = Alignment.CenterVertically,
                content = leading
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppNavBarDefaults.TrailingSpacing),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing
            )
        }
    ) { measurables, constraints ->
        val (leadingMeasurable, trailingMeasurable) = measurables
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gap = AppNavBarDefaults.SlotGap.roundToPx()
        val leadingReserved = minOf(leadingMeasurable.maxIntrinsicWidth(height), width / 2)
        val trailing = trailingMeasurable.measure(
            Constraints(maxWidth = (width - leadingReserved - gap).coerceAtLeast(0), maxHeight = height)
        )
        val leading = leadingMeasurable.measure(
            Constraints(maxWidth = (width - trailing.width - gap).coerceAtLeast(0), maxHeight = height)
        )

        layout(width, height) {
            leading.placeRelative(0, (height - leading.height) / 2)
            trailing.placeRelative(width - trailing.width, (height - trailing.height) / 2)
        }
    }
}

@Composable
fun AppNavBarTitle(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    iconModifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    colors: AppNavBarColors = AppNavBarDefaults.colors()
) {
    Row(
        modifier = modifier.height(AppNavBarDefaults.ItemHeight),
        horizontalArrangement = Arrangement.spacedBy(AppNavBarDefaults.LeadingSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressedAlpha by animatePressedAlpha(interactionSource)
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val inset = ((AppNavBarDefaults.ItemHeight - AppNavBarDefaults.BackIconSize) / 2).roundToPx()
                        layout(placeable.width - inset * 2, placeable.height) { placeable.place(-inset, 0) }
                    }
                    .size(AppNavBarDefaults.ItemHeight)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onBack
                    )
                    .alpha(pressedAlpha),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    PhosphorIcons.Light.ArrowLeft,
                    contentDescription = backContentDescription,
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(AppNavBarDefaults.BackIconSize)
                        .scale(scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f, scaleY = 1f)
                )
            }
        }
        if (icon != null || title != null || subtitle != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppNavBarDefaults.TitleIconSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    AppIcon(
                        it,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = iconModifier.size(AppNavBarDefaults.IconSize)
                    )
                }
                if (title != null || subtitle != null) {
                    Column(modifier = Modifier.semantics { heading() }) {
                        title?.let {
                            AppText(
                                text = it,
                                style = AppTheme.typography.label.lSemiBold,
                                color = colors.contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        subtitle?.let {
                            AppText(
                                text = it,
                                style = AppTheme.typography.body.s,
                                color = colors.subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavBarTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: AppNavBarColors = AppNavBarDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedAlpha by animatePressedAlpha(interactionSource)

    Box(
        modifier = modifier
            .height(AppNavBarDefaults.ItemHeight)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .alpha(pressedAlpha)
            .padding(horizontal = AppNavBarDefaults.TrailingItemInset),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            style = AppTheme.typography.label.mMedium,
            color = if (enabled) colors.actionColor else colors.disabledContentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class AppNavBarIconButtonVariant {
    Plain,
    Bordered
}

@Composable
fun AppNavBarIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppNavBarIconButtonVariant = AppNavBarIconButtonVariant.Plain,
    enabled: Boolean = true,
    active: Boolean = false,
    colors: AppNavBarColors = AppNavBarDefaults.colors()
) {
    Box(
        modifier = modifier
            .then(if (variant == AppNavBarIconButtonVariant.Bordered) Modifier.padding(end = AppNavBarDefaults.BorderedButtonEndInset) else Modifier)
            .size(AppNavBarDefaults.ItemHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AppNavBarDefaults.IconButtonSize)
                .clip(CircleShape)
                .then(
                    when (variant) {
                        AppNavBarIconButtonVariant.Plain -> Modifier
                        AppNavBarIconButtonVariant.Bordered -> Modifier
                            .background(colors.buttonContainerColor)
                            .border(BorderStroke(AppNavBarDefaults.BorderWidth, colors.buttonBorderColor), CircleShape)
                    }
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = appRipple(),
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AppNavBarIcon(
                painter = icon,
                contentDescription = contentDescription,
                tint = if (enabled) colors.contentColor else colors.disabledContentColor,
                active = active && enabled,
                colors = colors
            )
        }
    }
}

@Composable
fun AppNavBarIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AppNavBarDefaults.colors().contentColor,
    size: Dp = AppNavBarDefaults.IconSize,
    active: Boolean = false,
    colors: AppNavBarColors = AppNavBarDefaults.colors()
) {
    val scale = size / AppNavBarDefaults.IconSize
    Box(modifier = modifier.size(size)) {
        AppIcon(
            painter = painter,
            contentDescription = contentDescription,
            tint = if (active) colors.activeColor else tint,
            modifier = Modifier.size(size)
        )
        if (active) {
            Box(
                modifier = Modifier
                    .offset(x = AppNavBarDefaults.ActiveDotOffset * scale)
                    .size(AppNavBarDefaults.ActiveDotSize * scale)
                    .background(colors.containerColor, CircleShape)
                    .padding(AppNavBarDefaults.BorderWidth * scale)
                    .background(colors.activeColor, CircleShape)
            )
        }
    }
}

@Composable
fun AppNavBarRegionButton(
    code: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: AppNavBarColors = AppNavBarDefaults.colors()
) {
    Box(
        modifier = modifier.size(AppNavBarDefaults.ItemHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AppNavBarDefaults.IconButtonSize)
                .clip(CircleShape)
                .clickable(
                    interactionSource = null,
                    indication = appRipple(),
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(AppNavBarDefaults.RegionCircleSize)
                    .border(BorderStroke(AppNavBarDefaults.RegionBorderWidth, colors.contentColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = code,
                    style = AppTheme.typography.label.xs,
                    color = colors.contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AppNavBarWalletChip(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: AppNavBarColors = AppNavBarDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedAlpha by animatePressedAlpha(interactionSource)

    Row(
        modifier = modifier
            .height(AppNavBarDefaults.ItemHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .alpha(pressedAlpha)
            .padding(start = AppNavBarDefaults.LeadingSpacing, end = AppNavBarDefaults.TrailingItemInset),
        horizontalArrangement = Arrangement.spacedBy(AppNavBarDefaults.LeadingSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = name,
            style = AppTheme.typography.label.lMedium,
            color = colors.actionColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = AppNavBarDefaults.WalletChipNameMaxWidth)
        )
        Box(
            modifier = Modifier
                .size(AppNavBarDefaults.WalletChipIconContainerSize)
                .background(colors.chipContainerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                PhosphorIcons.Light.Wallet,
                contentDescription = null,
                tint = colors.chipContentColor,
                modifier = Modifier.size(AppNavBarDefaults.WalletChipIconSize)
            )
        }
    }
}

@Immutable
class AppNavBarColors(
    val containerColor: Color,
    val contentColor: Color,
    val subtitleColor: Color,
    val actionColor: Color,
    val disabledContentColor: Color,
    val activeColor: Color,
    val buttonContainerColor: Color,
    val buttonBorderColor: Color,
    val chipContainerColor: Color,
    val chipContentColor: Color
)

object AppNavBarDefaults {
    val Height = 56.dp
    val ItemHeight = 48.dp
    val IconButtonSize = 40.dp
    val IconSize = 24.dp
    val BackIconSize = 20.dp
    val StartPadding = 20.dp
    val EndPadding = 8.dp
    val SlotGap = 8.dp
    val LeadingSpacing = 8.dp
    val TrailingSpacing = 4.dp
    val TitleIconSpacing = 12.dp
    val TrailingItemInset = 12.dp
    val BorderedButtonEndInset = 8.dp
    val BorderWidth = 1.dp
    val ActiveDotSize = 8.dp
    val ActiveDotOffset = 16.dp
    val RegionCircleSize = 24.dp
    val RegionBorderWidth = 1.5.dp
    val WalletChipNameMaxWidth = 180.dp
    val WalletChipIconContainerSize = 28.dp
    val WalletChipIconSize = 16.dp

    @Composable
    fun colors(): AppNavBarColors {
        val colors = AppTheme.colors
        return remember(colors) { colors(colors) }
    }

    fun colors(colors: AppColors): AppNavBarColors = AppNavBarColors(
        containerColor = colors.surface.canvas,
        contentColor = colors.text.primary,
        subtitleColor = colors.text.subtle,
        actionColor = colors.text.brand,
        disabledContentColor = colors.text.disabled,
        activeColor = colors.icon.brand,
        buttonContainerColor = colors.surface.base,
        buttonBorderColor = colors.border.subtle,
        chipContainerColor = colors.action.primary,
        chipContentColor = colors.action.onPrimary
    )
}

@Composable
private fun animatePressedAlpha(interactionSource: MutableInteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) PressedAlpha else 1f, tween(100))
}

private const val PressedAlpha = 0.5f

@Preview
@Composable
private fun AppNavBarPreview() {
    ProvideAppTheme(isLight = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppNavBar(
                windowInsets = WindowInsets(0),
                leading = { AppNavBarTitle(title = "Title", onBack = {}) },
                trailing = { AppNavBarTextAction(text = "Add Note", onClick = {}) }
            )
            AppNavBar(
                windowInsets = WindowInsets(0),
                leading = { AppNavBarTitle(title = "Transact") },
                trailing = { AppNavBarWalletChip(name = "Wallet #1", onClick = {}) }
            )
            AppNavBar(
                windowInsets = WindowInsets(0),
                leading = { AppNavBarTitle(title = "Confirm Transaction", subtitle = "My Wallet", onBack = {}) },
                trailing = {
                    AppNavBarIconButton(
                        icon = rememberVectorPainter(PhosphorIcons.Light.Scan),
                        contentDescription = "Scan",
                        variant = AppNavBarIconButtonVariant.Bordered,
                        onClick = {}
                    )
                    AppNavBarIconButton(
                        icon = rememberVectorPainter(PhosphorIcons.Light.Info),
                        contentDescription = "About",
                        onClick = {}
                    )
                    AppNavBarIconButton(
                        icon = rememberVectorPainter(PhosphorIcons.Light.Funnel),
                        contentDescription = "Filter",
                        active = true,
                        onClick = {}
                    )
                }
            )
            AppNavBar(
                windowInsets = WindowInsets(0),
                leading = { AppNavBarTitle(title = "Buy", onBack = {}) },
                trailing = { AppNavBarRegionButton(code = "UA", onClick = {}) }
            )
        }
    }
}
