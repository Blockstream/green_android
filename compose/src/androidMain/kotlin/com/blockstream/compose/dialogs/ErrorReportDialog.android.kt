package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.ErrorReport
import com.blockstream.compose.theme.GreenThemePreview



@Composable
@Preview
fun ErrorReportDialogPreview() {
    GreenThemePreview {
        ErrorReportDialog(errorReport = ErrorReport.create(Exception("Preview")))
    }
}