package com.blockstream.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSwitchDefaultsTest {

    @Test
    fun disabledWinsOverChecked() {
        assertEquals(AppSwitchState.Disabled, AppSwitchState.of(enabled = false, checked = true))
        assertEquals(AppSwitchState.On, AppSwitchState.of(enabled = true, checked = true))
        assertEquals(AppSwitchState.Off, AppSwitchState.of(enabled = true, checked = false))
    }
}
