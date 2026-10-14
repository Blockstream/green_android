package com.blockstream.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AppTextFieldDefaultsTest {

    @Test
    fun stateFollowsPriority() {
        assertEquals(AppTextFieldState.Disabled, AppTextFieldState.of(enabled = false, focused = true, message = AppTextFieldMessageType.Error))
        assertEquals(AppTextFieldState.Error, AppTextFieldState.of(enabled = true, focused = true, message = AppTextFieldMessageType.Error))
        assertEquals(AppTextFieldState.Warning, AppTextFieldState.of(enabled = true, focused = true, message = AppTextFieldMessageType.Warning))
        assertEquals(AppTextFieldState.Info, AppTextFieldState.of(enabled = true, focused = true, message = AppTextFieldMessageType.Info))
        assertEquals(AppTextFieldState.Focused, AppTextFieldState.of(enabled = true, focused = true, message = null))
        assertEquals(AppTextFieldState.Default, AppTextFieldState.of(enabled = true, focused = false, message = null))
    }
}
