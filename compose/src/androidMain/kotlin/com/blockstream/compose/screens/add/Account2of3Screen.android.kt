package com.blockstream.compose.screens.add

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.add.Account2of3ViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun Account2of3ScreenPreview() {
    GreenAndroidPreview {
        Account2of3Screen(viewModel = Account2of3ViewModelPreview.preview())
    }
}
