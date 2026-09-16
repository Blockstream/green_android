package com.blockstream.domain.base

import com.blockstream.data.data.DataState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FirstSettledTest {
    private class RefreshUseCase : DataStateObservableUseCase<Unit, Int>() {
        var next: DataState<Int> = DataState.Success(1)
        var canExecute = true
        val releaseObserver = CompletableDeferred<Unit>()

        override fun shouldExecute(params: Unit) = canExecute

        override suspend fun doAsyncWork(params: Unit) = set(next)

        override fun createObservable(params: Unit): Flow<DataState<Int>> = get().onEach {
            // Keep the shared observer behind a completed refresh, without relying on timing.
            if (it != DataState.Success(1)) releaseObserver.await()
        }

        fun publish(state: DataState<Int>) = set(state)
    }

    @Test
    fun completedRefreshDoesNotReturnThePreviousSharedReplay() = runTest {
        val results = listOf(DataState.Success(2), DataState.Empty, DataState.Error(Exception("refresh failed")))
        for (result in results) {
            val useCase = RefreshUseCase()
            try {
                useCase(Unit)
                assertEquals(DataState.Success(1), useCase.observe().first())
                useCase.next = result

                assertEquals(result, useCase.firstSettled())
                assertEquals(result, useCase.getCurrent())
            } finally {
                useCase.releaseObserver.complete(Unit)
            }
        }
    }

    @Test
    fun skippedRefreshReturnsTheCurrentSettledState() = runTest {
        val useCase = RefreshUseCase()
        useCase(Unit)
        useCase.canExecute = false
        useCase.next = DataState.Success(2)

        assertEquals(DataState.Success(1), useCase.firstSettled(Unit))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun skippedRefreshWaitsWhileTheCurrentStateIsLoading() = runTest {
        val useCase = RefreshUseCase()
        useCase.canExecute = false
        val result = async { useCase.firstSettled() }
        runCurrent()
        assertFalse(result.isCompleted)

        useCase.publish(DataState.Empty)

        assertEquals(DataState.Empty, result.await())
    }
}
