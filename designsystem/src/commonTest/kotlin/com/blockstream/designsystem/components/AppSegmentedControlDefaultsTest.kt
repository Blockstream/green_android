package com.blockstream.designsystem.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSegmentedControlDefaultsTest {

    @Test
    fun firstAndLastSegmentsRoundTheOuterCorners() {
        val r = 12.dp
        assertEquals(
            RoundedCornerShape(topStart = r, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = r),
            AppSegmentedControlDefaults.shape(index = 0, count = 3, cornerRadius = r)
        )
        assertEquals(
            RoundedCornerShape(topStart = 0.dp, topEnd = r, bottomEnd = r, bottomStart = 0.dp),
            AppSegmentedControlDefaults.shape(index = 2, count = 3, cornerRadius = r)
        )
        assertEquals(RoundedCornerShape(0.dp), AppSegmentedControlDefaults.shape(index = 1, count = 3, cornerRadius = r))
        assertEquals(RoundedCornerShape(r), AppSegmentedControlDefaults.shape(index = 0, count = 1, cornerRadius = r))
    }
}
