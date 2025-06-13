package com.blockstream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_empty
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> CountriesList(
    countries: List<T>,
    query: String,
    onQueryChange: (String) -> Unit,
    onCountryClick: (T) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    GreenColumn(
        padding = 0, space = 8, modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()
    ) {
        GreenSearchField(
            value = query, onValueChange = onQueryChange
        )

        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(countries) { country ->
                GreenCard(onClick = { onCountryClick(country) }) {
                    itemContent(country)
                }
            }
        }

        if (countries.isEmpty()) {
            Text(
                text = stringResource(Res.string.id_empty),
                style = bodyMedium,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun CountryItem(
    name: String,
    code: String,
    flagText: String,
    dialCode: String? = null,
    showDialCode: Boolean = false
) {
    GreenRow(padding = 0) {

        Text(
            text = flagText, style = displaySmall
        )

        Column {
            Text(text = name, style = labelLarge)
            Text(
                text = if (showDialCode && dialCode != null) {
                    dialCode
                } else {
                    code.uppercase()
                }, style = bodyMedium, color = whiteMedium
            )
        }
    }
}