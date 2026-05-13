package com.blockstream.compose.screens.lightning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_lightning_is_in_beta
import blockstream_green.common.generated.resources.id_lightning_network
import blockstream_green.common.generated.resources.id_lightning_node
import blockstream_green.common.generated.resources.id_scaling_solution_for_faster
import blockstream_green.common.generated.resources.id_you_may_experience_bugs
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.adamglin.phosphoricons.regular.Info
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.events.Events.OpenBrowser
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.lightning.EnabledLightningViewModel
import com.blockstream.compose.models.lightning.EnabledLightningViewModelAbstract
import com.blockstream.compose.theme.GreenColors
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.appTestTag
import com.blockstream.data.Urls
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun EnabledLightningScreen(viewModel: EnabledLightningViewModelAbstract) {
    SetupScreen(
        withPadding = false,
        viewModel = viewModel,
    ) {
        val nodeId = viewModel.nodeId.collectAsStateWithLifecycle().value ?: "Unknown Node ID"

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(Res.string.id_lightning_network),
                    style = headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(Res.string.id_scaling_solution_for_faster),
                    style = bodyMedium,
                    textAlign = TextAlign.Center,
                    color = GreenColors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            OutlinedCard(
                onClick = {
                    viewModel.postEvent(EnabledLightningViewModel.LocalEvents.CopyNodeId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .appTestTag("lightning_node_card")
            ) {
                GreenRow(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.id_lightning_node),
                            style = titleSmall,
                        )
                        Text(
                            text = nodeId,
                            style = bodyMedium,
                            color = whiteMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = PhosphorIcons.Regular.Copy,
                        contentDescription = "Copy",
                        tint = whiteMedium,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GreenAlert(
                    title = stringResource(Res.string.id_lightning_is_in_beta),
                    message = stringResource(Res.string.id_you_may_experience_bugs),
                    isBlue = true,
                    icon = PhosphorIcons.Regular.Info
                )

                LearnMoreButton(
                    onClick = {
                        viewModel.postEvent(OpenBrowser(Urls.HELP_LIGHTNING_BETA))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}

@Preview
@Composable
fun EnabledLightningScreenPreview() {
    GreenPreview {
        EnabledLightningScreen(
            viewModel = EnabledLightningViewModelPreview()
        )
    }
}
private class EnabledLightningViewModelPreview : EnabledLightningViewModelAbstract(
    previewWallet()
) {
    override val nodeId = kotlinx.coroutines.flow.MutableStateFlow(
        "03c3cbdie952402938402938402938457ddvash1295"
    )
    override fun screenName(): String = "EnabledLightningPreview"
}