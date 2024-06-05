package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_change
import blockstream_green.common.generated.resources.id_confirm_on_your_device
import blockstream_green.common.generated.resources.id_fee
import blockstream_green.common.generated.resources.id_sent_to
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class VerifyOnDeviceBottomSheet(
    val greenWallet: GreenWallet,
    val transactionConfirmLook: TransactionConfirmLook?,
    val address: String?
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet, null, if (address == null) "VerifyTransaction" else "VerifyAddress")
        }

        VerifyOnDeviceBottomSheet(
            viewModel = viewModel,
            transactionConfirmLook = transactionConfirmLook,
            address = address,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyOnDeviceBottomSheet(
    viewModel: GreenViewModel,
    transactionConfirmLook: TransactionConfirmLook?,
    address: String?,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_confirm_on_your_device),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        Column {
            viewModel.sessionOrNull?.device?.also {
                Image(
                    painter = painterResource(it.icon()),
                    contentDescription = it.deviceBrand.toString(),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(100.dp)

                )
            }

            GreenColumn(
                padding = 0,
                space = 16,
                modifier = Modifier
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                if(transactionConfirmLook != null) {
                    transactionConfirmLook.utxos?.forEach {
                        GreenAmount(
                            title = stringResource(if (it.isChange) Res.string.id_change else Res.string.id_sent_to),
                            amount = it.amount ?: "",
                            assetId = it.assetId,
                            address = it.address,
                            session = viewModel.sessionOrNull,
                            showIcon = true
                        )
                    }

                    transactionConfirmLook.fee?.also {
                        GreenAmount(
                            title = stringResource(Res.string.id_fee),
                            amount = it,
                            assetId = transactionConfirmLook.feeAssetId,
                            session = viewModel.sessionOrNull,
                            showIcon = true
                        )
                    }
                }

                if(address != null){
                    GreenAddress(address = address)
                }
            }
        }
    }
}
