package com.blockstream.compose.screens.swap

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_have_a_stuck_swap
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_new_swaps_are_temporarily_disabled
import blockstream_green.common.generated.resources.id_reset_stuck_swaps
import blockstream_green.common.generated.resources.id_swaps_unavailable
import blockstream_green.common.generated.resources.swaps_unavailable
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.light.ArrowSquareOut
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.utils.appTestTag
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppButton
import com.blockstream.designsystem.components.AppLink
import com.blockstream.designsystem.components.AppText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun SwapsUnavailableV2(
    hasSwaps: Boolean,
    onProgress: Boolean,
    onResetClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .appTestTag("swaps_unavailable")
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.swaps_unavailable),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(
                    text = stringResource(Res.string.id_swaps_unavailable),
                    style = AppTheme.typography.title.mSemiBold,
                    color = AppTheme.colors.text.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                AppText(
                    text = stringResource(Res.string.id_new_swaps_are_temporarily_disabled),
                    style = AppTheme.typography.label.m,
                    color = AppTheme.colors.text.subtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasSwaps) {
                AppText(
                    text = stringResource(Res.string.id_have_a_stuck_swap),
                    style = AppTheme.typography.label.sm,
                    color = AppTheme.colors.text.subtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasSwaps) {
                    AppButton(
                        text = stringResource(Res.string.id_reset_stuck_swaps),
                        onClick = onResetClick,
                        enabled = !onProgress,
                        modifier = Modifier.appTestTag("reset_stuck_swaps").fillMaxWidth()
                    )
                }

                AppLink(
                    text = stringResource(Res.string.id_learn_more),
                    onClick = onLearnMoreClick,
                    trailingIcon = PhosphorIcons.Light.ArrowSquareOut,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = 56.dp)
                )
            }
        }
    }
}

@Composable
@Preview
fun SwapsUnavailableV2Preview() {
    GreenPreview {
        SwapsUnavailableV2(hasSwaps = true, onProgress = false, onResetClick = {}, onLearnMoreClick = {})
    }
}
