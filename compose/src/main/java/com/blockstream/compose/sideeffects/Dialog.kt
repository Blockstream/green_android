@file:OptIn(ExperimentalMaterial3Api::class)

package com.blockstream.compose.sideeffects

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.R
import com.blockstream.compose.dialogs.ErrorReportDialog
import com.blockstream.compose.utils.copyToClipboard
import com.blockstream.compose.utils.openNewTicketUrl
import com.blockstream.compose.utils.stringResourceId
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume


data class OpenDialogData(
    val title: String? = null,
    val message: String? = null,
    val primaryText: String? = null,
    val secondaryText: String? = null,
    var errorReport: ErrorReport? = null,
    val onSubmitErrorReport: ((submitErrorReport: Events.SubmitErrorReport) -> Unit)? = null,
    val onPrimary: () -> Unit = {},
    val onSecondary: (() -> Unit)? = null,
    val onDismiss: () -> Unit = {},
    var continuation: CancellableContinuation<Unit>? = null
) {
    fun dismiss() {
        if (continuation?.isActive == true) {
            continuation?.resume(Unit)
        }
    }
}

@Stable
class DialogState(val context: Context) {

    private val mutex = Mutex()
    var data by mutableStateOf<OpenDialogData?>(null)
        private set

    fun clear() {
        data?.dismiss()
        data = null
    }

    suspend fun openErrorDialog(
        throwable: Throwable,
        errorReport: ErrorReport? = null,
        onErrorReport: (errorReport: ErrorReport) -> Unit = {},
        onClose: () -> Unit = {},
    ) {

        // Prevent showing user triggered cancel events as errors
        if (throwable.message == "id_action_canceled") {
            return
        }

        openDialog(
            OpenDialogData(
                title = context.getString(R.string.id_error),
                message = throwable.message,
                onDismiss = onClose,
                onPrimary = onClose,
                secondaryText = context.getString(R.string.id_contact_support)
                    .takeIf { errorReport != null },
                onSecondary = {
                    onErrorReport.invoke(errorReport!!)
                    onClose()
                }.takeIf { errorReport != null }
            )
        )
    }

    suspend fun openErrorReportDialog(
        errorReport: ErrorReport,
        viewModel: GreenViewModel,
        onSubmitErrorReport: (submitErrorReport: Events.SubmitErrorReport) -> Unit,
        onClose: () -> Unit = {},
    ) {
        if (viewModel.settingsManager.appSettings.tor || !viewModel.zendeskSdk.isAvailable) {
            openBrowser(
                context = context,
                dialogState = this,
                isTor = viewModel.settingsManager.appSettings.tor,
                url = openNewTicketUrl(
                    appInfo = viewModel.appInfo,
                    subject = viewModel.screenName()?.let { "Android Issue in $it" } ?: "Android Error Report",
                    errorReport = errorReport,
                )
            )
            copyToClipboard(context = context, "Error Report", errorReport.error)
            onClose()
        } else {
            openDialog(
                OpenDialogData(
                    errorReport = errorReport,
                    onSubmitErrorReport = onSubmitErrorReport,
                    onDismiss = onClose,
                )
            )
        }
    }

    suspend fun openDialog(openDialogData: OpenDialogData): Unit = mutex.withLock {
        try {
            return suspendCancellableCoroutine { continuation ->
                data = openDialogData.also {
                    it.continuation = continuation
                }
            }
        } finally {
            data = null
        }
    }
}

@Composable
fun DialogHost(state: DialogState) {
    state.data?.also { data ->

        val errorReport = data.errorReport
        if (errorReport == null) {
            AlertDialog(
                title = data.title?.let {
                    {
                        Text(text = stringResourceId(it))
                    }
                },
                text = data.message?.let {
                    {
                        Text(text = stringResourceId(it))
                    }
                },
                onDismissRequest = {
                    data.onDismiss.invoke()
                    state.clear()
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            data.onPrimary.invoke()
                            state.clear()
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    if (state.data?.onSecondary != null) {
                        TextButton(
                            onClick = {
                                data.onSecondary?.invoke()
                                state.clear()
                            }
                        ) {
                            Text(data.secondaryText.takeIf { it.isNotBlank() }
                                ?: stringResource(android.R.string.cancel))
                        }
                    }
                }
            )
        } else {
            ErrorReportDialog(
                errorReport = errorReport,
                onSubmitErrorReport = data.onSubmitErrorReport,
                onDismiss = {
                    data.onDismiss()
                    state.clear()
                }
            )
        }
    }
}