package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_because_lightning
import blockstream_green.common.generated.resources.id_lightning_funding_fees
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightningFeeInfoBottomSheet(
    onLearnMoreClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_lightning_funding_fees),
        viewModel = null,
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(
            padding = 0,
            space = 24,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.id_because_lightning),
                style = bodyMedium,
                color = whiteMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LearnMoreButton {
                    onLearnMoreClick()
                }
            }
        }
    }
}