package com.blockstream.compose.screens.send

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_next
import blockstream_green.common.generated.resources.id_recipient_address
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.models.send.SendAddressViewModelAbstract
import com.blockstream.compose.models.send.SendAddressViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.data.AddressInputType
import com.blockstream.data.data.ScanResult
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendAddressScreen(
    viewModel: SendAddressViewModelAbstract
) {

    NavigateDestinations.Camera.getResult<ScanResult> {
        viewModel.address.value = it.result
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetAddressInputType(AddressInputType.SCAN))
    }

    val error by viewModel.error.collectAsStateWithLifecycle()

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Banner(viewModel)

                val address by viewModel.address.collectAsStateWithLifecycle()

                GreenTextField(
                    title = stringResource(Res.string.id_recipient_address),
                    value = address,
                    onValueChange = {
                        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetAddressInputType(AddressInputType.PASTE))
                        viewModel.address.value = it
                    },
                    singleLine = false,
                    maxLines = 8,
                    error = error,
                    testTag = "address_textfield",
                    onQrClick = {
                        viewModel.postEvent(
                            NavigateDestinations.Camera(
                                isDecodeContinuous = true,
                                parentScreenName = viewModel.screenName()
                            )
                        )
                    }
                )
            }

            val isValid by viewModel.isValid.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(Res.string.id_next),
                enabled = isValid,
                size = GreenButtonSize.BIG,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}

@Composable
@Preview
fun SendAddressScreenPreview2() {
    GreenPreview {
        SendAddressScreen(viewModel = SendAddressViewModelPreview.preview(isLightning = true))
    }
}

