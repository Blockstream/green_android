package com.blockstream.compose.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_do_you_want_to_enable_tor_to
import blockstream_green.common.generated.resources.id_ok
import blockstream_green.common.generated.resources.id_warning
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Warning
import com.blockstream.compose.models.MainViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun TorWarningDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            viewModel.postEvent(
                MainViewModel.LocalEvents.TorWarningResponse(
                    enable = false
                )
            )
            onDismiss()
        },
        icon = {
            Icon(imageVector = PhosphorIcons.Regular.Warning, contentDescription = null)
        },
        title = {
            Text(text = stringResource(Res.string.id_warning))
        },
        text = {
            Text(text = stringResource(Res.string.id_do_you_want_to_enable_tor_to))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.postEvent(
                        MainViewModel.LocalEvents.TorWarningResponse(
                            enable = true
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(Res.string.id_ok)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.postEvent(
                        MainViewModel.LocalEvents.TorWarningResponse(
                            enable = false
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(Res.string.id_cancel)
                )
            }
        }
    )
}
