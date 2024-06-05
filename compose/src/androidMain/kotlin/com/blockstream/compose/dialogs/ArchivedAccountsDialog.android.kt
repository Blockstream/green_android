package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.archived.ArchivedAccountsViewModelPreview
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
fun ArchivedAccountsDialogPreview() {
    GreenThemePreview {
        ArchivedAccountsDialog(
            viewModel = ArchivedAccountsViewModelPreview.preview()
        ) {

        }
    }
}