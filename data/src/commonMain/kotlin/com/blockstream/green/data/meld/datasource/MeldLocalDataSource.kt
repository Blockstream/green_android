package com.blockstream.green.data.meld.datasource

import com.blockstream.green.data.meld.models.Country

class MeldLocalDataSource {
    private var cachedCountries: List<Country>? = null
    
    fun getCachedCountries(): List<Country>? {
        return cachedCountries
    }
    
    fun saveCountries(countries: List<Country>) {
        cachedCountries = countries
    }
    

}