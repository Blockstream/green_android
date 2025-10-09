package com.blockstream.green.data.meld.datasource

import com.blockstream.green.data.meld.models.Country
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeldLocalDataSourceTest {

    @Test
    fun `test cache operations`() {
        val dataSource = MeldLocalDataSource()
        val mockCountries = listOf(
            Country("US", "United States", emptyList(), "https://example.com/us.png"),
            Country("CA", "Canada", emptyList(), "https://example.com/ca.png")
        )

        assertNull(dataSource.getCachedCountries())

        dataSource.saveCountries(mockCountries)
        assertEquals(mockCountries, dataSource.getCachedCountries())
    }
}