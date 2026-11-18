package com.blockstream.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.blockstream.designsystem.components.AppBottomSheetValue.Expanded
import com.blockstream.designsystem.components.AppBottomSheetValue.Hidden
import com.blockstream.designsystem.components.AppBottomSheetValue.PartiallyExpanded
import com.blockstream.designsystem.generated.resources.Res
import com.blockstream.designsystem.generated.resources.bottomsheet_close
import com.blockstream.designsystem.generated.resources.bottomsheet_collapse
import com.blockstream.designsystem.generated.resources.bottomsheet_dismiss
import com.blockstream.designsystem.generated.resources.bottomsheet_drag_handle
import com.blockstream.designsystem.generated.resources.bottomsheet_expand
import com.blockstream.designsystem.generated.resources.bottomsheet_pane_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun AppBottomSheetLayout(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetState: AppBottomSheetState,
    style: AppBottomSheetStyle,
    contentPadding: PaddingValues,
    properties: AppBottomSheetProperties,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
    val animateToDismiss: () -> Unit = {
        scope.launch { sheetState.dismissIfHiddenAfter(onDismissRequest) { hide() } }
    }
    val settleToDismiss: (velocity: Float) -> Unit = { velocity ->
        scope.launch { sheetState.dismissIfHiddenAfter(onDismissRequest) { settle(velocity) } }
    }
    val currentSettleToDismiss by rememberUpdatedState(settleToDismiss)
    val onBackRequest: () -> Unit = {
        if (sheetState.currentValue == Expanded && sheetState.hasPartiallyExpandedState) {
            scope.launch { predictiveBackProgress.animateTo(0f) }
            scope.launch { sheetState.partialExpand() }
        } else {
            scope.launch { sheetState.dismissIfHiddenAfter(onDismissRequest) { hide() } }.invokeOnCompletion {
                if (sheetState.isVisible) scope.launch { predictiveBackProgress.animateTo(0f) }
            }
        }
    }

    AppBottomSheetWindow(
        onDismissRequest = onBackRequest,
        properties = properties,
        contentColor = style.colors.contentColor,
        predictiveBackProgress = predictiveBackProgress
    ) {
        val paneTitleLabel = stringResource(Res.string.bottomsheet_pane_title)
        val dragHandleLabel = stringResource(Res.string.bottomsheet_drag_handle)
        val dismissLabel = stringResource(Res.string.bottomsheet_dismiss)
        val expandLabel = stringResource(Res.string.bottomsheet_expand)
        val collapseLabel = stringResource(Res.string.bottomsheet_collapse)
        val tapTarget = headerTapTarget(sheetState.currentValue, sheetState.hasPartiallyExpandedState)

        Box(modifier = Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup = true }) {
            AppBottomSheetScrim(
                color = style.colors.scrimColor,
                visible = sheetState.targetValue != Hidden,
                dismissEnabled = properties.shouldDismissOnClickOutside,
                onDismissRequest = animateToDismiss
            )
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                AppBottomSheetSurface(
                    style = style,
                    contentPadding = contentPadding,
                    modifier = modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = style.maxWidth)
                        .fillMaxWidth()
                        .nestedScroll(remember(sheetState) { sheetState.nestedScrollConnection(onFling = { currentSettleToDismiss(it) }) })
                        .draggableAnchors(sheetState.anchoredDraggableState) { sheetSize, constraints ->
                            val anchors = sheetAnchors(
                                sheetHeight = sheetSize.height.toFloat(),
                                fullHeight = constraints.maxHeight.toFloat(),
                                skipPartiallyExpanded = sheetState.skipPartiallyExpanded
                            )
                            anchors to newTargetOnAnchorChange(sheetState.anchoredDraggableState.targetValue, anchors)
                        }
                        .draggable(
                            state = sheetState.draggableState,
                            orientation = Orientation.Vertical,
                            enabled = sheetState.isVisible,
                            startDragImmediately = sheetState.anchoredDraggableState.isAnimationRunning,
                            onDragStopped = { settleToDismiss(it) }
                        )
                        .semantics {
                            paneTitle = paneTitleLabel
                            traversalIndex = 0f
                        }
                        .graphicsLayer {
                            val progress = predictiveBackProgress.value
                            scaleX = predictiveBackScaleX(progress)
                            scaleY = predictiveBackScaleY(progress)
                            transformOrigin = PredictiveBackSheetTransformOrigin
                        },
                    columnModifier = Modifier.graphicsLayer {
                        val progress = predictiveBackProgress.value
                        val backScaleX = predictiveBackScaleX(progress)
                        val backScaleY = predictiveBackScaleY(progress)
                        scaleY = if (backScaleY != 0f) backScaleX / backScaleY else 1f
                        transformOrigin = PredictiveBackChildTransformOrigin
                    },
                    headerModifier = Modifier
                        .then(
                            when (tapTarget) {
                                Expanded -> Modifier.clickable(interactionSource = null, indication = null, onClickLabel = expandLabel) {
                                    scope.launch { sheetState.expand() }
                                }

                                PartiallyExpanded -> Modifier.clickable(interactionSource = null, indication = null, onClickLabel = collapseLabel) {
                                    scope.launch { sheetState.partialExpand() }
                                }

                                Hidden, null -> Modifier
                            }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = dragHandleLabel
                            dismiss(dismissLabel) {
                                animateToDismiss()
                                true
                            }
                            when (tapTarget) {
                                Expanded -> expand(expandLabel) {
                                    scope.launch { sheetState.expand() }
                                    true
                                }

                                PartiallyExpanded -> collapse(collapseLabel) {
                                    scope.launch { sheetState.partialExpand() }
                                    true
                                }

                                Hidden, null -> Unit
                            }
                        },
                    content = content
                )
            }
        }
    }

    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

@Composable
internal expect fun AppBottomSheetWindow(
    onDismissRequest: () -> Unit,
    properties: AppBottomSheetProperties,
    contentColor: Color,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit
)

@Composable
private fun AppBottomSheetScrim(
    color: Color,
    visible: Boolean,
    dismissEnabled: Boolean,
    onDismissRequest: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AppBottomSheetDefaults.MotionSpec
    )
    val closeLabel = stringResource(Res.string.bottomsheet_close)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val dismissModifier = if (dismissEnabled) {
        Modifier
            .pointerInput(Unit) { detectTapGestures { currentOnDismissRequest() } }
            .semantics(mergeDescendants = true) {
                traversalIndex = 1f
                contentDescription = closeLabel
                onClick {
                    currentOnDismissRequest()
                    true
                }
            }
    } else {
        Modifier
    }
    Canvas(modifier = Modifier.fillMaxSize().then(dismissModifier)) {
        drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
    }
}

private fun Modifier.draggableAnchors(
    state: AnchoredDraggableState<AppBottomSheetValue>,
    anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<AppBottomSheetValue>, AppBottomSheetValue>
): Modifier = this then DraggableAnchorsElement(state, anchors)

private class DraggableAnchorsElement(
    private val state: AnchoredDraggableState<AppBottomSheetValue>,
    private val anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<AppBottomSheetValue>, AppBottomSheetValue>
) : ModifierNodeElement<DraggableAnchorsNode>() {
    override fun create() = DraggableAnchorsNode(state, anchors)

    override fun update(node: DraggableAnchorsNode) {
        node.state = state
        node.anchors = anchors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DraggableAnchorsElement) return false
        return state == other.state && anchors === other.anchors
    }

    override fun hashCode(): Int = 31 * state.hashCode() + anchors.hashCode()
}

private class DraggableAnchorsNode(
    var state: AnchoredDraggableState<AppBottomSheetValue>,
    var anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<AppBottomSheetValue>, AppBottomSheetValue>
) : Modifier.Node(), LayoutModifierNode {
    private var didLookahead: Boolean = false

    override fun onDetach() {
        didLookahead = false
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        if (!isLookingAhead || !didLookahead) {
            val (newAnchors, newTarget) = anchors(IntSize(placeable.width, placeable.height), constraints)
            state.updateAnchors(newAnchors, newTarget)
        }
        didLookahead = isLookingAhead || didLookahead
        return layout(placeable.width, placeable.height) {
            val offset = if (isLookingAhead) state.anchors.positionOf(state.targetValue) else state.requireOffset()
            placeable.place(0, offset.roundToInt())
        }
    }
}

private fun GraphicsLayerScope.predictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress) / width
    }
}

private fun GraphicsLayerScope.predictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

private val PredictiveBackMaxScaleXDistance = 48.dp
private val PredictiveBackMaxScaleYDistance = 24.dp
private val PredictiveBackSheetTransformOrigin = TransformOrigin(0.5f, 1f)
private val PredictiveBackChildTransformOrigin = TransformOrigin(0.5f, 0f)
