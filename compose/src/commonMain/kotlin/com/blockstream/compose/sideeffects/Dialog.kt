package com.blockstream.compose.sideeffects

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_contact_support
import blockstream_green.common.generated.resources.id_error
import blockstream_green.common.generated.resources.id_ok
import com.blockstream.common.data.SupportData
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.dialogs.SingleChoiceDialog
import com.blockstream.compose.theme.whiteHigh
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.resume


data class OpenDialogData constructor(
    val title: StringHolder? = null,
    val message: StringHolder? = null,
    val icon: DrawableResource? = null,
    val primaryText: String? = null,
    val secondaryText: String? = null,
    var supportData: SupportData? = null,
    val items: List<String>? = null,
    val onItem: (index: Int?) -> Unit = {},
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
class DialogState {

    private val mutex = Mutex()
    var data by mutableStateOf<OpenDialogData?>(null)
        private set

    fun clear() {
        data?.dismiss()
        data = null
    }

    suspend fun openErrorDialog(
        throwable: Throwable,
        supportData: SupportData? = null,
        onErrorReport: (supportData: SupportData) -> Unit = {},
        onClose: () -> Unit = {},
    ) {

        // Prevent showing user triggered cancel events as errors
        if (throwable.message == "id_action_canceled") {
            onClose()
            return
        }

        openDialog(
            OpenDialogData(
                title = StringHolder.create(Res.string.id_error),
                message = StringHolder.create(throwable.message ?: throwable.cause?.message),
                onDismiss = onClose,
                onPrimary = onClose,
                secondaryText = getString(Res.string.id_contact_support)
                    .takeIf { supportData != null },
                onSecondary = {
                    onErrorReport.invoke(supportData!!)
                }.takeIf { supportData != null }
            )
        )
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

        val items = data.items
        when {
            items != null -> {
                SingleChoiceDialog(
                    title = data.title?.string() ?: "",
                    message = data.message?.string(),
                    items = items,
                    onNeutralText = data.secondaryText,
                    onNeutralClick = {
                        data.onSecondary?.invoke()
                        state.clear()
                    },
                    onDismissRequest = {
                        data.onItem.invoke(it)
                        data.onDismiss.invoke()
                        state.clear()
                    }
                )
            }
            else -> {
                AlertDialog(
                    title = data.title?.let {
                        {
                            Text(text = it.string())
                        }
                    },
                    text = data.message?.let {
                        {
                            SelectionContainer {
                                Text(text = it.string())
                            }
                        }
                    },
                    icon = data.icon?.let {
                        {
                            Icon(
                                painter = painterResource(it),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                            )
                        }
                    },
                    iconContentColor = whiteHigh,
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
                            Text(
                                data.primaryText ?: stringResource(
                                    Res.string.id_ok
                                ))
                        }
                    },
                    dismissButton = {
                        if (state.data?.onSecondary != null || data.secondaryText != null) {
                            TextButton(
                                onClick = {
                                    data.onSecondary?.invoke()
                                    state.clear()
                                }
                            ) {
                                Text(data.secondaryText?.takeIf { it.isNotBlank() }
                                    ?: stringResource(Res.string.id_cancel))
                            }
                        }
                    }
                )
            }
        }
    }
}