package com.blockstream.compose.screens.send

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.send.SendChooseAssetViewModelAbstract
import com.blockstream.common.models.send.SendChooseAssetViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendChooseAssetScreen(
    viewModel: SendChooseAssetViewModelAbstract
) {

    val assets = viewModel.assets

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
                GreenAsset(assetBalance = AssetBalance.create(asset), session = viewModel.sessionOrNull) {
                    viewModel.selectAsset(asset)
                }
            }
        }
    }
}

@Composable
@Preview
fun SendChooseAssetScreenPreview() {
    GreenPreview {
        SendChooseAssetScreen(viewModel = SendChooseAssetViewModelPreview.preview())
    }
}

