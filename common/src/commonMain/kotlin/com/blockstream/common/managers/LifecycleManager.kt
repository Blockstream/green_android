package com.blockstream.common.managers

import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.di.ApplicationScope
import com.blockstream.green.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class LifecycleState {
    Foreground, Background;

    fun isForeground(): Boolean {
        return this == Foreground
    }

    fun isBackground(): Boolean {
        return this == Background
    }
}

class LifecycleManager constructor(
    val settingsManager: SettingsManager,
    val keystore: GreenKeystore,
    val scope: ApplicationScope
) {
    private val _lifecycleState = MutableStateFlow(LifecycleState.Background)

    @NativeCoroutinesState
    val lifecycleState = _lifecycleState.asStateFlow()

    private val _isLocked = MutableStateFlow(canLock())

    @NativeCoroutinesState
    val isLocked = _isLocked.asStateFlow()

    private var lockJob: Job? = null

    init {
        lifecycleState.onEach {
            lockJob?.cancel()

            if (canLock() && it == LifecycleState.Background) {
                val lockAfterSeconds = settingsManager.getApplicationSettings().screenLockInSeconds
                if (lockAfterSeconds > 0) {

                    lockJob = scope.launch {
                        delay(lockAfterSeconds * 1000L)
                        _isLocked.value = true
                    }
                } else {
                    _isLocked.value = true
                }
            }
        }.launchIn(scope)
    }

    private fun canLock() =
        settingsManager.getApplicationSettings().enhancedPrivacy && keystore.canUseBiometrics()

    fun unlock() {
        _isLocked.value = false
    }

    fun updateState(isOnForeground: Boolean) {
        logger.d { "updateState (isOnForeground:$isOnForeground)" }
        _lifecycleState.value =
            if (isOnForeground) LifecycleState.Foreground else LifecycleState.Background
    }

    companion object : Loggable()
}