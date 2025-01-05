package com.blockstream.compose.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.hw_matrix_bg
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_import_pubkey
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_login_with_biometrics
import blockstream_green.common.generated.resources.id_navigate_on_your_hardware_device
import blockstream_green.common.generated.resources.id_navigate_on_your_jade_to_options
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.models.devices.ImportPubKeyViewModel
import com.blockstream.common.models.devices.ImportPubKeyViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.screens.jade.JadeQRScreen
import com.blockstream.compose.sheets.EnvironmentBottomSheet
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.noRippleToggleable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


data class ImportPubKeyScreen(val deviceModel: DeviceModel) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ImportPubKeyViewModel> {
            parametersOf(deviceModel)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        ImportPubKeyScreen(viewModel = viewModel)
    }
}

@Composable
fun ImportPubKeyScreen(
    viewModel: ImportPubKeyViewModelAbstract
) {
    JadeQRScreen.getResult {
        viewModel.postEvent(
            ImportPubKeyViewModel.LocalEvents.ImportPubKey(
                pubKey = it
            )
        )
    }

    EnvironmentBottomSheet.getResult {
        if(it >= 0) {
            viewModel.postEvent(
                ImportPubKeyViewModel.LocalEvents.SelectEnviroment(
                    isTestnet = it == 1,
                    customNetwork = null
                )
            )
        }
    }

    HandleSideEffect(viewModel = viewModel)

    val deviceModel = viewModel.deviceModel
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Image(
                    painter = painterResource(Res.drawable.hw_matrix_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.Center)
                )

                Image(
                    painter = painterResource(deviceModel.icon()),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
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

                    val canUseBiometrics by viewModel.canUseBiometrics.collectAsStateWithLifecycle()
                    val withBiometrics by viewModel.withBiometrics.collectAsStateWithLifecycle()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.noRippleToggleable(
                            value = withBiometrics,
                            enabled = canUseBiometrics,
                            onValueChange = viewModel.withBiometrics.onValueChange()
                        )
                    ) {
                        Text(
                            text = stringResource(Res.string.id_login_with_biometrics),
                            style = bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f),
                            color = if (canUseBiometrics) whiteHigh else whiteLow
                        )

                        Switch(
                            checked = withBiometrics,
                            onCheckedChange = viewModel.withBiometrics.onValueChange(),
                            enabled = canUseBiometrics
                        )
                    }


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