package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.blockstream_jade_device
import blockstream_green.common.generated.resources.id_firmware_upgrade
import com.blockstream.common.models.sheets.JadeFirmwareUpdateViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadeFirmwareUpdateBottomSheet(
    viewModel: JadeFirmwareUpdateViewModelAbstract,
    onDismissRequest: () -> Unit,
) {

    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val firmware by viewModel.firmware.collectAsStateWithLifecycle()
    val hash by viewModel.hash.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()

    GreenBottomSheet(
        title = stringResource(Res.string.id_firmware_upgrade),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Image(
                painter = painterResource(Res.drawable.blockstream_jade_device),
                contentDescription = null
            )

            GreenColumn(padding = 0, space = 24, horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = firmware,
                        style = labelLarge,
                        color = whiteHigh,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = hash,
                        style = bodyMedium,
                        color = whiteMedium,
                        fontFamily = MonospaceFont(),
                        textAlign = TextAlign.Center
                    )
                }

                Column {
                    LinearProgressIndicator(progress = {
                        progress / 100f
                    }, modifier = Modifier.fillMaxWidth().height(6.dp))

                    Text(
                        text = transfer,
                        style = bodySmall,
                        color = whiteMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
