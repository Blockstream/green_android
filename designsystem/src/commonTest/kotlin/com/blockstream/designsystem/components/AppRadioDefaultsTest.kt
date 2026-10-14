package com.blockstream.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AppRadioDefaultsTest {
    @Test
    fun disabledWinsOverSelected() {
        assertEquals(AppRadioState.Disabled, AppRadioState.of(enabled = false, selected = true))
        assertEquals(AppRadioState.Selected, AppRadioState.of(enabled = true, selected = true))
        assertEquals(AppRadioState.Unselected, AppRadioState.of(enabled = true, selected = false))
    }
}
