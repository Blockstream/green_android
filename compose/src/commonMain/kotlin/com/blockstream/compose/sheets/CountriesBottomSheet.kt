package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_empty
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.Countries
import com.blockstream.common.data.Country
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenCard
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.components.GreenSearchField
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.bodyMedium
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class CountriesBottomSheet(
    val greenWallet: GreenWallet
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet, null, "Countries")
        }

        CountriesBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }

    companion object {
        @Composable
        fun getResult(fn: (Country) -> Unit) = getNavigationResult(this::class, fn)

        internal fun setResult(result: Country) =
            setNavigationResult(this::class, result)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
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
                        it.name.lowercase().contains(query.lowercase()) || it.dialCodeString.contains(query)
                    }
                }
            }
        }

        GreenColumn(
            padding = 0, space = 8, modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            GreenSearchField(
                value = query,
                onValueChange = {
                    query = it
                }
            )

            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(data) { country ->
                    GreenCard(onClick = {
                        CountriesBottomSheet.setResult(country)
                        onDismissRequest()
                    }) {
                        Column {
                            Text(country.name)
                            Text(country.dialCodeString)
                        }
                    }
                }
            }


            if (data.isEmpty()) {
                Text(
                    text = stringResource(Res.string.id_empty),
                    style = bodyMedium,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}