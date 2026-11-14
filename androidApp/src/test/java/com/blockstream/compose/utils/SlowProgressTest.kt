package com.blockstream.compose.utils

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SlowProgressTest {
    private class TestViewModel : ViewModel()

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun slowMessageAppearsAfterThresholdAndResetsForNextAttempt() = runTest(dispatcher) {
        val viewModel = TestViewModel()
        val active = MutableStateFlow(false)
        val slow = viewModel.observeIsSlow(active, threshold = 15.seconds)

        runCurrent()
        active.value = true
        runCurrent()

        advanceTimeBy(14_999)
        runCurrent()
        assertFalse(slow.value)

        advanceTimeBy(1)
        runCurrent()
        assertTrue(slow.value)

        active.value = false
        runCurrent()
        assertFalse(slow.value)

        active.value = true
        runCurrent()
        advanceTimeBy(14_999)
        runCurrent()
        assertFalse(slow.value)
    }
}
