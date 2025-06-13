package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.CountriesList
import com.blockstream.compose.components.CountryItem
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.green.data.countries.Countries
import com.blockstream.ui.navigation.setResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesBottomSheet(
    viewModel: GreenViewModel,
    title: String? = null,
    subtitle: String? = null,
    showDialCode: Boolean = true,
    onDismissRequest: () -> Unit,
) {
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
        var query by remember { mutableStateOf("") }
        val data by remember {
            derivedStateOf {
                if (query.isBlank()) {
                    Countries.Countries
                } else {
                    Countries.Countries.filter {
                        it.name.lowercase()
                            .contains(query.lowercase()) || it.dialCodeString.contains(query)
                    }
                }
            }
        }

        CountriesList(
            countries = data,
            query = query,
            onQueryChange = { query = it },
            onCountryClick = { country ->
                NavigateDestinations.Countries.setResult(country)
                onDismissRequest()
            },
            itemContent = { country ->
                CountryItem(
                    name = country.name,
                    code = country.code,
                    dialCode = country.dialCodeString,
                    flagText = country.flag,
                    showDialCode = showDialCode
                )
            }
        )
    }
}