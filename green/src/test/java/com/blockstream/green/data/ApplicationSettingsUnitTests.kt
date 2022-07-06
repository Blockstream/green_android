package com.blockstream.green.data

import android.content.SharedPreferences
import com.blockstream.green.settings.ApplicationSettings
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ApplicationSettingsUnitTests {

    @Mock
    private lateinit var prefs: SharedPreferences

    @Test
    fun test_initial_values_with_empty_prefs(){
        val appSettings = ApplicationSettings.fromSharedPreferences(prefs)

        Assert.assertFalse(appSettings.tor)
        Assert.assertNull(appSettings.proxyUrl)
    }

    @Test
    fun test_values_from_prefs(){
        whenever(prefs.getString(any(), anyOrNull())).thenAnswer {
            // value is same as requested key
            it.arguments[0] as String
        }
        whenever(prefs.getBoolean(any(), any())).thenReturn(true)

        val appSettings = ApplicationSettings.fromSharedPreferences(prefs)

        Assert.assertEquals("proxyURL", appSettings.proxyUrl)
        Assert.assertTrue(appSettings.tor)
    }
}