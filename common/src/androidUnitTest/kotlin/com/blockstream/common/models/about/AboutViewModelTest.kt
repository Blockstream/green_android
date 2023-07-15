package com.blockstream.common.models.about

import app.cash.turbine.test
import com.blockstream.common.models.TestViewModel
import com.blockstream.common.sideeffects.SideEffects
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AboutViewModelTest : TestViewModel<AboutViewModel>() {

    override fun setup() {
        viewModel = AboutViewModel()
    }

    @Test
    fun `Test year and version`() = runTest {
        assertEquals(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year.toString(),
            viewModel.year
        )
    }

    @Test
    fun `Test click events`() = runTest {
        viewModel.postEvent(AboutViewModel.LocalEvents.ClickWebsite())
        viewModel.sideEffect.test {
            assertTrue { awaitItem() is SideEffects.OpenBrowser }
        }

        viewModel.postEvent(AboutViewModel.LocalEvents.ClickFeedback())
        viewModel.sideEffect.test {
            assertTrue { awaitItem() is SideEffects.OpenDialog }
        }
    }
}