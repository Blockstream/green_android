package com.blockstream.compose.screens.archived

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.archived.ArchivedAccountsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun ArchivedAccountsScreenPreview() {
    GreenAndroidPreview {
        ArchivedAccountsScreen(viewModel = ArchivedAccountsViewModelPreview.preview())
    }
}