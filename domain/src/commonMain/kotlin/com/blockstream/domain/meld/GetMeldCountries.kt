package com.blockstream.domain.meld

import com.blockstream.domain.base.NetworkResultUseCase
import com.blockstream.network.NetworkResponse

class GetMeldCountries(
    private val meldRepository: com.blockstream.data.meld.MeldRepository
) : NetworkResultUseCase<Unit, List<com.blockstream.data.meld.models.Country>>() {

    override suspend fun doWork(params: Unit): NetworkResponse<List<com.blockstream.data.meld.models.Country>> {
        return meldRepository.getCountries()
    }
}