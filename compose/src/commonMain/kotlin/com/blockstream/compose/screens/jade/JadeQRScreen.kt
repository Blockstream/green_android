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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.DeviceModel
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
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.components.GreenScanner
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.getNavigationResultForKey
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.navigation.setNavigationResultForKey
import com.blockstream.compose.sheets.AskJadeUnlockBottomSheet
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.textHigh
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class JadeQRScreen(
    val greenWallet: GreenWallet? = null,
    val operation: JadeQrOperation,
    val deviceModel: DeviceModel
) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<JadeQRViewModel> {
            parametersOf(greenWallet, operation, deviceModel)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        JadeQRScreen(viewModel = viewModel)
    }

    @IgnoredOnParcel
    override val key = uniqueScreenKey

    companion object {
        @Composable
        fun getResult(fn: (String) -> Unit) = getNavigationResult(this::class, fn)

        @Composable
        fun getResultPinUnlock(fn: (Boolean) -> Unit) = getNavigationResultForKey("${this::class.resultKey}-pinUnlock", fn)

        internal fun setResult(result: String) =
            setNavigationResult(this::class, result)

        internal fun setResultPinUnlock(result: Boolean) = setNavigationResultForKey("${this::class.resultKey}-pinUnlock", result)
    }
}

@Composable
fun JadeQRScreen(
    viewModel: JadeQRViewModelAbstract,
) {

    val isLightTheme by viewModel.isLightTheme.collectAsStateWithLifecycle()

    AskJadeUnlockBottomSheet.getResult { isUnlocked ->
        if (!isUnlocked) {
            viewModel.postEvent(NavigateDestinations.JadeQR(operation = JadeQrOperation.PinUnlock))
        }
    }

    HandleSideEffect(viewModel = viewModel) {
        when (it) {
            is SideEffects.Success -> {
                if (viewModel.operation is JadeQrOperation.PinUnlock) {
                    JadeQRScreen.setResultPinUnlock(true)
                } else {
                    JadeQRScreen.setResult(it.data as String)
                }
            }

            is SideEffects.Mnemonic -> {
                JadeQRScreen.setResult(it.mnemonic)
            }
        }
    }

    val step by viewModel.stepInfo.collectAsStateWithLifecycle()

    GreenTheme(isLight = isLightTheme) {

        Scaffold {

            GreenColumn(space = 24, padding = 24, horizontalAlignment = Alignment.CenterHorizontally) {

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
                                    text = stringResource(Res.string.id_step_1s, it).uppercase(),
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

                        if((viewModel.operation as? JadeQrOperation.Psbt)?.transactionConfirmLook != null) {
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