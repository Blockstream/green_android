package com.blockstream.compose.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_custom
import blockstream_green.common.generated.resources.id_network_fee
import com.blockstream.common.data.FeePriority
import com.blockstream.compose.components.GreenArrow
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.models.send.FeeViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.roundBackground
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeRateBottomSheet(
    viewModel: FeeViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_network_fee),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = onDismissRequest
    ) {

        val feePriorities by viewModel.feePriorities.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        feePriorities.forEach { feePriority ->
            FeeItem(feePriority = feePriority, onProgress = onProgress, onClick = {
                NavigateDestinations.FeeRate.setResult(feePriority)
                onDismissRequest()
            })
        }

        GreenButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.id_custom),
            type = GreenButtonType.TEXT
        ) {
            NavigateDestinations.FeeRate.setResult(FeePriority.Custom())
            onDismissRequest()
        }
    }
}

@Composable
fun FeeItem(feePriority: FeePriority, onProgress: Boolean = false, onClick: () -> Unit = {}) {
    GreenCard(onClick = onClick, helperText = feePriority.error, enabled = feePriority.enabled) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .appTestTag(feePriority.toString())
        ) {
            Column {
                GreenRow(padding = 0, space = 6, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(feePriority.title), style = titleSmall, color = whiteHigh)

                    AnimatedNullableVisibility(feePriority.expectedConfirmationTime) {
                        Text(
                            it,
                            style = bodyMedium,
                            color = whiteMedium,
                            modifier = Modifier.roundBackground(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(feePriority.feeRate ?: "", style = bodySmall, color = whiteMedium)
            }


            GreenRow(padding = 0, space = 12) {
                if (onProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp),
                        strokeWidth = 2.dp
                    )
                }

                AnimatedVisibility(visible = !onProgress && feePriority.fee != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(feePriority.fee ?: "", style = labelMedium, color = whiteMedium)
                        Text(feePriority.feeFiat ?: "", style = bodyMedium, color = whiteMedium)
                    }
                }
                GreenArrow(enabled = feePriority.enabled)
            }
        }
    }
}
