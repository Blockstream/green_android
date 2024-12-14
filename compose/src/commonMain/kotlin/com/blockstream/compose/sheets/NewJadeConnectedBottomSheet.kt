package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.blockstream_jade_device
import blockstream_green.common.generated.resources.hw_matrix_bg
import blockstream_green.common.generated.resources.id_a_new_device_has_been_detected
import blockstream_green.common.generated.resources.id_genuine_check
import blockstream_green.common.generated.resources.id_genuine_check_is_mandatory_for
import blockstream_green.common.generated.resources.id_new_jade_plus_connected
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.textMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
object NewJadeConnectedBottomSheet : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {

        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(null, null, "NewJadeConnected", false)
        }

        NewJadeConnectedBottomSheet(
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
fun NewJadeConnectedBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {

    var dismiss by remember { mutableStateOf(false) }

    GreenBottomSheet(
        title = stringResource(Res.string.id_new_jade_plus_connected),
        subtitle = stringResource(Res.string.id_a_new_device_has_been_detected),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { dismiss }
        ),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        onDismissRequest = onDismissRequest
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                Image(
                    painter = painterResource(Res.drawable.hw_matrix_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.Center)
                )

                Image(
                    painter = painterResource(Res.drawable.blockstream_jade_device),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.Center)
                )
            }


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
                GreenButton(
                    text = stringResource(Res.string.id_genuine_check),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NewJadeConnectedBottomSheet.setResult(true)
                    dismiss = true
                    onDismissRequest()
                }

                Text(
                    text = stringResource(Res.string.id_genuine_check_is_mandatory_for),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = bodyMedium,
                    color = textMedium
                )
            }
        }
    }
}
