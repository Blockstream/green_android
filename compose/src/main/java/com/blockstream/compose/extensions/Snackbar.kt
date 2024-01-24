package com.blockstream.compose.extensions

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.R
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.utils.stringResourceId

suspend fun SnackbarHostState.showErrorSnackbar(
    context: Context,
    dialogState: DialogState,
    viewModel: GreenViewModel,
    error: Throwable,
    errorReport: ErrorReport? = null
) {
    val result = showSnackbar(
        message = stringResourceId(context, error.message ?: "Undefined Error"),
        actionLabel = if (errorReport != null) context.getString(R.string.id_contact_support) else null
    )

    if (result == SnackbarResult.ActionPerformed && errorReport != null) {
        dialogState.openErrorReportDialog(
            errorReport = errorReport,
            viewModel = viewModel,
            onSubmitErrorReport = { submitErrorReport ->
                viewModel.postEvent(submitErrorReport)
            }
        )
    }
}