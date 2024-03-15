package com.blockstream.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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

@Composable
fun TextInputPassword(passwordVisibility: MutableState<Boolean>) {
    IconButton(onClick = {
        passwordVisibility.value = !passwordVisibility.value
    }) {
        Icon(
            painter = painterResource(id = if (passwordVisibility.value) R.drawable.eye_slash else R.drawable.eye),
            contentDescription = "password visibility",
        )
    }
}