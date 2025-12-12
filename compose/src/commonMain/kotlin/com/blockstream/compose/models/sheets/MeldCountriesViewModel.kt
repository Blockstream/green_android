package com.blockstream.compose.models.sheets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.blockstream.common.data.GreenWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.green.data.meld.models.Country
import com.blockstream.green.domain.base.Result
import com.blockstream.green.domain.meld.GetMeldCountries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

class MeldCountriesViewModel(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    private val getMeldCountries: GetMeldCountries by inject()
    
    val uiState: StateFlow<MeldCountriesState> = 
        meldCountriesUiState(getMeldCountries).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MeldCountriesState.Loading
        )

    override fun screenName() = "MeldCountries"
}

private fun meldCountriesUiState(
    getMeldCountries: GetMeldCountries
): Flow<MeldCountriesState> {
    return getMeldCountries(Unit).map { result ->
        when (result) {
            is Result.Success -> MeldCountriesState.Success(result.data)
            is Result.Error -> MeldCountriesState.Error(result.message)
            is Result.Loading -> MeldCountriesState.Loading
        }
    }
}

@Immutable
sealed interface MeldCountriesState {
    data class Success(val countries: List<Country>) : MeldCountriesState
    data object Loading : MeldCountriesState
    data class Error(val error: String) : MeldCountriesState
}