package com.blockstream.compose.screens.receive

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.compose.models.receive.ReceiveChooseAssetViewModelAbstract
import com.blockstream.compose.models.receive.ReceiveChooseAssetViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.screens.receive.components.LightningReadyBadge
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReceiveChooseAssetScreen(
    viewModel: ReceiveChooseAssetViewModelAbstract
) {

    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val isSwapsEnabled by viewModel.isSwapsEnabled.collectAsStateWithLifecycle()

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
    ) {

        GreenColumn(
            space = 4, modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            assets.forEach { asset ->
                val showLightningBadge = isSwapsEnabled && asset.isLiquidPolicyAsset(viewModel.session)

                GreenAsset(
                    assetBalance = AssetBalance.create(asset),
                    session = viewModel.sessionOrNull,
                    onClick = { viewModel.selectAsset(asset) },
                    trailingContent = if (showLightningBadge) {
                        { LightningReadyBadge() }
                    } else null
                )
            }
        }
    }
}

@Composable
@Preview
fun ReceiveChooseAssetScreenPreview() {
    GreenPreview {
        ReceiveChooseAssetScreen(viewModel = ReceiveChooseAssetViewModelPreview.preview())
    }
}

