package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_passphrase
import blockstream_green.common.generated.resources.id_please_enter_the_passphrase_for
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.TextInputPassword
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
object PassphraseBottomSheet : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(null, null, "PassphraseHW")
        }

        PassphraseBottomSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassphraseBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {

    HandleSideEffect(viewModel = viewModel)

    GreenBottomSheet(
        title = stringResource(Res.string.id_please_enter_the_passphrase_for),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        GreenColumn(padding = 0) {

            var passphrase by remember { mutableStateOf("") }
            val passwordVisibility = remember { mutableStateOf(false) }
            val focusManager = LocalFocusManager.current
            TextField(
                value = passphrase,
                visualTransformation = if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation(),
                onValueChange = {
                    passphrase = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                label = { Text(stringResource(Res.string.id_passphrase)) },
                trailingIcon = {
                    TextInputPassword(passwordVisibility)
                }
            )

            GreenButton(
                text = stringResource(Res.string.id_continue),
                modifier = Modifier.fillMaxWidth()
            ) {
                PassphraseBottomSheet.setResult(passphrase)
                onDismissRequest()
            }
        }
    }
}