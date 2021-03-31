package com.blockstream.green.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.settings.ApplicationSettings
import com.blockstream.green.lifecycle.ListenableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


class AppSettingsViewModel @AssistedInject constructor(
    private val settingsManager: SettingsManager,
    @Assisted defaultElectrumBackend: String
) : AppViewModel() {
    private var appSettings: ApplicationSettings = settingsManager.getApplicationSettings()

    val proxyURLInvalid = MutableLiveData(true)

    val proxyURL = ListenableLiveData(appSettings.proxyURL ?: "") {
        proxyURLInvalid.postValue(it.isBlank())
    }

    val enableTorRouting = MutableLiveData(appSettings.tor)
    val enableProxy = MutableLiveData<Boolean>(appSettings.proxyURL != null)


    fun getSettings() = ApplicationSettings(
        proxyURL = if (enableProxy.value!! && !proxyURL.value.isNullOrBlank()) proxyURL.value else null,
        tor = enableTorRouting.value!!,
    )

    fun saveSettings(){
        settingsManager.saveApplicationSettings(getSettings())
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(defaultElectrumBackend: String): AppSettingsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            defaultElectrumBackend: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(defaultElectrumBackend) as T
            }
        }
    }
}