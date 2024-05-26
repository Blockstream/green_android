package com.blockstream.compose.sheets

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rive.runtime.kotlin.RiveAnimationView
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.AddressInputType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.common.models.addresses.SignMessageViewModel
import com.blockstream.common.models.addresses.SignMessageViewModelAbstract
import com.blockstream.common.models.addresses.SignMessageViewModelPreview
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.wallet.WalletNameViewModel
import com.blockstream.common.models.wallet.WalletNameViewModelAbstract
import com.blockstream.common.models.wallet.WalletNameViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.onTextFieldValueChange
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.views.DataListItem
import org.koin.core.parameter.parametersOf

@Parcelize
data class SignMessageBottomSheet(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset,
    val address: String
) :
    BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SignMessageViewModel> {
            parametersOf(greenWallet, accountAsset, address)
        }

        SignMessageBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignMessageBottomSheet(
    viewModel: SignMessageViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_authenticate_address),
        subtitle = viewModel.address,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        val message by viewModel.message.collectAsStateWithLifecycle()
        val signature by viewModel.signature.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

        Column(modifier = Modifier.offset(y = -32.dp)) {
            AnimatedVisibility(visible = signature != null) {
                GreenColumn(padding = 0) {
                    Box {
                        AndroidView({
                            LayoutInflater.from(it)
                                .inflate(R.layout.rive, null)
                                .apply {
                                    val animationView: RiveAnimationView = findViewById(R.id.rive)
                                    animationView.setRiveResource(
                                        R.raw.checkmark,
                                        autoplay = true
                                    )
                                }
                        })

                        Text(
                            stringResource(R.string.id_here_s_the_proof_of_ownership),
                            style = titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomEnd)
                                .padding(top = 8.dp)
                                .padding(horizontal = 32.dp)
                        )
                    }

                    GreenColumn(padding = 0) {
                        DataListItem(
                            title = stringResource(R.string.id_message),
                            data = message,
                            withDataLayout = true
                        )

                        DataListItem(
                            title = stringResource(R.string.id_signature),
                            data = signature ?: "",
                            withDataLayout = true
                        )
                    }
// Sync with iOS to enable this feature
//                    GreenButton(
//                        text = stringResource(R.string.id_try_again),
//                        type = GreenButtonType.TEXT,
//                        size = GreenButtonSize.SMALL,
//                        modifier = Modifier.align(Alignment.CenterHorizontally)
//                    ) {
//                        viewModel.postEvent(SignMessageViewModel.LocalEvents.TryAgain)
//                    }
                }
            }

            AnimatedVisibility(visible = signature == null) {
                GreenColumn(padding = 0, modifier = Modifier.padding(top = 40.dp)) {
                    GreenTextField(
                        title = stringResource(id = R.string.id_message),
                        value = message,
                        onValueChange = viewModel.message.onValueChange(),
                        singleLine = false,
                        minLines = 5,
                        placeholder = stringResource(id = R.string.id_paste_here_the_message_to_be_signed)
                    )

                    GreenButton(
                        text = stringResource(R.string.id_sign_message),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = buttonEnabled
                    ) {
                        viewModel.postEvent(Events.Continue)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SignMessageBottomSheetPreview() {
    GreenPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            if (showBottomSheet) {
                SignMessageBottomSheet(
                    viewModel = SignMessageViewModelPreview.preview(),
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}