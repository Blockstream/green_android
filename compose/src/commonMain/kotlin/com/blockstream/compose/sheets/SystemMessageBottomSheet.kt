package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_accept
import blockstream_green.common.generated.resources.id_i_confirm_i_have_read_and
import blockstream_green.common.generated.resources.id_system_message
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMessageBottomSheet(
    viewModel: GreenViewModel,
    network: Network,
    message: String,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_system_message),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        var isConfirmed by remember { mutableStateOf(false) }

        GreenColumn(
            padding = 0, space = 16, modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            Text(message)

            Row(
                modifier = Modifier.toggleable(
                    value = isConfirmed,
                    onValueChange = {
                        isConfirmed = it
                    },
                    role = Role.Checkbox
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isConfirmed,
                    onCheckedChange = {
                        isConfirmed = it
                    }
                )

                Text(text = stringResource(Res.string.id_i_confirm_i_have_read_and), style = bodyMedium)
            }

            val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

            GreenButton(
                text = stringResource(Res.string.id_accept),
                enabled = isConfirmed && !onProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.postEvent(Events.AckSystemMessage(network = network, message = message))
            }
        }
    }
}
