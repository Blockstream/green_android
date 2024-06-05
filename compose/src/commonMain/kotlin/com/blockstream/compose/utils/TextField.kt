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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.eye_slash
import com.blockstream.compose.managers.LocalPlatformManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.painterResource


@Composable
fun TextInputPaste(state: MutableStateFlow<String>) {
    val platformManager = LocalPlatformManager.current
    val value by state.collectAsStateWithLifecycle()

    if (value.isEmpty()) {
        Icon(painterResource(Res.drawable.clipboard),
            contentDescription = "clear text",
            modifier = Modifier.clickable {
                state.value = platformManager.getClipboard() ?: ""
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
            painter = painterResource(if(passwordVisibility.value) Res.drawable.eye_slash else Res.drawable.eye),
            contentDescription = "Password visibility",
        )
    }
}