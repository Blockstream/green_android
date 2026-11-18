package com.blockstream.designsystem.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.components.AppBottomSheetValue.Expanded
import com.blockstream.designsystem.components.AppBottomSheetValue.Hidden
import com.blockstream.designsystem.components.AppBottomSheetValue.PartiallyExpanded
import kotlin.math.abs
import kotlin.math.max

enum class AppBottomSheetValue {
    Hidden,
    Expanded,
    PartiallyExpanded
}

@Stable
class AppBottomSheetState internal constructor(
    internal val skipPartiallyExpanded: Boolean,
    private val density: Density,
    initialValue: AppBottomSheetValue = Hidden,
    internal val confirmValueChange: (AppBottomSheetValue) -> Boolean = { true }
) {
    init {
        if (skipPartiallyExpanded) {
            require(initialValue != PartiallyExpanded) {
                "The initial value must not be PartiallyExpanded when skipPartiallyExpanded is true."
            }
        }
    }

    internal val anchoredDraggableState = AnchoredDraggableState(initialValue)

    private var valueBeforeDrag: AppBottomSheetValue? by mutableStateOf(null)

    val currentValue: AppBottomSheetValue
        get() = valueBeforeDrag ?: anchoredDraggableState.settledValue

    val targetValue: AppBottomSheetValue
        get() = anchoredDraggableState.targetValue

    val isVisible: Boolean
        get() = currentValue != Hidden

    internal val hasExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasPositionFor(Expanded)

    internal val hasPartiallyExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasPositionFor(PartiallyExpanded)

    internal val draggableState: DraggableState = object : DraggableState {
        private val dragScope = object : DragScope {
            override fun dragBy(pixels: Float) {
                anchoredDraggableState.dispatchRawDelta(pixels)
            }
        }

        override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) {
            if (valueBeforeDrag == null) valueBeforeDrag = anchoredDraggableState.settledValue
            anchoredDraggableState.anchoredDrag(dragPriority) { dragScope.block() }
        }

        override fun dispatchRawDelta(delta: Float) {
            anchoredDraggableState.dispatchRawDelta(delta)
        }
    }

    suspend fun show() {
        val target = if (hasPartiallyExpandedState) PartiallyExpanded else Expanded
        if (confirmValueChange(target)) animateTo(target, AppBottomSheetDefaults.MotionSpec)
    }

    suspend fun hide() {
        if (confirmValueChange(Hidden)) animateTo(Hidden, AppBottomSheetDefaults.MotionSpec)
    }

    internal suspend fun expand() {
        if (confirmValueChange(Expanded)) animateTo(Expanded, AppBottomSheetDefaults.MotionSpec)
    }

    internal suspend fun partialExpand() {
        check(!skipPartiallyExpanded) {
            "Cannot partially expand a sheet created with skipPartiallyExpanded = true."
        }
        if (confirmValueChange(PartiallyExpanded)) animateTo(PartiallyExpanded, AppBottomSheetDefaults.MotionSpec)
    }

    internal suspend fun settle(velocity: Float) {
        val startValue = currentValue
        val target = computeTarget(
            anchors = anchoredDraggableState.anchors,
            offset = anchoredDraggableState.requireOffset(),
            settledValue = startValue,
            velocity = velocity,
            positionalThreshold = with(density) { PositionalThreshold.toPx() },
            velocityThreshold = with(density) { VelocityThreshold.toPx() }
        )
        animateTo(if (confirmValueChange(target)) target else startValue, AppBottomSheetDefaults.MotionSpec, velocity)
    }

    private suspend fun animateTo(
        target: AppBottomSheetValue,
        animationSpec: FiniteAnimationSpec<Float>,
        velocity: Float = anchoredDraggableState.lastVelocity
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = target) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                val start = anchoredDraggableState.offset.takeUnless { it.isNaN() } ?: 0f
                animate(start, targetOffset, velocity, animationSpec) { value, frameVelocity ->
                    dragTo(value, frameVelocity)
                }
            }
        }
        valueBeforeDrag = null
    }

    internal companion object {
        val PositionalThreshold = 56.dp
        val VelocityThreshold = 125.dp

        fun saver(
            skipPartiallyExpanded: Boolean,
            density: Density,
            confirmValueChange: (AppBottomSheetValue) -> Boolean
        ) = Saver<AppBottomSheetState, AppBottomSheetValue>(
            save = { it.currentValue },
            restore = { AppBottomSheetState(skipPartiallyExpanded, density, it, confirmValueChange) }
        )
    }
}

@Composable
fun rememberAppBottomSheetState(
    skipPartiallyExpanded: Boolean = true,
    confirmValueChange: (AppBottomSheetValue) -> Boolean = { true }
): AppBottomSheetState {
    val density = LocalDensity.current
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        saver = AppBottomSheetState.saver(skipPartiallyExpanded, density, confirmValueChange)
    ) {
        AppBottomSheetState(skipPartiallyExpanded, density, Hidden, confirmValueChange)
    }
}

internal suspend fun AppBottomSheetState.dismissIfHiddenAfter(
    onDismissRequest: () -> Unit,
    action: suspend AppBottomSheetState.() -> Unit
) {
    try {
        action()
    } finally {
        if (!isVisible) onDismissRequest()
    }
}

internal fun headerTapTarget(
    currentValue: AppBottomSheetValue,
    hasPartiallyExpandedState: Boolean
): AppBottomSheetValue? {
    if (!hasPartiallyExpandedState) return null
    return when (currentValue) {
        Expanded -> PartiallyExpanded
        PartiallyExpanded -> Expanded
        Hidden -> null
    }
}

internal fun sheetAnchors(
    sheetHeight: Float,
    fullHeight: Float,
    skipPartiallyExpanded: Boolean
): DraggableAnchors<AppBottomSheetValue> = DraggableAnchors {
    Hidden at fullHeight
    if (sheetHeight > fullHeight / 2 && !skipPartiallyExpanded) {
        PartiallyExpanded at fullHeight / 2f
    }
    if (sheetHeight != 0f) {
        Expanded at max(0f, fullHeight - sheetHeight)
    }
}

internal fun newTargetOnAnchorChange(
    oldTarget: AppBottomSheetValue,
    anchors: DraggableAnchors<AppBottomSheetValue>
): AppBottomSheetValue = when (oldTarget) {
    Hidden -> Hidden
    PartiallyExpanded -> when {
        anchors.hasPositionFor(PartiallyExpanded) -> PartiallyExpanded
        anchors.hasPositionFor(Expanded) -> Expanded
        else -> Hidden
    }

    Expanded -> if (anchors.hasPositionFor(Expanded)) Expanded else Hidden
}

internal fun computeTarget(
    anchors: DraggableAnchors<AppBottomSheetValue>,
    offset: Float,
    settledValue: AppBottomSheetValue,
    velocity: Float,
    positionalThreshold: Float,
    velocityThreshold: Float
): AppBottomSheetValue {
    val settledPosition = anchors.positionOf(settledValue)
    val above = anchors.closestAnchor(offset, searchUpwards = false) ?: settledValue
    val below = anchors.closestAnchor(offset, searchUpwards = true) ?: settledValue
    return when {
        settledPosition == offset || settledPosition.isNaN() -> settledValue
        velocity >= velocityThreshold -> below
        velocity <= -velocityThreshold -> above
        settledPosition < offset -> if (offset < abs(settledPosition + positionalThreshold)) settledValue else below
        else -> if (offset > abs(settledPosition - positionalThreshold)) settledValue else above
    }
}

internal fun AppBottomSheetState.nestedScrollConnection(onFling: (velocity: Float) -> Unit): NestedScrollConnection =
    object : NestedScrollConnection {
        private var settledOnPreFling = false

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0 && source == NestedScrollSource.UserInput) {
                Offset(0f, anchoredDraggableState.dispatchRawDelta(available.y))
            } else {
                Offset.Zero
            }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (source == NestedScrollSource.UserInput) {
                Offset(0f, anchoredDraggableState.dispatchRawDelta(available.y))
            } else {
                Offset.Zero
            }

        override suspend fun onPreFling(available: Velocity): Velocity {
            settledOnPreFling = available.y < 0 &&
                anchoredDraggableState.requireOffset() > anchoredDraggableState.anchors.minPosition()
            if (!settledOnPreFling) return Velocity.Zero
            onFling(available.y)
            return available
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (settledOnPreFling) {
                settledOnPreFling = false
            } else {
                onFling(available.y)
            }
            return available
        }
    }
