package com.blockstream.compose.screens.jade

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_check_transaction_details
import blockstream_green.common.generated.resources.id_next
import blockstream_green.common.generated.resources.id_step_1s
import blockstream_green.common.generated.resources.id_troubleshoot
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.scan
import com.blockstream.common.events.Events
import com.blockstream.common.models.jade.JadeQRViewModel
import com.blockstream.common.models.jade.JadeQRViewModelAbstract
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.components.GreenScanner
import com.blockstream.ui.navigation.LocalInnerPadding
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.textHigh
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.getResult
import com.blockstream.ui.navigation.setResult
import com.blockstream.ui.utils.bottom
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data class JadeQRResult(
    val pinUnlock: Boolean? = null,
    val result: String = ""
)

@Composable
fun JadeQRScreen(
    viewModel: JadeQRViewModelAbstract,
) {
    val isLightTheme by viewModel.isLightTheme.collectAsStateWithLifecycle()
    val innerPadding = LocalInnerPadding.current

    NavigateDestinations.AskJadeUnlock.getResult<Boolean> { isUnlocked ->
        if (!isUnlocked) {
            viewModel.postEvent(
                NavigateDestinations.JadeQR(
                    greenWalletOrNull = viewModel.greenWalletOrNull,
                    operation = JadeQrOperation.PinUnlock,
                    deviceModel = viewModel.deviceModel
                )
            )
        }
    }

    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false, sideEffectsHandler = {
        when (it) {
            is SideEffects.Success -> {
                if (viewModel.operation is JadeQrOperation.PinUnlock) {
                    NavigateDestinations.JadeQR.setResult(JadeQRResult(pinUnlock = true))
                } else {
                    NavigateDestinations.JadeQR.setResult(JadeQRResult(result = it.data as String))
                }
            }

            is SideEffects.Mnemonic -> {
                NavigateDestinations.JadeQR.setResult(JadeQRResult(result = it.mnemonic))
            }
        }
    }) {

        val step by viewModel.stepInfo.collectAsStateWithLifecycle()

        GreenTheme(isLight = isLightTheme) {

            Scaffold {

                GreenColumn(
                    modifier = Modifier.padding(innerPadding.bottom()),
                    space = 24,
                    padding = 24,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    GreenColumn(
                        padding = 0,
                        space = 6,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        val scenario by viewModel.scenario.collectAsStateWithLifecycle()
                        if (scenario.showStepCounter) {
                            GreenColumn(
                                padding = 0,
                                space = 6,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(if (step.isScan) Res.drawable.scan else Res.drawable.qr_code),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(green),
                                    modifier = Modifier.size(50.dp)
                                )

                                step.step?.also {
                                    Text(
                                        text = stringResource(
                                            Res.string.id_step_1s,
                                            it
                                        ).uppercase(),
                                        style = labelLarge,
                                        textAlign = TextAlign.Center,
                                        color = green
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(step.title),
                            style = headlineSmall,
                            color = textHigh
                        )

                        Text(
                            text = stringResource(step.message),
                            style = labelLarge,
                            color = textMedium,
                            textAlign = TextAlign.Center
                        )
                    }


                    val qrCode by viewModel.urPart.collectAsState()
                    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
                    Box(
                        modifier = Modifier
                            .weight(1f),
                    ) {

                        if (onProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(120.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            if (step.isScan) {
                                GreenScanner(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .align(Alignment.Center),
                                    isDecodeContinuous = true,
                                    showScanFromImage = false,
                                    viewModel = viewModel
                                )
                            } else {
                                GreenQR(
                                    data = qrCode,
                                    isJadeQR = true,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    GreenColumn(padding = 0, space = 8) {

                        if (!step.isScan) {

                            if ((viewModel.operation as? JadeQrOperation.Psbt)?.transactionConfirmLook != null) {
                                GreenButton(
                                    text = stringResource(Res.string.id_check_transaction_details),
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    size = GreenButtonSize.BIG,
                                    color = GreenButtonColor.WHITE,
                                    type = GreenButtonType.OUTLINE
                                ) {
                                    viewModel.postEvent(JadeQRViewModel.LocalEvents.CheckTransactionDetails)
                                }
                            }


                            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
                            GreenButton(
                                text = stringResource(Res.string.id_next),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                size = GreenButtonSize.BIG,
                                enabled = buttonEnabled
                            ) {
                                viewModel.postEvent(Events.Continue)
                            }
                        }

                        GreenButton(
                            text = stringResource(Res.string.id_troubleshoot),
                            modifier = Modifier
                                .fillMaxWidth(),
                            type = GreenButtonType.TEXT
                        ) {
                            viewModel.postEvent(JadeQRViewModel.LocalEvents.ClickTroubleshoot)
                        }
                    }
                }
            }
        }
    }
}