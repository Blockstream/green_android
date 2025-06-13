package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_loading
import com.blockstream.common.models.sheets.MeldCountriesState
import com.blockstream.common.models.sheets.MeldCountriesViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.CountriesList
import com.blockstream.compose.components.CountryItem
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeldCountriesBottomSheet(
    viewModel: MeldCountriesViewModel,
    title: String? = null,
    subtitle: String? = null,
    onDismissRequest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GreenBottomSheet(
        title = title,
        subtitle = subtitle,
        viewModel = viewModel,
        withHorizontalPadding = false,
        withBottomPadding = false,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        ),
        onDismissRequest = onDismissRequest
    ) {
        when (val state = uiState) {
            is MeldCountriesState.Loading -> LoadingOrErrorContent(stringResource(Res.string.id_loading))
            is MeldCountriesState.Error -> LoadingOrErrorContent(state.error)
            is MeldCountriesState.Success -> {
                var query by remember { mutableStateOf("") }
                
                val filteredCountries = remember(state.countries, query) {
                    if (query.isBlank()) {
                        state.countries
                    } else {
                        state.countries.filter {
                            it.name.lowercase().contains(query.lowercase()) || 
                            it.countryCode.lowercase().contains(query.lowercase())
                        }
                    }
                }

                CountriesList(
                    countries = filteredCountries,
                    query = query,
                    onQueryChange = { query = it },
                    onCountryClick = { country ->
                        NavigateDestinations.MeldCountries.setResult(country)
                        onDismissRequest()
                    },
                    itemContent = { country ->
                        CountryItem(
                            name = country.name,
                            code = country.countryCode,
                            flagText = country.flagEmoji
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun LoadingOrErrorContent(message: String) {
    Text(
        text = message,
        style = bodyMedium,
        textAlign = TextAlign.Center,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).padding(horizontal = 16.dp)
    )
}
