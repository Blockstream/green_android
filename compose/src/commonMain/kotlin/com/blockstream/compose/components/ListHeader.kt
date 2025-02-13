package com.blockstream.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable

@Composable
fun ListHeader(title: String, cta: String? = null, onClick: () -> Unit = {}) {

    Row(modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)) {
        Text(
            text = title,
            style = bodyLarge,
            color = whiteMedium,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
        )

        cta?.also {
            Text(
                text = it,
                color = green,
                style = bodyLarge,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .noRippleClickable {
                        onClick()
                    }
            )
        }
    }
}