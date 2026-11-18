package com.blockstream.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: AppBottomSheetState = rememberAppBottomSheetState(),
    type: AppBottomSheetType = AppBottomSheetType.Modal,
    contentPadding: PaddingValues = AppBottomSheetDefaults.ContentPadding,
    properties: AppBottomSheetProperties = AppBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    AppBottomSheetLayout(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        style = AppBottomSheetDefaults.style(type),
        contentPadding = contentPadding,
        properties = properties,
        content = content
    )
}

enum class AppBottomSheetType {
    Modal,
    Default
}

enum class AppBottomSheetSecurePolicy {
    Inherit,
    SecureOn,
    SecureOff
}

@Immutable
class AppBottomSheetProperties(
    val shouldDismissOnBackPress: Boolean = true,
    val shouldDismissOnClickOutside: Boolean = true,
    val securePolicy: AppBottomSheetSecurePolicy = AppBottomSheetSecurePolicy.Inherit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppBottomSheetProperties) return false
        return shouldDismissOnBackPress == other.shouldDismissOnBackPress &&
            shouldDismissOnClickOutside == other.shouldDismissOnClickOutside &&
            securePolicy == other.securePolicy
    }

    override fun hashCode(): Int {
        var result = shouldDismissOnBackPress.hashCode()
        result = 31 * result + shouldDismissOnClickOutside.hashCode()
        result = 31 * result + securePolicy.hashCode()
        return result
    }
}

@Immutable
class AppBottomSheetColors(
    val containerColor: Color,
    val contentColor: Color,
    val handleColor: Color,
    val scrimColor: Color
)

@Immutable
class AppBottomSheetStyle(
    val colors: AppBottomSheetColors,
    val shape: Shape,
    val maxWidth: Dp,
    val headerPadding: Dp,
    val handleWidth: Dp,
    val handleHeight: Dp,
    val handleShape: Shape
)

object AppBottomSheetDefaults {
    val Shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val ContentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    val MaxWidth = 640.dp
    val HeaderPadding = 16.dp
    val HandleWidth = 32.dp
    val HandleHeight = 4.dp
    const val ScrimAlpha = 0.256f

    internal val MotionSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 300, easing = FastOutSlowInEasing)

    @Composable
    internal fun style(type: AppBottomSheetType): AppBottomSheetStyle {
        val colors = AppTheme.colors
        return remember(type, colors) { style(type, colors) }
    }

    fun style(type: AppBottomSheetType, colors: AppColors): AppBottomSheetStyle = AppBottomSheetStyle(
        colors = colors(type, colors),
        shape = Shape,
        maxWidth = MaxWidth,
        headerPadding = HeaderPadding,
        handleWidth = HandleWidth,
        handleHeight = HandleHeight,
        handleShape = CircleShape
    )

    fun colors(type: AppBottomSheetType, colors: AppColors): AppBottomSheetColors = AppBottomSheetColors(
        containerColor = colors.surface.overlay,
        contentColor = colors.text.primary,
        handleColor = colors.border.bold,
        scrimColor = when (type) {
            AppBottomSheetType.Modal -> colors.overlay.scrim.copy(alpha = ScrimAlpha)
            AppBottomSheetType.Default -> Color.Transparent
        }
    )
}

@Composable
internal fun AppBottomSheetSurface(
    style: AppBottomSheetStyle,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppSurface(
        modifier = modifier,
        shape = style.shape,
        color = style.colors.containerColor,
        contentColor = style.colors.contentColor
    ) {
        Column(
            modifier = columnModifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            Box(
                modifier = headerModifier
                    .fillMaxWidth()
                    .padding(style.headerPadding),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(style.handleWidth, style.handleHeight)
                        .background(style.colors.handleColor, style.handleShape)
                )
            }
            Column(modifier = Modifier.fillMaxWidth().padding(contentPadding), content = content)
        }
    }
}

@Preview
@Composable
private fun BottomSheetModalPreviewDark() = AppBottomSheetPreview(isLight = false, type = AppBottomSheetType.Modal)

@Preview
@Composable
private fun BottomSheetModalPreviewLight() = AppBottomSheetPreview(isLight = true, type = AppBottomSheetType.Modal)

@Preview
@Composable
private fun BottomSheetDefaultPreviewDark() = AppBottomSheetPreview(isLight = false, type = AppBottomSheetType.Default)

@Preview
@Composable
private fun BottomSheetDefaultPreviewLight() = AppBottomSheetPreview(isLight = true, type = AppBottomSheetType.Default)

@Composable
private fun AppBottomSheetPreview(isLight: Boolean, type: AppBottomSheetType) {
    ProvideAppTheme(isLight = isLight) {
        val style = AppBottomSheetDefaults.style(type)
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                AppText("Screen behind the sheet", modifier = Modifier.padding(16.dp))
                Box(modifier = Modifier.matchParentSize().background(style.colors.scrimColor))
                AppBottomSheetSurface(
                    style = style,
                    contentPadding = AppBottomSheetDefaults.ContentPadding,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText("Sheet title", style = AppTheme.typography.title.mSemiBold)
                        AppText("Content goes in the slot, padded 16 on the sides and 24 at the bottom.", color = AppTheme.colors.text.subtle)
                    }
                }
            }
        }
    }
}
