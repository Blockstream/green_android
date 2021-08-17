package com.blockstream.green.ui.settings

import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.TestViewModel
import com.blockstream.green.settings.ApplicationSettings
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AppSettingsViewModelUnitTests : TestViewModel<AppSettingsViewModel>() {

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        whenever(settingsManager.getApplicationSettings()).thenReturn(ApplicationSettings())
        viewModel = AppSettingsViewModel(settingsManager, "DEFAULT")
    }

    @Test
    fun test_initial_values() {
        val s = viewModel.getSettings()

        assertNull(s.proxyUrl)
        assertFalse(s.tor)
    }

    @Test
    fun test_tor_routing() {
        viewModel.enableTorRouting.value = true
        assertTrue(viewModel.getSettings().tor)
    }

    @Test
    fun proxy_shouldBe_empty() {
        viewModel.enableProxy.value = true
        assertTrue(viewModel.getSettings().proxyUrl.isNullOrEmpty())
    }

    @Test
    fun proxy_shouldBeEmpty_ifIsNotEnabled() {
        viewModel.proxyURL.value = "proxyURL"
        assertTrue(viewModel.getSettings().proxyUrl.isNullOrEmpty())
    }

    @Test
    fun proxy_should_haveValue() {
        viewModel.proxyURL.value = "proxyURL"
        viewModel.enableProxy.value = true
        assertTrue(viewModel.getSettings().proxyUrl!!.isNotEmpty())
    }

    @Test
    fun should_save_settings() {
        viewModel.saveSettings()
        verify(settingsManager).saveApplicationSettings(any())
    }
}