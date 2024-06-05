package com.blockstream.compose.screens.jade

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.jade.JadeQRViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun QrPinUnlockScreenPreview() {
    GreenAndroidPreview {
        JadeQRScreen(JadeQRViewModelPreview.preview().also {
//            it.viewModelScope.coroutineScope.launch {
//                delay(2000L)
//                it.onProgress.value = true
//                delay(2000L)
//                it.onProgress.value = false
//            }
        })
    }
}