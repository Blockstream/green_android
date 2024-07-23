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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_u_left_down
import blockstream_green.common.generated.resources.binoculars
import blockstream_green.common.generated.resources.export
import blockstream_green.common.generated.resources.gauge
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_closed_channel
import blockstream_green.common.generated.resources.id_confidential_transaction
import blockstream_green.common.generated.resources.id_edit_note
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_incoming
import blockstream_green.common.generated.resources.id_initiate_refund
import blockstream_green.common.generated.resources.id_more_details
import blockstream_green.common.generated.resources.id_network_fees
import blockstream_green.common.generated.resources.id_non_confidential_transaction
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_outgoing
import blockstream_green.common.generated.resources.id_received
import blockstream_green.common.generated.resources.id_received_on
import blockstream_green.common.generated.resources.id_redeposit
import blockstream_green.common.generated.resources.id_redeposited
import blockstream_green.common.generated.resources.id_send_to
import blockstream_green.common.generated.resources.id_sent
import blockstream_green.common.generated.resources.id_share
import blockstream_green.common.generated.resources.id_share_transaction
import blockstream_green.common.generated.resources.id_speed_up_transaction
import blockstream_green.common.generated.resources.id_swap
import blockstream_green.common.generated.resources.id_swap_was_successfully_executed
import blockstream_green.common.generated.resources.id_the_transaction_was
import blockstream_green.common.generated.resources.id_total_spent
import blockstream_green.common.generated.resources.id_transaction_id
import blockstream_green.common.generated.resources.id_unblinding_data
import blockstream_green.common.generated.resources.id_view_in_explorer
import blockstream_green.common.generated.resources.id_your_transaction_failed_s
import blockstream_green.common.generated.resources.id_your_transaction_was
import blockstream_green.common.generated.resources.magnifying_glass
import blockstream_green.common.generated.resources.pencil_simple_line
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.looks.transaction.Failed
import com.blockstream.common.looks.transaction.Unconfirmed
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.common.models.transaction.TransactionViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAmounts
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.TransactionStatusIcon
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.color
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.extensions.title
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.MenuBottomSheet
import com.blockstream.compose.sheets.MenuEntry
import com.blockstream.compose.sheets.NoteBottomSheet
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
data class TransactionScreen(val transaction: Transaction, val greenWallet: GreenWallet) : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TransactionViewModel> {
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
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

    NoteBottomSheet.getResult {
        viewModel.postEvent(TransactionViewModel.LocalEvents.SetNote(it))
    }

    MenuBottomSheet.getResult {
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
        Res.drawable.pencil_simple_line
        if (sideEffect is TransactionViewModel.LocalSideEffects.SelectLiquidShareTransaction) {
            bottomSheetNavigator?.show(
                MenuBottomSheet(
                    title = getString(Res.string.id_share), entries = listOf(
                        MenuEntry(
                            title = getString(Res.string.id_confidential_transaction),
                            iconRes = "eye_slash"
                        ),
                        MenuEntry(
                            title = getString(Res.string.id_non_confidential_transaction),
                            iconRes = "eye"
                        ),
                        MenuEntry(
                            title = getString(Res.string.id_unblinding_data),
                            iconRes = "code_block"
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
                    Image(painter = painterResource(spv.icon()), contentDescription = "SPV")
                    Text(
                        text = stringResource(spv.title()),
                        style = bodyMedium,
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
        val isCloseChannel by viewModel.isCloseChannel.collectAsStateWithLifecycle()
        val message: String = when {
            status is Failed -> stringResource(Res.string.id_your_transaction_failed_s, (status as Failed).error)
            type == Transaction.Type.IN -> stringResource(Res.string.id_the_transaction_was)
            type == Transaction.Type.OUT || type == Transaction.Type.REDEPOSIT -> stringResource(Res.string.id_your_transaction_was)
            type == Transaction.Type.MIXED -> stringResource(Res.string.id_swap_was_successfully_executed)
            else -> {
                ""
            }
        }


        val createdAt by viewModel.createdAt.collectAsStateWithLifecycle()

        GreenColumn(
            space = 8,
            padding = 0,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = status.title(), style = headlineSmall)
            Text(text = message, style = bodyMedium)


            createdAt?.also {
                Text(text = it, style = bodyMedium, color = whiteMedium)
            }

            val typeRes = when  {
                status is Unconfirmed && type == Transaction.Type.OUT -> Res.string.id_outgoing
                status is Unconfirmed && type == Transaction.Type.IN -> Res.string.id_incoming
                status is Unconfirmed && type == Transaction.Type.REDEPOSIT -> Res.string.id_redeposit
                isCloseChannel -> Res.string.id_closed_channel
                type == Transaction.Type.OUT -> Res.string.id_sent
                type == Transaction.Type.REDEPOSIT -> Res.string.id_redeposited
                type == Transaction.Type.MIXED -> Res.string.id_swap
                else -> Res.string.id_received
            }

            Text(
                text = stringResource(typeRes), style = labelMedium, modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(status.color())
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }


        if (amounts.isNotEmpty()) {
            GreenAmounts(
                amounts = amounts,
                session = viewModel.sessionOrNull,
                modifier = Modifier.padding(vertical = 32.dp),
                onAssetClick = {
                    viewModel.postEvent(
                        NavigateDestinations.AssetDetails(
                            assetId = it,
                            accountAsset = viewModel.accountAsset.value
                        )
                    )
                }
            )
        } else {
            GreenSpacer(32)
        }

        val fee by viewModel.fee.collectAsStateWithLifecycle()
        val feeRate by viewModel.feeRate.collectAsStateWithLifecycle()
        val total by viewModel.total.collectAsStateWithLifecycle()
        val totalFiat by viewModel.totalFiat.collectAsStateWithLifecycle()
        val address by viewModel.address.collectAsStateWithLifecycle()
        val transactionId by viewModel.transactionId.collectAsStateWithLifecycle()
        val note by viewModel.note.collectAsStateWithLifecycle()
        val canEditNote by viewModel.canEditNote.collectAsStateWithLifecycle()

        if(listOfNotNull(fee, feeRate, address, transactionId, note).isNotEmpty()) {
            HorizontalDivider()

            GreenColumn(space = 8, padding = 0, modifier = Modifier.padding(vertical = 32.dp)) {
                fee?.also {
                    Detail(label = Res.string.id_network_fees) {
                        Text(text = it)
                    }
                }

                feeRate?.also {
                    Detail(label = Res.string.id_fee_rate) {
                        Text(text = it)
                    }
                }

                address?.also {
                    Detail(label = if (type == Transaction.Type.IN) Res.string.id_received_on else Res.string.id_send_to) {
                        GreenAddress(address = it)
                    }
                }

                transactionId?.also {
                    Detail(label = Res.string.id_transaction_id) {
                        CopyContainer(value = it) { Text(text = it, fontFamily = MonospaceFont()) }
                    }
                }

                AnimatedNullableVisibility(value = note) {
                    Detail(label = Res.string.id_note) {
                        CopyContainer(value = it) {
                            Text(it)
                        }
                    }
                }
            }

            total?.also {
                HorizontalDivider()

                Detail(label = Res.string.id_total_spent, labelColor = whiteHigh, labelStyle = labelLarge, modifier = Modifier.padding(top = 8.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = it, style = labelLarge, color = whiteHigh, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        totalFiat?.also {
                            Text(text = it, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                GreenSpacer(32)
            }
        }

        val canReplaceByFee by viewModel.canReplaceByFee.collectAsStateWithLifecycle()
        Column {
            if (canReplaceByFee) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(Res.string.id_speed_up_transaction),
                    painterResource(Res.drawable.gauge)
                ) {
                    viewModel.postEvent(TransactionViewModel.LocalEvents.BumpFee)
                }
            }

            if (!viewModel.account.isLightning) {
                if(canEditNote) {
                    HorizontalDivider()
                    MenuListItem(
                        stringResource(if (note.isNullOrBlank()) Res.string.id_add_note else Res.string.id_edit_note),
                        painterResource(Res.drawable.pencil_simple_line)
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.Note(
                                note = viewModel.note.value ?: "",
                                noteType = NoteType.Note
                            )
                        )
                    }
                }

                HorizontalDivider()
                MenuListItem(
                    stringResource(Res.string.id_view_in_explorer),
                    painterResource(Res.drawable.binoculars)
                ) {
                    viewModel.postEvent(TransactionViewModel.LocalEvents.ViewInBlockExplorer)
                }

                HorizontalDivider()
                MenuListItem(
                    stringResource(Res.string.id_share_transaction),
                    painterResource(Res.drawable.export)
                ) {
                    viewModel.postEvent(TransactionViewModel.LocalEvents.ShareTransaction())
                }
            }
            val transaction by viewModel.transaction.collectAsStateWithLifecycle()

            if (transaction.isRefundableSwap) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(Res.string.id_initiate_refund),
                    painterResource(Res.drawable.arrow_u_left_down)
                ) {
                     viewModel.postEvent(TransactionViewModel.LocalEvents.RecoverFunds)
                }
            }

            val hasMoreDetails by viewModel.hasMoreDetails.collectAsStateWithLifecycle()

            if(hasMoreDetails) {
                HorizontalDivider()
                MenuListItem(
                    stringResource(Res.string.id_more_details),
                    painterResource(Res.drawable.magnifying_glass)
                ) {
                    viewModel.postEvent(
                        NavigateDestinations.TransactionDetails(
                            transaction = transaction
                        )
                    )
                }
            }
        }

    }
}

@Composable
private fun Detail(
    modifier: Modifier = Modifier,
    label: StringResource,
    labelColor: Color = whiteMedium,
    labelStyle: TextStyle = bodyLarge,
    content: @Composable () -> Unit
) {
    GreenRow(
        padding = 0, space = 8, verticalAlignment = Alignment.Top, modifier = modifier
    ) {
        Text(stringResource(label), color = labelColor, style = labelStyle, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.weight(2f)) {
            content()
        }
    }
}

@Composable
internal fun MenuListItem(text: String, painter: Painter, onClick: () -> Unit = {}) {
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