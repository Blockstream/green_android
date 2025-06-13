package com.blockstream.green.domain.meld

import com.blockstream.green.data.meld.MeldRepository
import com.blockstream.green.data.meld.models.Country
import com.blockstream.green.domain.base.NetworkResultUseCase
import com.blockstream.green.network.NetworkResponse

class GetMeldCountries(
    private val meldRepository: MeldRepository
) : NetworkResultUseCase<Unit, List<Country>>() {

    override suspend fun doWork(params: Unit): NetworkResponse<List<Country>> {
        return meldRepository.getCountries()
    }
}