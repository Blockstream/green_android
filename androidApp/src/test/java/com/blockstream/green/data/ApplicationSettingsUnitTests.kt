package com.blockstream.green.data

import com.blockstream.common.data.ApplicationSettings
import com.russhwolf.settings.Settings
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ApplicationSettingsUnitTests {

    @Mock
    private lateinit var settings: Settings

    @Test
    fun test_initial_values_with_empty_prefs(){
        val appSettings = ApplicationSettings.fromSettings(settings)

        Assert.assertFalse(appSettings.tor)
        Assert.assertNull(appSettings.proxyUrl)
    }

    @Test
    fun test_values_from_prefs(){

        whenever(settings.getStringOrNull(any())).thenAnswer {
            // value is same as requested key
            it.arguments[0] as String
        }

//        whenever(settings.getString(any(), anyOrNull())).thenAnswer {
//            // value is same as requested key
//            it.arguments[0] as String
//        }
        
        whenever(settings.getBoolean(any(), any())).thenReturn(true)

        val appSettings = ApplicationSettings.fromSettings(settings)

        Assert.assertEquals("proxyURL", appSettings.proxyUrl)
        Assert.assertTrue(appSettings.tor)
    }
}