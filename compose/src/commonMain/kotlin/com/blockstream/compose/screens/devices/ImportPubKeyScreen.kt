package com.blockstream.compose.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_import_pubkey
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_navigate_on_your_hardware_device
import blockstream_green.common.generated.resources.id_navigate_on_your_jade_to_options
import com.blockstream.common.models.devices.ImportPubKeyViewModel
import com.blockstream.common.models.devices.ImportPubKeyViewModelAbstract
import com.blockstream.common.models.devices.ImportPubKeyViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.screens.jade.JadeQRResult
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ImportPubKeyScreen(
    viewModel: ImportPubKeyViewModelAbstract
) {
    NavigateDestinations.JadeQR.getResult<JadeQRResult> {
        viewModel.postEvent(
            ImportPubKeyViewModel.LocalEvents.ImportPubKey(
                pubKey = it.result
            )
        )
    }

    NavigateDestinations.Environment.getResult<Int> {
        if (it >= 0) {
            viewModel.postEvent(
                ImportPubKeyViewModel.LocalEvents.SelectEnviroment(
                    isTestnet = it == 1,
                    customNetwork = null
                )
            )
        }
    }

    val deviceModel = viewModel.deviceModel
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, onProgressStyle = OnProgressStyle.Disabled) {

        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Image(
                    painter = painterResource(deviceModel.icon()),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            GreenColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                Text(
                    text = stringResource(Res.string.id_import_pubkey),
                    style = titleMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(if (deviceModel.isJade) Res.string.id_navigate_on_your_jade_to_options else Res.string.id_navigate_on_your_hardware_device),
                    style = bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        GreenColumn(
            padding = 0,
            space = 0,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom
        ) {

            AnimatedVisibility(visible = onProgress, Modifier.fillMaxSize()) {
                Box {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(120.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = !onProgress) {

                GreenColumn {
                    GreenButton(
                        text = stringResource(Res.string.id_continue),
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.postEvent(ImportPubKeyViewModel.LocalEvents.ScanXpub)
                        }
                    )

                    GreenButton(
                        text = stringResource(Res.string.id_learn_more),
                        type = GreenButtonType.TEXT,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.postEvent(ImportPubKeyViewModel.LocalEvents.LearnMore)
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun ImportPubKeyScreenPreview() {
    GreenPreview {
        ImportPubKeyScreen(ImportPubKeyViewModelPreview())
    }
}