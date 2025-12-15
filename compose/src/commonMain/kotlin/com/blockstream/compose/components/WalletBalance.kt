package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.eye_slash
import blockstream_green.common.generated.resources.id_total_bitcoin_balance
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.compose.models.overview.IWalletBalance
import com.blockstream.compose.models.overview.WalletBalanceViewModel.LocalEvents
import com.blockstream.compose.theme.textHigh
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.utils.appTestTag
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WalletBalance(modifier: Modifier = Modifier, viewModel: IWalletBalance) {
    Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(Res.string.id_total_bitcoin_balance), color = textMedium)
            val hideAmounts by viewModel.hideAmounts.collectAsStateWithLifecycle()
            Icon(
                painter = painterResource(if (hideAmounts) Res.drawable.eye_slash else Res.drawable.eye),
                contentDescription = null,
                modifier = Modifier
                    .appTestTag("eye_button")
                    .noRippleClickable {
                        viewModel.postEvent(LocalEvents.ToggleHideAmounts)
                    }
                    .padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically)
            )
        }

        Box {
            val balancePrimary by viewModel.balancePrimary.collectAsStateWithLifecycle()
            Column(modifier = Modifier.noRippleClickable {
                viewModel.postEvent(LocalEvents.ToggleBalance)
            }) {
                Text(
                    text = balancePrimary.takeIf { it.isNotBlank() } ?: " ",
                    color = textHigh,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.appTestTag("total_balance")
                )
//                val balanceSecondary by viewModel.balanceSecondary.collectAsStateWithLifecycle()
//                Text(text = balanceSecondary.takeIf { it.isNotBlank() } ?: " ",
//                    color = textMedium,
//                    style = bodyLarge
//                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = balancePrimary == null,
                modifier = Modifier.align(Alignment.Center)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .height(1.dp)
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
