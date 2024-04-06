package com.blockstream.compose.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.labelMedium

@Composable
fun GreenDataLayout(
    title: String,
    error: String? = null,
    withPadding: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            style = labelMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        GreenCard(
            padding = (if (withPadding) 16.dp else 0.dp),
            onClick = onClick,
            error = error,
            content = content
        )
    }
}

@Preview
@Composable
fun GreenDataLayoutPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenDataLayout(stringResource(R.string.id_amount)) {
                Text(text = "Test")
            }

            var error by remember {
                mutableStateOf<String?>("Error")
            }

            GreenDataLayout(stringResource(R.string.id_error), error = error, onClick = {
                if (error == null) {
                    error = "This is an error"
                } else {
                    error = null
                }
            }) {
                Text(text = "Test")
            }
        }
    }
}