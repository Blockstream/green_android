package com.blockstream.compose.screens.jade

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.blockstream.common.events.Events
import com.blockstream.common.models.jade.JadeQRViewModel
import com.blockstream.common.models.jade.JadeQRViewModelAbstract
import com.blockstream.common.models.jade.JadeQRViewModelPreview
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.BarcodeScanner
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf

@Parcelize
data class JadeQRScreen(val isLightningMnemonicExport: Boolean = false) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<JadeQRViewModel> {
            parametersOf(isLightningMnemonicExport)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        JadeQRScreen(viewModel = viewModel)
    }
}

@Composable
fun JadeQRScreen(
    viewModel: JadeQRViewModelAbstract,
) {

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel = viewModel) {

        if (it is SideEffects.Mnemonic) {
            setNavigationResult(JadeQRScreen::class.resultKey, it.mnemonic)
        } else if (it is JadeQRViewModel.LocalSideEffects.ScanQr) {
            bottomSheetNavigator.show(
                CameraBottomSheet(
                    isDecodeContinuous = true,
                    parentScreenName = viewModel.screenName(),
                )
            )
        }
    }

    val step by viewModel.stepInfo.collectAsStateWithLifecycle()

    GreenColumn(space = 24, horizontalAlignment = Alignment.CenterHorizontally) {
        GreenColumn(padding = 0, space = 6, horizontalAlignment = Alignment.CenterHorizontally) {
            val scenario by viewModel.scenario.collectAsStateWithLifecycle()
            if(scenario.showStepCounter) {
                GreenColumn(
                    padding = 0,
                    space = 6,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = if (step.isScan) R.drawable.scan else R.drawable.qr_code),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(green),
                        modifier = Modifier.size(50.dp)
                    )


                    Text(
                        text = stringResourceId(id = step.stepMessage).uppercase(),
                        style = labelLarge,
                        textAlign = TextAlign.Center,
                        color = green
                    )
                }
            }

            Text(
                text = stringResourceId(id = step.title),
                style = headlineSmall
            )

            Text(
                text = stringResourceId(id = step.message),
                style = bodyLarge,
                color = whiteMedium
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
                    BarcodeScanner(
                        isDecodeContinuous = true,
                        viewModel = viewModel,
                        showScanFromImage = false,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .align(Alignment.Center)
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

        Column {
            if (!step.isScan) {
                val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
                GreenButton(
                    text = stringResource(id = R.string.id_next),
                    modifier = Modifier
                        .fillMaxWidth(),
                    size = GreenButtonSize.BIG,
                    enabled = buttonEnabled
                ) {
                    viewModel.postEvent(Events.Continue)
                }
            }

            GreenButton(
                text = stringResource(id = R.string.id_troubleshoot),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                type = GreenButtonType.TEXT
            ) {
                viewModel.postEvent(JadeQRViewModel.LocalEvents.ClickTroubleshoot)
            }
        }
    }
}

@Composable
@Preview
fun QrPinUnlockScreenPreview() {
    GreenPreview {
        JadeQRScreen(JadeQRViewModelPreview.preview().also {

            it.viewModelScope.coroutineScope.launch {
                delay(2000L)
                it.onProgress.value = true
                delay(2000L)
                it.onProgress.value = false
            }
        })
    }
}