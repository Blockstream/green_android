package com.blockstream.common.models.about

import app.cash.turbine.test
import com.blockstream.common.models.TestViewModel
import com.blockstream.common.sideeffects.SideEffects
import kotlinx.coroutines.launch
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
        // Delay them as .test is suspended
        // this won't be needed when Channel is used in GreenViewModel
        launch {
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickWebsite)
            viewModel.postEvent(AboutViewModel.LocalEvents.ClickFeedback)
        }

        viewModel.sideEffect.test {
            assertTrue { awaitItem() is SideEffects.OpenBrowser }
            assertTrue { awaitItem() is SideEffects.NavigateTo }
        }
    }
}