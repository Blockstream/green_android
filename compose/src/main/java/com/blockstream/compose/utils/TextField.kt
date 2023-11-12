package com.blockstream.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.compose.R
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun TextInputPaste(state: MutableStateFlow<String>) {

    val context = LocalContext.current
    val value by state.collectAsStateWithLifecycle()

    if (value.isEmpty()) {
        Icon(painterResource(id = R.drawable.clipboard_text),
            contentDescription = "clear text",
            modifier = Modifier.clickable {
                    state.value = getClipboard(context) ?: ""
                })
    } else {
        Icon(Icons.Default.Clear, contentDescription = "clear text", modifier = Modifier.clickable {
                state.value = ""
            })
    }
}