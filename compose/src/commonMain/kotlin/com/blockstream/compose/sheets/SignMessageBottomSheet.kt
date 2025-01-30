package com.blockstream.compose.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticate_address
import blockstream_green.common.generated.resources.id_heres_the_proof_of_ownership_of
import blockstream_green.common.generated.resources.id_message
import blockstream_green.common.generated.resources.id_paste_here_the_message_to_be
import blockstream_green.common.generated.resources.id_sign_message
import blockstream_green.common.generated.resources.id_signature
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.addresses.SignMessageViewModel
import com.blockstream.common.models.addresses.SignMessageViewModelAbstract
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.views.DataListItem
import org.jetbrains.compose.resources.stringResource
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
        title = stringResource(Res.string.id_authenticate_address),
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
                        Rive(RiveAnimation.CHECKMARK)

                        Text(
                            stringResource(Res.string.id_heres_the_proof_of_ownership_of),
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
                            title = StringHolder.create(Res.string.id_message),
                            data = StringHolder.create(message),
                            withDataLayout = true
                        )

                        DataListItem(
                            title = StringHolder.create(Res.string.id_signature),
                            data = StringHolder.create(signature ?: ""),
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
                        title = stringResource(Res.string.id_message),
                        value = message,
                        onValueChange = viewModel.message.onValueChange(),
                        singleLine = false,
                        minLines = 5,
                        placeholder = stringResource(Res.string.id_paste_here_the_message_to_be)
                    )

                    GreenButton(
                        text = stringResource(Res.string.id_sign_message),
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