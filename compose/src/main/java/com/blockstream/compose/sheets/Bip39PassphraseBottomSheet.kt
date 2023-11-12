package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.login.Bip39PassphraseViewModel
import com.blockstream.common.models.login.Bip39PassphraseViewModelAbstract
import com.blockstream.common.models.login.Bip39PassphraseViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.views.GreenBottomSheet
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Parcelize
data class Bip39PassphraseBottomSheet(
    val greenWallet: GreenWallet,
    val passphrase: String,
    val setBip39Passphrase: (passphrase: String) -> Unit = { }
) : BottomScreen() {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<Bip39PassphraseViewModel> {
            parametersOf(greenWallet, passphrase)
        }

        Bip39PassphraseBottomSheet(
            viewModel = viewModel,
            sheetState = sheetState(skipPartiallyExpanded = true),
            setBip39Passphrase = setBip39Passphrase,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bip39PassphraseBottomSheet(
    viewModel: Bip39PassphraseViewModelAbstract,
    sheetState: SheetState = rememberModalBottomSheetState(),
    setBip39Passphrase: (passphrase: String) -> Unit = { },
    onDismissRequest: () -> Unit,
) {

    HandleSideEffect(viewModel = viewModel) {
        if(it is Bip39PassphraseViewModel.LocalSideEffects.SetBip39Passphrase){
            setBip39Passphrase(it.passphrase)
        }
    }

    GreenBottomSheet(
        title = stringResource(id = R.string.id_login_with_bip39_passphrase),
        viewModel = viewModel,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {

        val passphrase by viewModel.passphrase.collectAsStateWithLifecycle()
        var passwordVisibility: Boolean by remember { mutableStateOf(false) }
        TextField(
            value = passphrase,
            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
            onValueChange = viewModel.passphrase.onValueChange(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(id = R.string.id_passphrase)) },
            supportingText = {
                Text(
                    text = "${passphrase.length} / 100",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    passwordVisibility = !passwordVisibility
                }) {
                    Icon(
                        painter = painterResource(id = if (passwordVisibility) R.drawable.eye_slash else R.drawable.eye),
                        contentDescription = "password visibility",
                    )
                }
            }
        )

        Column {
            Text(text = stringResource(R.string.id_different_passphrases_generate))

            LearnMoreButton {
                viewModel.postEvent(Bip39PassphraseViewModel.LocalEvents.LearnMore)
            }
        }

        GreenColumn(padding = 0, space = 8) {
            Text(text = stringResource(R.string.id_always_ask), style = titleSmall)

            val isAlwaysAsk by viewModel.isAlwaysAsk.collectAsStateWithLifecycle()

            GreenRow(padding = 0, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.id_you_will_be_asked_to_enter_your),
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = isAlwaysAsk,
                    onCheckedChange = viewModel.isAlwaysAsk.onValueChange(),
                )
            }
        }

        GreenRow(padding = 0, modifier = Modifier.fillMaxWidth()) {
            GreenButton(
                text = stringResource(id = R.string.id_clear),
                modifier = Modifier.weight(1f),
                type = GreenButtonType.OUTLINE
            ) {
                viewModel.postEvent(Bip39PassphraseViewModel.LocalEvents.Clear)

            }

            GreenButton(
                text = stringResource(id = android.R.string.ok),
                modifier = Modifier.weight(1f)
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun Bip39PassphraseSheetPreview() {
    GreenTheme {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            Text("Bip39PassphraseSheet")

            if (showBottomSheet) {
                Bip39PassphraseBottomSheet(
                    viewModel = Bip39PassphraseViewModelPreview.preview(),
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}