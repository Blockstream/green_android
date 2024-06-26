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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.icon
import org.koin.core.parameter.parametersOf

@Parcelize
data class VerifyTransactionBottomSheet(
    val greenWallet: GreenWallet,
    val transactionConfirmLook: TransactionConfirmLook
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet, null, "VerifyTransaction")
        }

        VerifyTransactionBottomSheet(
            viewModel = viewModel,
            transactionConfirmLook = transactionConfirmLook,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyTransactionBottomSheet(
    viewModel: GreenViewModel,
    transactionConfirmLook: TransactionConfirmLook,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(R.string.id_confirm_on_your_device),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        Column {
            viewModel.sessionOrNull?.device?.also {
                Image(
                    painter = painterResource(id = it.icon()),
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

                transactionConfirmLook.utxos?.forEach {
                    GreenAmount(
                        title = stringResource(if (it.isChange) R.string.id_change else R.string.id_sent_to),
                        amount = it.amount ?: "",
                        assetId = it.assetId,
                        address = it.address,
                        session = viewModel.sessionOrNull,
                        showIcon = true
                    )
                }

                transactionConfirmLook.fee?.also {
                    GreenAmount(
                        title = stringResource(R.string.id_fee),
                        amount = it,
                        assetId = transactionConfirmLook.feeAssetId,
                        session = viewModel.sessionOrNull,
                        showIcon = true
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun VerifyTransactionBottomSheetPreview() {
    GreenPreview {
        VerifyTransactionBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            transactionConfirmLook = TransactionConfirmLook(
                utxos = listOf(
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        isChange = true,
                        amount = "1 BTC"
                    ),
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        isChange = false,
                        amount = "2 BTC"
                    )
                ),
                fee = "1 BTC"
            ),
            onDismissRequest = { }
        )
    }
}