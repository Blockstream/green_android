package com.blockstream.green.data.meld.datasource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeldLocalDataSourceTest {

    @Test
    fun `test cache operations`() {
        val dataSource = _root_ide_package_.com.blockstream.data.meld.datasource.MeldLocalDataSource()
        val mockCountries = listOf(
            _root_ide_package_.com.blockstream.data.meld.models.Country("US", "United States", emptyList(), "https://example.com/us.png"),
            _root_ide_package_.com.blockstream.data.meld.models.Country("CA", "Canada", emptyList(), "https://example.com/ca.png")
        )

        assertNull(dataSource.getCachedCountries())

        dataSource.saveCountries(mockCountries)
        assertEquals(mockCountries, dataSource.getCachedCountries())
    }
}