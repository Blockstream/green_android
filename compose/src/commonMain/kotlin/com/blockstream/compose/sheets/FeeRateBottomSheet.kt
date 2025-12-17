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
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.CaretRight
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.title
import com.blockstream.compose.models.send.FeeViewModelAbstract
import com.blockstream.compose.models.send.FeeViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.roundBackground
import com.blockstream.data.data.FeePriority
import com.blockstream.data.utils.feeRateWithUnit
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        uiState.feePriorities.forEach { feePriority ->
            FeeItem(feePriority = feePriority, isFeeRateOnly = uiState.isFeeRateOnly, onProgress = onProgress, onClick = {
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
fun FeeItem(feePriority: FeePriority, isFeeRateOnly: Boolean = false, onProgress: Boolean = false, onClick: () -> Unit = {}) {
    GreenCard(onClick = onClick, helperText = feePriority.error.takeIf { !isFeeRateOnly }, enabled = feePriority.enabled || isFeeRateOnly) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .appTestTag(feePriority.toString())
        ) {
            Column {
                GreenRow(padding = 0, space = 6, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(feePriority.title), style = titleSmall, color = whiteHigh)

                    if (!isFeeRateOnly) {
                        AnimatedNullableVisibility(feePriority.expectedConfirmationTime) {
                            Text(
                                it,
                                style = bodyMedium,
                                color = whiteMedium,
                                modifier = Modifier.roundBackground(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (isFeeRateOnly) {
                    AnimatedNullableVisibility(feePriority.expectedConfirmationTime) {
                        Text(
                            it,
                            style = bodyMedium,
                            color = whiteMedium
                        )
                    }
                } else {
                    Text(feePriority.feeRate ?: "", style = bodySmall, color = whiteMedium)
                }
            }


            GreenRow(padding = 0, space = 12) {
                if (onProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp),
                        strokeWidth = 2.dp
                    )
                }

                AnimatedVisibility(visible = !isFeeRateOnly && !onProgress && feePriority.fee != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(feePriority.fee ?: "", style = labelMedium, color = whiteMedium)
                        Text(feePriority.feeFiat ?: "", style = bodyMedium, color = whiteMedium)
                    }
                }

                AnimatedVisibility(visible = isFeeRateOnly && !onProgress) {
                    Text(feePriority.feeRate ?: "", style = labelLarge, color = whiteMedium)
                }

                CaretRight(enabled = feePriority.enabled)
            }
        }
    }
}

@Composable
@Preview
fun FeeItemPreview() {
    GreenChromePreview {
        GreenColumn {
            FeeItem(
                FeePriority.High(
                    fee = "0,0001235 BTC",
                    feeFiat = "~ 45,42 USD",
                    feeRate = 2345L.feeRateWithUnit(),
                    expectedConfirmationTime = "~ 10 Minutes"
                )
            )

            FeeItem(
                FeePriority.Medium(
                    fee = "0,0000235 BTC",
                    feeFiat = "~ 40,42 USD",
                    feeRate = 1234L.feeRateWithUnit(),
                    error = "id_insufficient_funds",
                    expectedConfirmationTime = "~ 30 Minutes"
                )
            )

            FeeItem(
                FeePriority.Medium(
                    fee = "0,0000235 BTC",
                    feeFiat = "~ 40,42 USD",
                    feeRate = 1234L.feeRateWithUnit(),
                    error = "id_insufficient_funds",
                    expectedConfirmationTime = "~ 30 Minutes"
                ), onProgress = true
            )

            FeeItem(
                FeePriority.High(
                    feeRate = 1234L.feeRateWithUnit(),
                    expectedConfirmationTime = "~ 30 Minutes",
                ), isFeeRateOnly = true, onProgress = true
            )

            FeeItem(
                FeePriority.High(
                    feeRate = 1234L.feeRateWithUnit(),
                    expectedConfirmationTime = "~ 30 Minutes",
                ), isFeeRateOnly = true
            )

            FeeItem(
                FeePriority.High(), isFeeRateOnly = true
            )
        }
    }
}

@Composable
@Preview
fun FeeRateBottomSheetPreview() {
    GreenPreview {
        FeeRateBottomSheet(
            viewModel = FeeViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}