package com.blockstream.green.ui.settings

import androidx.lifecycle.MutableLiveData
import com.blockstream.green.TestViewModel
import com.blockstream.green.settings.ApplicationSettings
import com.blockstream.green.settings.SettingsManager
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class AppSettingsViewModelUnitTests : TestViewModel<AppSettingsViewModel>() {

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Before
    override fun setup() {
        super.setup()

        whenever(settingsManager.getApplicationSettings()).thenReturn(ApplicationSettings())
        whenever(settingsManager.getApplicationSettingsLiveData()).thenReturn(MutableLiveData(ApplicationSettings()))
        viewModel = AppSettingsViewModel(settingsManager, mock())
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