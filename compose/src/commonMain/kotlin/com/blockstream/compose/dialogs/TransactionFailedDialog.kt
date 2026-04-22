package com.blockstream.compose.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.failure_x
import blockstream_green.common.generated.resources.id_copy
import blockstream_green.common.generated.resources.id_ok
import blockstream_green.common.generated.resources.id_transaction_failed
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_dialogSurface
import com.blockstream.compose.theme.md_theme_surface
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.stringResourceFromId
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TransactionFailedDialog(
    message: String,
    onDismissRequest: () -> Unit,
) {
    val platformManager = LocalPlatformManager.current
    val resolvedMessage = stringResourceFromId(message)

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
                        painter = painterResource(Res.drawable.failure_x),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${stringResource(Res.string.id_transaction_failed)}!",
                            style = titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                            ),
                            color = whiteHigh,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 70.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(md_theme_surface)
                                .padding(12.dp),
                        ) {
                            Text(
                                text = resolvedMessage,
                                style = bodyMedium.copy(
                                    fontFamily = MonospaceFont(),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                ),
                                color = whiteMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    platformManager.copyToClipboard(content = resolvedMessage)
                                },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Copy,
                                    contentDescription = stringResource(Res.string.id_copy),
                                    tint = whiteHigh,
                                )
                            }
                        }
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_ok),
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismissRequest,
                    )
                }
            }
        }
    }
}
