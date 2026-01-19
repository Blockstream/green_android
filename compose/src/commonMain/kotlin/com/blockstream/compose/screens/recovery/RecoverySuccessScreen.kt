package com.blockstream.compose.screens.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_done
import blockstream_green.common.generated.resources.id_great_you_successfully_backed
import blockstream_green.common.generated.resources.id_keep_your_recovery_phrase_fully
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.models.recovery.RecoverySuccessViewModelAbstract
import com.blockstream.compose.models.recovery.RecoverySuccessViewModelPreview
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun RecoverySuccessScreen(
    viewModel: RecoverySuccessViewModelAbstract,
) {
    SetupScreen(viewModel = viewModel) {
        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Rive(
                riveAnimation = RiveAnimation.ROCKET,
            )

            GreenSpacer(24)

            Text(
                text = stringResource(Res.string.id_great_you_successfully_backed),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            GreenSpacer(12)

            Text(
                text = stringResource(Res.string.id_keep_your_recovery_phrase_fully),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        GreenButton(
            text = stringResource(Res.string.id_done),
            size = GreenButtonSize.BIG,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            onClick = {
                viewModel.done()
            }
        )
    }
}

@Composable
@Preview
fun RecoverySuccessScreenPreview() {
    GreenPreview {
        RecoverySuccessScreen(viewModel = RecoverySuccessViewModelPreview.preview())
    }
}

