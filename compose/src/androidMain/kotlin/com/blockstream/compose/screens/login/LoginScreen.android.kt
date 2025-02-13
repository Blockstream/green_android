package com.blockstream.compose.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.login.LoginViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun LoginScreenPreview() {
    GreenAndroidPreview {
        LoginScreen(viewModel = LoginViewModelPreview.previewWithPassword().also {

        })
    }
}

@Composable
@Preview
fun LoginScreenPreview2() {
    GreenAndroidPreview {
        LoginScreen(viewModel = LoginViewModelPreview.previewWatchOnly().also {
            it.onProgress.value = false
        })
    }
}

@Composable
@Preview
fun LoginScreenPreview4() {
    GreenAndroidPreview {
        LoginScreen(viewModel = LoginViewModelPreview.previewWithDevice().also {
            it.onProgress.value = true
        })
    }
}