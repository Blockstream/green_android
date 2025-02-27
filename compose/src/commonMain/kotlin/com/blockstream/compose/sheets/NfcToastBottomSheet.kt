package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import blockstream_green.common.generated.resources.blockstream_devices
import blockstream_green.common.generated.resources.id_change
import blockstream_green.common.generated.resources.id_confirm_on_your_device
import blockstream_green.common.generated.resources.id_fee
import blockstream_green.common.generated.resources.id_green_needs_the_master_blinding
import blockstream_green.common.generated.resources.id_sent_to
import blockstream_green.common.generated.resources.id_to_show_balances_and
import blockstream_green.common.generated.resources.nfc_scan
import blockstream_green.common.generated.resources.phone_keys
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.extensions.actionIcon
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Parcelize
data class NfcToastBottomSheet(
    val message: String? = null
) : BottomScreen(), Parcelable {

    @Composable
    override fun Content() {


        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(null, null, "Scan card")
        }

        NfcToastBottomSheet(
            viewModel = viewModel,
            message = StringHolder.create(message),
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcToastBottomSheet(
    viewModel: SimpleGreenViewModel,
    message: StringHolder? = null,
    onDismissRequest: () -> Unit,
) {

    val title = "Scan your card" //stringResource(Res.string.id_scan_your_card)

    val deviceIcon = viewModel.device?.icon()

    GreenBottomSheet(
        title = title,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Image(
                //painter = painterResource(deviceIcon ?: Res.drawable.blockstream_devices),
                //painter = painterResource(Res.drawable.phone_keys),
                painter = painterResource(Res.drawable.nfc_scan),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(160.dp)
                    .padding(bottom = 16.dp)
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
                if (message != null) {
                    Text(
                        text = message.string(),
                        color = whiteMedium,
                        textAlign = TextAlign.Center,
                        style = titleSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
