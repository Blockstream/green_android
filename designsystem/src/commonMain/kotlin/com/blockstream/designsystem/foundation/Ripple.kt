package com.blockstream.designsystem.foundation

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.unit.Dp
import com.blockstream.designsystem.LocalAppContentColor

@Stable
fun appRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified
): IndicationNodeFactory {
    return if (radius == Dp.Unspecified && color == Color.Unspecified) {
        if (bounded) DefaultBoundedRipple else DefaultUnboundedRipple
    } else {
        RippleNodeFactory(bounded, radius, color)
    }
}

@Stable
fun appRipple(
    color: ColorProducer,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified
): IndicationNodeFactory = RippleNodeFactory(bounded, radius, color)

object AppRippleDefaults {
    val RippleAlpha: RippleAlpha = RippleAlpha(
        pressedAlpha = StateTokens.PressedStateLayerOpacity,
        focusedAlpha = StateTokens.FocusStateLayerOpacity,
        draggedAlpha = StateTokens.DraggedStateLayerOpacity,
        hoveredAlpha = StateTokens.HoverStateLayerOpacity
    )
}

val LocalAppRippleConfiguration: ProvidableCompositionLocal<AppRippleConfiguration?> = compositionLocalOf { AppRippleConfiguration() }

@Immutable
class AppRippleConfiguration(
    val color: Color = Color.Unspecified,
    val rippleAlpha: RippleAlpha? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppRippleConfiguration) return false
        return color == other.color && rippleAlpha == other.rippleAlpha
    }

    override fun hashCode(): Int = 31 * color.hashCode() + (rippleAlpha?.hashCode() ?: 0)

    override fun toString(): String = "AppRippleConfiguration(color=$color, rippleAlpha=$rippleAlpha)"
}

@Stable
private class RippleNodeFactory private constructor(
    private val bounded: Boolean,
    private val radius: Dp,
    private val colorProducer: ColorProducer?,
    private val color: Color
) : IndicationNodeFactory {
    constructor(bounded: Boolean, radius: Dp, colorProducer: ColorProducer) : this(bounded, radius, colorProducer, Color.Unspecified)

    constructor(bounded: Boolean, radius: Dp, color: Color) : this(bounded, radius, null, color)

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        val colorProducer = colorProducer ?: ColorProducer { color }
        return DelegatingThemeAwareRippleNode(interactionSource, bounded, radius, colorProducer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleNodeFactory) return false
        return bounded == other.bounded && radius == other.radius && colorProducer == other.colorProducer && color == other.color
    }

    override fun hashCode(): Int {
        var result = bounded.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + colorProducer.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}

private class DelegatingThemeAwareRippleNode(
    private val interactionSource: InteractionSource,
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: ColorProducer
) : DelegatingNode(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var rippleNode: DelegatableNode? = null

    override fun onAttach() {
        updateConfiguration()
    }

    override fun onObservedReadsChanged() {
        updateConfiguration()
    }

    private fun updateConfiguration() {
        observeReads {
            val configuration = currentValueOf(LocalAppRippleConfiguration)
            if (configuration == null) {
                removeRipple()
            } else if (rippleNode == null) {
                attachNewRipple()
            }
        }
    }

    private fun attachNewRipple() {
        val calculateColor = ColorProducer {
            val userDefinedColor = color()
            if (userDefinedColor.isSpecified) {
                userDefinedColor
            } else {
                val configuration = currentValueOf(LocalAppRippleConfiguration)
                if (configuration?.color?.isSpecified == true) configuration.color else currentValueOf(LocalAppContentColor)
            }
        }

        val calculateRippleAlpha = {
            currentValueOf(LocalAppRippleConfiguration)?.rippleAlpha ?: AppRippleDefaults.RippleAlpha
        }

        rippleNode = delegate(createRippleModifierNode(interactionSource, bounded, radius, calculateColor, calculateRippleAlpha))
    }

    private fun removeRipple() {
        rippleNode?.let { undelegate(it) }
        rippleNode = null
    }
}

private object StateTokens {
    const val DraggedStateLayerOpacity = 0.16f
    const val FocusStateLayerOpacity = 0.1f
    const val HoverStateLayerOpacity = 0.08f
    const val PressedStateLayerOpacity = 0.1f
}

private val DefaultBoundedRipple = RippleNodeFactory(bounded = true, radius = Dp.Unspecified, color = Color.Unspecified)
private val DefaultUnboundedRipple = RippleNodeFactory(bounded = false, radius = Dp.Unspecified, color = Color.Unspecified)
