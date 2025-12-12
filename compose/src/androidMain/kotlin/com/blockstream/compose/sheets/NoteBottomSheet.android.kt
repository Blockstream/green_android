package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.sheets.NoteViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun NoteBottomSheetPreview() {
    GreenAndroidPreview {
        NoteBottomSheet(
            viewModel = NoteViewModelPreview.preview(),
            onDismissRequest = {}
        )
    }
}