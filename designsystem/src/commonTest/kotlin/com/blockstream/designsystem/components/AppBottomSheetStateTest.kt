package com.blockstream.designsystem.components

import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import com.blockstream.designsystem.components.AppBottomSheetValue.Expanded
import com.blockstream.designsystem.components.AppBottomSheetValue.Hidden
import com.blockstream.designsystem.components.AppBottomSheetValue.PartiallyExpanded
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppBottomSheetStateTest {

    private val fullAnchors = DraggableAnchors {
        Hidden at 1000f
        Expanded at 0f
    }

    private val threeAnchors = DraggableAnchors {
        Hidden at 1000f
        PartiallyExpanded at 500f
        Expanded at 0f
    }

    private fun expandedSheet(confirmValueChange: (AppBottomSheetValue) -> Boolean = { true }): AppBottomSheetState {
        val state = AppBottomSheetState(skipPartiallyExpanded = true, density = Density(1f), confirmValueChange = confirmValueChange)
        state.anchoredDraggableState.updateAnchors(fullAnchors, Expanded)
        return state
    }

    private fun halfHeightSheet(
        initialValue: AppBottomSheetValue,
        confirmValueChange: (AppBottomSheetValue) -> Boolean = { true }
    ): AppBottomSheetState {
        val state = AppBottomSheetState(
            skipPartiallyExpanded = false,
            density = Density(1f),
            initialValue = initialValue,
            confirmValueChange = confirmValueChange
        )
        state.anchoredDraggableState.updateAnchors(threeAnchors, initialValue)
        return state
    }

    private fun target(offset: Float, settled: AppBottomSheetValue, velocity: Float = 0f, anchors: DraggableAnchors<AppBottomSheetValue> = fullAnchors) =
        computeTarget(anchors, offset, settled, velocity, positionalThreshold = 56f, velocityThreshold = 125f)

    @Test
    fun valuesStaySettledWhileDraggingPastHalfway() {
        val state = expandedSheet()

        state.anchoredDraggableState.dispatchRawDelta(600f)

        assertEquals(Hidden, state.anchoredDraggableState.currentValue)
        assertEquals(Expanded, state.currentValue)
        assertTrue(state.isVisible)
    }

    @Test
    fun expandedStaysExpandedWhenAnchorsChange() {
        val anchors = sheetAnchors(sheetHeight = 800f, fullHeight = 1000f, skipPartiallyExpanded = false)

        assertTrue(anchors.hasPositionFor(PartiallyExpanded))
        assertEquals(Expanded, newTargetOnAnchorChange(Expanded, anchors))
    }

    @Test
    fun anchorChangeFallsBackWhenTargetAnchorIsGone() {
        val shortSheet = sheetAnchors(sheetHeight = 300f, fullHeight = 1000f, skipPartiallyExpanded = false)
        val emptySheet = sheetAnchors(sheetHeight = 0f, fullHeight = 1000f, skipPartiallyExpanded = false)

        assertEquals(Expanded, newTargetOnAnchorChange(PartiallyExpanded, shortSheet))
        assertEquals(Hidden, newTargetOnAnchorChange(Expanded, emptySheet))
        assertEquals(Hidden, newTargetOnAnchorChange(Hidden, shortSheet))
    }

    @Test
    fun anchorsFollowSheetHeight() {
        val tall = sheetAnchors(sheetHeight = 800f, fullHeight = 1000f, skipPartiallyExpanded = false)
        assertEquals(1000f, tall.positionOf(Hidden))
        assertEquals(500f, tall.positionOf(PartiallyExpanded))
        assertEquals(200f, tall.positionOf(Expanded))

        val skipped = sheetAnchors(sheetHeight = 800f, fullHeight = 1000f, skipPartiallyExpanded = true)
        assertFalse(skipped.hasPositionFor(PartiallyExpanded))

        val overflowing = sheetAnchors(sheetHeight = 1200f, fullHeight = 1000f, skipPartiallyExpanded = true)
        assertEquals(0f, overflowing.positionOf(Expanded))
    }

    @Test
    fun computeTargetUsesPositionalThresholdDownwards() {
        assertEquals(Expanded, target(offset = 0f, settled = Expanded))
        assertEquals(Expanded, target(offset = 55f, settled = Expanded))
        assertEquals(Hidden, target(offset = 57f, settled = Expanded))
        assertEquals(PartiallyExpanded, target(offset = 57f, settled = Expanded, anchors = threeAnchors))
    }

    @Test
    fun computeTargetUsesPositionalThresholdUpwards() {
        assertEquals(Hidden, target(offset = 945f, settled = Hidden))
        assertEquals(Expanded, target(offset = 943f, settled = Hidden))
        assertEquals(PartiallyExpanded, target(offset = 943f, settled = Hidden, anchors = threeAnchors))
    }

    @Test
    fun computeTargetUsesVelocityThreshold() {
        assertEquals(Expanded, target(offset = 10f, settled = Expanded, velocity = 124f))
        assertEquals(Hidden, target(offset = 10f, settled = Expanded, velocity = 125f))
        assertEquals(Hidden, target(offset = 990f, settled = Hidden, velocity = -124f))
        assertEquals(Expanded, target(offset = 990f, settled = Hidden, velocity = -125f))
    }

    @Test
    fun computeTargetFollowsAFlingBackAgainstTheDrag() {
        assertEquals(Hidden, target(offset = 300f, settled = Expanded, velocity = -124f))
        assertEquals(Expanded, target(offset = 300f, settled = Expanded, velocity = -125f))
        assertEquals(Hidden, target(offset = 700f, settled = Hidden, velocity = 125f))
        assertEquals(PartiallyExpanded, target(offset = 700f, settled = Expanded, velocity = -125f, anchors = threeAnchors))
    }

    @Test
    fun quickFlingUpOnContentExpandsTheSheet() = runTest {
        val sheet = halfHeightSheet(PartiallyExpanded)
        val content = sheet.nestedScrollConnection(onFling = { launch(TestFrameClock()) { sheet.settle(it) } })

        content.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)
        val consumed = content.onPreFling(Velocity(0f, -1000f))
        content.onPostFling(consumed, Velocity.Zero)
        advanceUntilIdle()

        assertEquals(Expanded, sheet.currentValue)
    }

    @Test
    fun slowDragOnContentSettlesAfterTheContent() = runTest {
        val sheet = expandedSheet()
        val content = sheet.nestedScrollConnection(onFling = { launch(TestFrameClock()) { sheet.settle(it) } })

        content.onPostScroll(Offset.Zero, Offset(0f, 600f), NestedScrollSource.UserInput)
        val consumed = content.onPreFling(Velocity.Zero)
        content.onPostFling(consumed, Velocity.Zero)
        advanceUntilIdle()

        assertEquals(Hidden, sheet.currentValue)
    }

    @Test
    fun hideRespectsVeto() = runTest {
        val allowed = expandedSheet()
        val vetoed = expandedSheet(confirmValueChange = { it != Hidden })

        withContext(TestFrameClock()) {
            allowed.hide()
            vetoed.hide()
        }

        assertFalse(allowed.isVisible)
        assertTrue(vetoed.isVisible)
    }

    @Test
    fun settleRespectsVeto() = runTest {
        val allowed = expandedSheet()
        val vetoed = expandedSheet(confirmValueChange = { it != Hidden })
        allowed.anchoredDraggableState.dispatchRawDelta(900f)
        vetoed.anchoredDraggableState.dispatchRawDelta(900f)

        withContext(TestFrameClock()) {
            allowed.settle(0f)
            vetoed.settle(0f)
        }

        assertEquals(Hidden, allowed.currentValue)
        assertEquals(Expanded, vetoed.currentValue)
        assertEquals(0f, vetoed.anchoredDraggableState.offset)
    }

    @Test
    fun settleRespectsVetoAfterDragToTheBottomEdge() = runTest {
        var allowedDismissals = 0
        var vetoedDismissals = 0
        val allowed = expandedSheet()
        val vetoed = expandedSheet(confirmValueChange = { it != Hidden })

        allowed.draggableState.drag { dragBy(1000f) }
        vetoed.draggableState.drag { dragBy(1000f) }

        assertEquals(Hidden, vetoed.anchoredDraggableState.settledValue)
        assertTrue(vetoed.isVisible)

        withContext(TestFrameClock()) {
            allowed.dismissIfHiddenAfter({ allowedDismissals++ }) { settle(0f) }
            vetoed.dismissIfHiddenAfter({ vetoedDismissals++ }) { settle(0f) }
        }

        assertEquals(Hidden, allowed.currentValue)
        assertEquals(1, allowedDismissals)
        assertEquals(Expanded, vetoed.currentValue)
        assertEquals(0f, vetoed.anchoredDraggableState.offset)
        assertEquals(0, vetoedDismissals)
    }

    @Test
    fun headerTapTogglesBetweenHeights() {
        assertEquals(PartiallyExpanded, headerTapTarget(Expanded, hasPartiallyExpandedState = true))
        assertEquals(Expanded, headerTapTarget(PartiallyExpanded, hasPartiallyExpandedState = true))
    }

    @Test
    fun headerTapNeverDismisses() {
        assertNull(headerTapTarget(Expanded, hasPartiallyExpandedState = false))
        assertNull(headerTapTarget(Hidden, hasPartiallyExpandedState = true))
    }

    @Test
    fun headerActionsRespectVeto() = runTest {
        val allowed = halfHeightSheet(Expanded)
        val vetoed = halfHeightSheet(Expanded) { it != PartiallyExpanded }
        val vetoedFromHalf = halfHeightSheet(PartiallyExpanded) { it != Expanded }

        withContext(TestFrameClock()) {
            allowed.partialExpand()
            vetoed.partialExpand()
            vetoedFromHalf.expand()
        }

        assertEquals(PartiallyExpanded, allowed.currentValue)
        assertEquals(Expanded, vetoed.currentValue)
        assertEquals(PartiallyExpanded, vetoedFromHalf.currentValue)
    }

    @Test
    fun vetoedHideDoesNotDismiss() = runTest {
        var allowedDismissals = 0
        var vetoedDismissals = 0
        val allowed = expandedSheet()
        val vetoed = expandedSheet(confirmValueChange = { it != Hidden })

        withContext(TestFrameClock()) {
            allowed.dismissIfHiddenAfter({ allowedDismissals++ }) { hide() }
            vetoed.dismissIfHiddenAfter({ vetoedDismissals++ }) { hide() }
        }

        assertEquals(1, allowedDismissals)
        assertEquals(0, vetoedDismissals)
    }

    private class TestFrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(16)
            frameTimeNanos += 16_000_000L
            return onFrame(frameTimeNanos)
        }
    }
}
