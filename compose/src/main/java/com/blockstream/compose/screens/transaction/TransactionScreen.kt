package com.blockstream.compose.screens.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.looks.transaction.Failed
import com.blockstream.common.looks.transaction.TransactionStatus
import com.blockstream.common.looks.transaction.Unconfirmed
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.common.models.transaction.TransactionViewModelAbstract
import com.blockstream.common.models.transaction.TransactionViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.TransactionStatusIcon
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.color
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.extensions.title
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.AssetDetailsBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.MenuBottomSheet
import com.blockstream.compose.sheets.MenuEntry
import com.blockstream.compose.sheets.NoteBottomSheet
import com.blockstream.compose.sheets.TransactionDetailsBottomSheet
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.monospaceFont
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.formatFullWithTime
import com.blockstream.compose.utils.stringResourceId
import org.koin.core.parameter.parametersOf
import java.util.Date


@Parcelize
data class TransactionScreen(val transaction: Transaction, val greenWallet: GreenWallet) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel = getScreenModel<TransactionViewModel>() {
            parametersOf(transaction, greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        TransactionScreen(viewModel = viewModel)
    }
}

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModelAbstract
) {
    val context = LocalContext.current
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

    getNavigationResult<String>(NoteBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(TransactionViewModel.LocalEvents.SetNote(it))
    }

    getNavigationResult<Int>(MenuBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(
            TransactionViewModel.LocalEvents.ShareTransaction(
                liquidShareType = when (it) {
                    0 -> TransactionViewModel.LiquidShareType.CONFIDENTIAL_TRANSACTION
                    1 -> TransactionViewModel.LiquidShareType.NON_CONFIDENTIAL_TRANSACTION
                    else -> TransactionViewModel.LiquidShareType.UNBLINDING_DATA
                }
            )
        )
    }

    HandleSideEffect(viewModel) { sideEffect ->
        if (sideEffect is TransactionViewModel.LocalSideEffects.SelectLiquidShareTransaction) {
            bottomSheetNavigator.show(
                MenuBottomSheet(
                    title = context.getString(R.string.id_share), entries = listOf(
                        MenuEntry(
                            title = context.getString(R.string.id_confidential_transaction),
                            iconRes = R.drawable.eye_slash
                        ),
                        MenuEntry(
                            title = context.getString(R.string.id_non_confidential_transaction),
                            iconRes = R.drawable.eye
                        ),
                        MenuEntry(
                            title = context.getString(R.string.id_unblinding_data),
                            iconRes = R.drawable.code_block
                        )
                    )
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val status by viewModel.status.collectAsStateWithLifecycle()
        val amounts by viewModel.amounts.collectAsStateWithLifecycle()

        Box(modifier = Modifier.fillMaxWidth()) {

            val spv by viewModel.spv.collectAsStateWithLifecycle()
            if (!spv.disabled()) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painter = painterResource(id = spv.icon()), contentDescription = "SPV")
                    Text(
                        text = stringResource(id = spv.title()),
                        style = bodySmall,
                        color = whiteMedium
                    )
                }
            }

            val icons = amounts.map {
                it.assetId.assetIcon(
                    session = viewModel.sessionOrNull,
                    isLightning = viewModel.account.isLightning
                )
            }

            TransactionStatusIcon(
                modifier = Modifier.align(Alignment.Center),
                transactionStatus = status,
                icons = icons,
                isSwap = viewModel.type.value == Transaction.Type.MIXED
            )
        }

        val type by viewModel.type.collectAsStateWithLifecycle()
        val message: String = when {
            status is Failed -> "id_your_transaction_failed_s|${(status as Failed).error}"
            type == Transaction.Type.IN -> "id_your_transaction_was_successfully_received"
            type == Transaction.Type.OUT || type == Transaction.Type.REDEPOSIT -> "id_your_transaction_was_successfully_sent"
            type == Transaction.Type.MIXED -> "id_your_transaction_was_successfully_swapped"
            else -> ""
        }


        val createdAtTs by viewModel.createdAtTs.collectAsStateWithLifecycle()

        GreenColumn(
            space = 8,
            padding = 0,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResourceId(id = status.title()), style = headlineSmall)
            Text(text = stringResourceId(message), style = bodyMedium)


            createdAtTs.takeIf { it > 0 }?.let { Date(it / 1000).formatFullWithTime() }?.also {
                Text(text = it, style = bodyMedium, color = whiteMedium)
            }

            val typeRes = when  {
                status is Unconfirmed && type == Transaction.Type.OUT -> R.string.id_outgoing
                status is Unconfirmed && type == Transaction.Type.IN -> R.string.id_incoming
                status is Unconfirmed && type == Transaction.Type.REDEPOSIT -> R.string.id_redeposit
                type == Transaction.Type.OUT -> R.string.id_sent
                type == Transaction.Type.REDEPOSIT -> R.string.id_redeposited
                type == Transaction.Type.MIXED -> R.string.id_swap
                else -> R.string.id_received
            }

            Text(
                text = stringResource(id = typeRes), style = labelMedium, modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(status.color())
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }


        if (amounts.isNotEmpty()) {
            GreenAmount(
                amounts = amounts,
                session = viewModel.sessionOrNull,
                modifier = Modifier.padding(vertical = 32.dp),
                onAssetClick = {
                    bottomSheetNavigator.show(
                        AssetDetailsBottomSheet(
                            assetId = it,
                            accountAsset = viewModel.accountAsset.value,
                            greenWallet = viewModel.greenWallet
                        )
                    )
                }
            )
        } else {
            GreenSpacer(32)
        }

        val fee by viewModel.fee.collectAsStateWithLifecycle()
        val feeRate by viewModel.feeRate.collectAsStateWithLifecycle()
        val address by viewModel.address.collectAsStateWithLifecycle()
        val transactionId by viewModel.transactionId.collectAsStateWithLifecycle()
        val note by viewModel.note.collectAsStateWithLifecycle()

        if(listOfNotNull(fee, feeRate, address, transactionId, note).isNotEmpty()) {
            HorizontalDivider()

            GreenColumn(space = 8, padding = 0, modifier = Modifier.padding(vertical = 32.dp)) {
                fee?.also {
                    Detail(label = R.string.id_network_fees) {
                        Text(text = it)
                    }
                }

                feeRate?.also {
                    Detail(label = R.string.id_fee_rate) {
                        Text(text = it)
                    }
                }

                address?.also {
                    Detail(label = if (type == Transaction.Type.IN) R.string.id_received_on else R.string.id_send_to) {
                        CopyContainer(value = it) { GreenAddress(address = it) }
                    }
                }

                transactionId?.also {
                    Detail(label = R.string.id_transaction_id) {
                        CopyContainer(value = it) { Text(text = it, fontFamily = monospaceFont) }
                    }
                }

                AnimatedNullableVisibility(value = note) {
                    Detail(label = R.string.id_note) {
                        CopyContainer(value = it) {
                            Text(it)
                        }
                    }
                }
            }
        }

        val canReplaceByFee by viewModel.canReplaceByFee.collectAsStateWithLifecycle()
        Column {
            if (canReplaceByFee) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(id = R.string.id_speed_up_transaction),
                    painterResource(id = R.drawable.gauge)
                ) {
                    viewModel.postEvent(TransactionViewModel.LocalEvents.BumpFee)
                }
            }

            if (!viewModel.account.isLightning) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(id = if (note.isNullOrBlank()) R.string.id_add_note else R.string.id_edit_note),
                    painterResource(id = R.drawable.pencil_simple_line)
                ) {
                    bottomSheetNavigator.show(
                        NoteBottomSheet(
                            note = viewModel.note.value ?: "",
                            isLightning = viewModel.account.isLightning,
                            greenWallet = viewModel.greenWallet
                        )
                    )
                }

                HorizontalDivider()
                MenuListItem(
                    stringResource(id = R.string.id_view_in_explorer),
                    painterResource(id = R.drawable.binoculars)
                ) {
                    viewModel.postEvent(TransactionViewModel.LocalEvents.ViewInBlockExplorer)
                }

                HorizontalDivider()
                MenuListItem(
                    stringResource(id = R.string.id_share_transaction),
                    painterResource(id = R.drawable.export)
                ) {
                    viewModel.postEvent(TransactionViewModel.LocalEvents.ShareTransaction())
                }
            }
            val transaction by viewModel.transaction.collectAsStateWithLifecycle()

            if (transaction.isRefundableSwap) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(id = R.string.id_initiate_refund),
                    painterResource(id = R.drawable.arrow_u_left_down)
                ) {
                     viewModel.postEvent(TransactionViewModel.LocalEvents.RecoverFunds)
                }
            }

            val hasMoreDetails by viewModel.hasMoreDetails.collectAsStateWithLifecycle()

            if(hasMoreDetails) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(id = R.string.id_more_details),
                    painterResource(id = R.drawable.magnifying_glass)
                ) {
                    bottomSheetNavigator.show(
                        TransactionDetailsBottomSheet(
                            transaction = viewModel.transaction.value,
                            greenWallet = viewModel.greenWallet
                        )
                    )
                }
            }
        }

    }
}

@Composable
private fun Detail(label: Int, content: @Composable () -> Unit) {
    GreenRow(
        padding = 0, space = 8, verticalAlignment = Alignment.Top,
    ) {
        Text(stringResource(id = label), color = whiteMedium, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.weight(2f)) {
            content()
        }
    }
}

@Composable
private fun MenuListItem(text: String, painter: Painter, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(green)
        )
        Text(text, color = green, style = bodyLarge)
    }
}

object TransactionViewModelPreviewProvider : PreviewParameterProvider<TransactionViewModelPreview> {
    override val values = sequenceOf(
        TransactionViewModelPreview.previewUnconfirmed(),
        TransactionViewModelPreview.previewConfirmed(),
        TransactionViewModelPreview.previewCompleted(),
        TransactionViewModelPreview.previewFailed()
    )
}

@Composable
@Preview
fun TransactionScreenPreview(
    //@PreviewParameter(TransactionViewModelPreviewProvider::class)
//    viewModel: TransactionViewModelPreview
) {
    GreenPreview {
        TransactionScreen(viewModel = TransactionViewModelPreview.previewCompleted())
    }
}

@Composable
@Preview
fun TransactionScreenPreviewConfirmed() {
    GreenPreview {
        TransactionScreen(viewModel = TransactionViewModelPreview.previewConfirmed())
    }
}

@Composable
@Preview
fun TransactionScreenPreviewFailed() {
    GreenPreview {
        TransactionScreen(viewModel = TransactionViewModelPreview.previewFailed())
    }
}

@Composable
@Preview
fun MenuPreview() {
    GreenThemePreview {
        Column {
            HorizontalDivider()
            MenuListItem("Add Note", painterResource(id = R.drawable.pencil_simple_line))
            HorizontalDivider()
            MenuListItem("Share Transaction", painterResource(id = R.drawable.export))
            HorizontalDivider()
            MenuListItem("Initiate Refund", painterResource(id = R.drawable.arrow_u_left_down))
            HorizontalDivider()
            MenuListItem("More Details", painterResource(id = R.drawable.magnifying_glass))
        }
    }
}