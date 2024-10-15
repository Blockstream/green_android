package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.generic_device
import blockstream_green.common.generated.resources.id_signer_unlocked
import blockstream_green.common.generated.resources.id_unlock_jade
import blockstream_green.common.generated.resources.id_unlock_signing_device
import blockstream_green.common.generated.resources.id_unlock_your_signing_device_before
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.textMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
object AskDeviceUnlockBottomSheet : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {

        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(null, null, "AskDeviceUnlock", false)
        }

        AskDeviceUnlockBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }

    @Composable
    fun getResult(fn: (Boolean) -> Unit) =
        getNavigationResult(this::class, fn)

    internal fun setResult(result: Boolean) =
        setNavigationResult(this::class, result)

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskDeviceUnlockBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {

    GreenBottomSheet(
        title = stringResource(Res.string.id_unlock_signing_device),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Image(
                painter = painterResource(Res.drawable.generic_device),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp)
            )

            GreenColumn(
                padding = 0,
                space = 16,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Text(
                    text = stringResource(Res.string.id_unlock_your_signing_device_before),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = textMedium
                )

//                GreenButton(
//                    text = stringResource(Res.string.id_learn_more),
//                    type = GreenButtonType.TEXT,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    viewModel.postEvent(Events.OpenBrowser(Urls.HELP_JADE_AIRGAPPED))
//                }

                GreenButton(
                    text = stringResource(Res.string.id_unlock_jade),
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE,
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AskJadeUnlockBottomSheet.setResult(true)
                    onDismissRequest()
                }

                GreenButton(
                    text = stringResource(Res.string.id_signer_unlocked),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AskJadeUnlockBottomSheet.setResult(false)
                    onDismissRequest()
                }
            }
        }
    }
}
