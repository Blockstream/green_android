package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_empty_lightning_account
import blockstream_green.common.generated.resources.id_node_info
import blockstream_green.common.generated.resources.id_rescan_swaps
import blockstream_green.common.generated.resources.id_share_logs
import blockstream_green.common.generated.resources.id_show_recovery_phrase
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.sheets.LightningNodeViewModel
import com.blockstream.common.models.sheets.LightningNodeViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.compose.views.DataListItem
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class LightningNodeBottomSheet(
    val greenWallet: GreenWallet,
) : BottomScreen(), Parcelable {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LightningNodeViewModel> {
            parametersOf(greenWallet)
        }

        LightningNodeBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightningNodeBottomSheet(
    viewModel: LightningNodeViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_node_info),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
        val data by viewModel.data.collectAsStateWithLifecycle()
        val showEmptyAccount by viewModel.showEmptyAccount.collectAsStateWithLifecycle()

        Box {

            if(data.isNotEmpty()) {
                GreenColumn(
                    padding = 0, space = 16, modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(bottom = 16.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    data.forEachIndexed { index, pair ->
                        DataListItem(
                            title = pair.first,
                            data = pair.second,
                            withDivider = index < data.size - 1
                        )
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_show_recovery_phrase),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !onProgress
                    ) {
                        viewModel.postEvent(LightningNodeViewModel.LocalEvents.ShowRecoveryPhrase)
                    }

                    if (showEmptyAccount) {
                        GreenButton(
                            text = stringResource(Res.string.id_empty_lightning_account),
                            type = GreenButtonType.OUTLINE,
                            enabled = !onProgress,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.postEvent(LightningNodeViewModel.LocalEvents.EmptyAccount)
                        }
                    }

                    GreenRow(
                        padding = 0,
                        space = 16,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GreenButton(
                            text = stringResource(Res.string.id_rescan_swaps),
                            type = GreenButtonType.TEXT,
                            enabled = !onProgress,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.postEvent(LightningNodeViewModel.LocalEvents.RescanSwaps)
                        }

                        GreenButton(
                            text = stringResource(Res.string.id_share_logs),
                            type = GreenButtonType.TEXT,
                            enabled = !onProgress,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.postEvent(LightningNodeViewModel.LocalEvents.ShareLogs)
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = onProgress,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}