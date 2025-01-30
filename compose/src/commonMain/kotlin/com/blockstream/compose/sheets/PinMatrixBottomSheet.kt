package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.backspace
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_enter_the_pin_for_your_hardware
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
object PinMatrixBottomSheet : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(null, null, "PinMatrix")
        }

        PinMatrixBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }

    @Composable
    fun getResult(fn: (String) -> Unit) =
        getNavigationResult(this::class, fn)

    fun setResult(result: String) =
        setNavigationResult(this::class, result)

}


@Composable
fun PinMatrixButton(digit: String, onClick: (digit: String) -> Unit) {
    OutlinedButton(onClick = { onClick.invoke(digit) }) {
        Text(
            "•",
            color = green,
            style = headlineSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinMatrixBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {

    HandleSideEffect(viewModel = viewModel)

    GreenBottomSheet(
        title = stringResource(Res.string.id_enter_the_pin_for_your_hardware),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        GreenColumn(padding = 0) {

            var pin by remember { mutableStateOf("") }

            TextField(
                value = pin,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        pin = pin.dropLast(1)
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.backspace),
                            contentDescription = "Backspace",
                        )
                    }
                }
            )

            GreenColumn(
                space = 6,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val onClick = { it: String ->
                    pin += it
                }
                GreenRow(padding = 0, space = 6) {
                    PinMatrixButton(digit = "7", onClick = onClick)
                    PinMatrixButton(digit = "8", onClick = onClick)
                    PinMatrixButton(digit = "9", onClick = onClick)
                }
                GreenRow(padding = 0, space = 6) {
                    PinMatrixButton(digit = "4", onClick = onClick)
                    PinMatrixButton(digit = "5", onClick = onClick)
                    PinMatrixButton(digit = "6", onClick = onClick)
                }
                GreenRow(padding = 0, space = 6) {
                    PinMatrixButton(digit = "1", onClick = onClick)
                    PinMatrixButton(digit = "2", onClick = onClick)
                    PinMatrixButton(digit = "3", onClick = onClick)
                }
            }

            GreenButton(
                text = stringResource(Res.string.id_continue),
                modifier = Modifier.fillMaxWidth()
            ) {
                PinMatrixBottomSheet.setResult(pin)
                onDismissRequest()
            }
        }
    }
}