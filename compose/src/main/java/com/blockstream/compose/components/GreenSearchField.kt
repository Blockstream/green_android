package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.utils.getClipboard

@Composable
fun GreenSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(stringResource(R.string.id_search))
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.magnifying_glass),
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (value.isEmpty()) {
                val context = LocalContext.current
                IconButton(
                    onClick = { onValueChange(getClipboard(context) ?: "") },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.clipboard),
                        contentDescription = "Edit"
                    )
                }
            } else {
                IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.x_circle),
                        contentDescription = "Clear"
                    )
                }
            }
        }
    )

}

@Preview
@Composable
fun GreenSearchFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            var text by remember {
                mutableStateOf("")
            }
            GreenSearchField(text, {
                text = it
            })
        }
    }
}