package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.looks.transaction.Confirmed
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.compose.R
import com.blockstream.compose.extensions.directionColor
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.md_theme_inverseSurface
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.formatAuto
import com.blockstream.compose.utils.stringResourceId
import java.util.Date

@Composable
fun GreenTransaction(
    modifier: Modifier = Modifier,
    transactionLook: TransactionLook,
    showAccount: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .then(modifier)
    ) {
        Box {

            val status = transactionLook.status
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {

                when (transactionLook.transaction.txType) {
                    Transaction.Type.IN -> R.drawable.arrow_line_down
                    Transaction.Type.OUT -> R.drawable.arrow_line_up
                    Transaction.Type.REDEPOSIT -> R.drawable.arrow_u_left_down
                    Transaction.Type.MIXED -> R.drawable.arrows_down_up
                    Transaction.Type.UNKNOWN -> R.drawable.question
                }.let {
                    painterResource(id = it)
                }.also {
                    Image(
                        painter = it,
                        contentDescription = null
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        GreenRow(
                            padding = 0,
                            space = 2,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResourceId(id = transactionLook.directionText),
                                style = labelLarge,
                                lineHeight = 1.sp
                            )

                            if(!transactionLook.transaction.spv.disabledOrUnconfirmedOrVerified()){
                                Image(
                                    painter = painterResource(id = transactionLook.transaction.spv.icon()),
                                    contentDescription = "SPV",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            transactionLook.assets.forEachIndexed { index, asset ->
                                Text(
                                    text = asset,
                                    style = labelLarge,
                                    textAlign = TextAlign.End,
                                    color = transactionLook.directionColor(index)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        AnimatedVisibility(visible = status.onProgress) {

                            val isRefundableSwap = transactionLook.transaction.isRefundableSwap
                            if (status.onProgress || isRefundableSwap) {
                                val text = if (isRefundableSwap) {
                                    stringResource(R.string.id_refundable)
                                } else if (status is Confirmed) {
                                    stringResource(
                                        if (status.confirmationsRequired > 2) R.string.id_d6_confirmations else R.string.id_12_confirmations,
                                        status.confirmations
                                    )
                                } else {
                                    stringResource(R.string.id_unconfirmed)
                                }

                                val bgColor = when {
                                    isRefundableSwap -> red
                                    status is Confirmed -> md_theme_inverseSurface
                                    else -> orange
                                }

                                Text(
                                    text = text, style = bodySmall, modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .background(bgColor)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )

                            }

                        }

                        AnimatedVisibility(visible = !status.onProgress) {
                            // Date
                            Text(
                                text = transactionLook.transaction.createdAtInstant.toEpochMilliseconds()
                                    .takeIf { it > 0 }?.let { Date(it).formatAuto() } ?: "",
                                style = labelSmall,
                                color = whiteMedium
                            )
                        }

                        // Account
                        if (showAccount) {
                            Text(
                                text = transactionLook.transaction.account.name,
                                style = bodySmall,
                                color = whiteMedium
                            )
                        }
                    }

                    if (transactionLook.transaction.memo.isNotBlank()) {
                        // Memo
                        Text(
                            text = transactionLook.transaction.memo,
                            style = bodySmall,
                            color = whiteMedium
                        )
                    }

                }
            }

//            transactionLook
            // transaction.isLoadingTransaction


            androidx.compose.animation.AnimatedVisibility(
                visible = status.onProgress,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (status is Confirmed) {
                    LinearProgressIndicator(progress = {
                        (status.confirmations / status.confirmationsRequired).toFloat()
                    }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
@Preview
fun GreenTransactionPreview() {
    GreenThemePreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, space = 1, padding = 0) {
            GreenTransaction(transactionLook = previewTransactionLook()) {

            }

            GreenTransaction(transactionLook = previewTransactionLook(), showAccount = false) {

            }
        }
    }
}