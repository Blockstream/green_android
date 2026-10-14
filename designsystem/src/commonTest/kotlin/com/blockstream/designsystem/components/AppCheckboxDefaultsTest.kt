package com.blockstream.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AppCheckboxDefaultsTest {

    @Test
    fun disabledWinsOverChecked() {
        assertEquals(AppCheckboxState.Disabled, AppCheckboxState.of(enabled = false, checked = true))
        assertEquals(AppCheckboxState.Checked, AppCheckboxState.of(enabled = true, checked = true))
        assertEquals(AppCheckboxState.Unchecked, AppCheckboxState.of(enabled = true, checked = false))
    }
}
