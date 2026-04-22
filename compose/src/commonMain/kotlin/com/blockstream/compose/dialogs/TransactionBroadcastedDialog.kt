package com.blockstream.compose.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_copy
import blockstream_green.common.generated.resources.id_done
import blockstream_green.common.generated.resources.id_share_link
import blockstream_green.common.generated.resources.id_transaction_id
import blockstream_green.common.generated.resources.id_transaction_successful
import blockstream_green.common.generated.resources.id_you_transferred_s
import blockstream_green.common.generated.resources.success_tick
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.md_theme_dialogSurface
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TransactionBroadcastedDialog(
    txHash: String,
    amount: String? = null,
    onDismissRequest: () -> Unit,
    onShare: (() -> Unit)? = null,
) {
    val platformManager = LocalPlatformManager.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GreenCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            padding = 0,
            colors = CardDefaults.outlinedCardColors(containerColor = md_theme_dialogSurface),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = whiteMedium,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 64.dp, bottom = 32.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.success_tick),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(Res.string.id_transaction_successful),
                            style = titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                            ),
                            color = whiteHigh,
                            textAlign = TextAlign.Center,
                        )
                        if (!amount.isNullOrBlank()) {
                            Text(
                                text = stringResource(Res.string.id_you_transferred_s, amount),
                                style = bodyMedium,
                                color = whiteMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "${stringResource(Res.string.id_transaction_id)}:",
                                style = bodySmall,
                                color = whiteMedium,
                            )
                            IconButton(
                                onClick = {
                                    platformManager.copyToClipboard(content = txHash)
                                },
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(16.dp),
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Copy,
                                    contentDescription = stringResource(Res.string.id_copy),
                                    tint = whiteMedium,
                                )
                            }
                        }
                        Text(
                            text = txHash,
                            style = bodyMedium.copy(
                                fontFamily = MonospaceFont(),
                                fontWeight = FontWeight.Medium,
                            ),
                            color = whiteHigh,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        GreenButton(
                            text = stringResource(Res.string.id_done),
                            size = GreenButtonSize.BIG,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onDismissRequest,
                        )
                        if (onShare != null) {
                            GreenButton(
                                text = stringResource(Res.string.id_share_link),
                                type = GreenButtonType.OUTLINE,
                                color = GreenButtonColor.GREENER,
                                size = GreenButtonSize.BIG,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onShare,
                            )
                        }
                    }
                }
            }
        }
    }
}
