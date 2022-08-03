package com.blockstream.green.ui.recovery

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.savedstate.SavedStateRegistryOwner
import com.blockstream.gdk.GdkBridge
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.ui.AppViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mu.KLogging

class RecoveryIntroViewModel @AssistedInject constructor(
    gdkBridge: GdkBridge,
    countly: Countly,
    @Assisted private val savedStateHandle: SavedStateHandle,
    @Assisted val generateMnemonic: Boolean,
) : AppViewModel(countly) {

    var recoverySize = savedStateHandle.getLiveData<Int>(SIZE)

    init {

        if (!savedStateHandle.contains(SIZE)) {
            savedStateHandle[SIZE] = R.id.button12
        }

        recoverySize.distinctUntilChanged().observe(lifecycleOwner) {
            if (generateMnemonic) {
                savedStateHandle[MNEMONIC] =
                    if (recoverySize.value == R.id.button12) gdkBridge.generateMnemonic12() else gdkBridge.generateMnemonic24()
            }
        }
    }

    val mnemonic: String
        get() = savedStateHandle.get<String>(MNEMONIC) ?: ""

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            savedStateHandle: SavedStateHandle,
            generateMnemonic: Boolean
        ): RecoveryIntroViewModel
    }

    companion object : KLogging(){
        const val SIZE = "SIZE"
        const val MNEMONIC = "MNEMONIC"

        fun provideFactory(
            assistedFactory: AssistedFactory,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
            generateMnemonic: Boolean,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    return assistedFactory.create(handle, generateMnemonic) as T
                }
            }
    }
}