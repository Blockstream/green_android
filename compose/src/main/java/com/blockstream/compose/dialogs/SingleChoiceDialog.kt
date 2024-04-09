package com.blockstream.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.stringResourceId


@Composable
fun SingleChoiceDialog(
    title: String,
    message: String? = null,
    items: List<String>,
    checkedItem: Int? = null,
    onDismissRequest: (position: Int?) -> Unit
) {
    var checked by remember { mutableStateOf(checkedItem) }
    Dialog(
        onDismissRequest = {
            onDismissRequest(null)
        }
    ) {
        GreenCard(modifier = Modifier.fillMaxWidth()) {
            GreenColumn(padding = 0, space = 16) {
                Text(
                    text = stringResourceId(id = title),
                    style = titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!message.isNullOrBlank()) {
                    Text(
                        text = stringResourceId(id = message),
                        style = bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                LazyColumn {
                    itemsIndexed(items) { index, item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checked = index
                                    onDismissRequest(index)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (index == checked), onClick = null)
                            Text(
                                text = stringResourceId(item),
                                style = labelLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    GreenButton(
                        text = stringResource(id = R.string.id_cancel),
                        type = GreenButtonType.TEXT
                    ) {
                        onDismissRequest(null)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SingleChoiceDialogPreview() {
    GreenPreview {
        SingleChoiceDialog(
            title = "Auto logout Timeout",
            items = listOf("1 Minutes", "2 Minute", "5 Minutes", "30 Minutes"),
            checkedItem = 0
        ) {

        }
    }
}