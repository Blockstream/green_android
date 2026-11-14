package com.blockstream.compose.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Flips to true if [isActive] stays true for longer than [threshold], and resets to false
 * as soon as [isActive] becomes false again (success, error or cancellation), restarting the
 * wait on every subsequent retry.
 */
fun ViewModel.observeIsSlow(
    isActive: StateFlow<Boolean>,
    threshold: Duration = 15.seconds
): StateFlow<Boolean> {
    val isSlowLogin = MutableStateFlow(false)

    viewModelScope.launch {
        isActive.collectLatest { active ->
            isSlowLogin.value = false
            if (active) {
                delay(threshold)
                isSlowLogin.value = true
            }
        }
    }

    return isSlowLogin.asStateFlow()
}
