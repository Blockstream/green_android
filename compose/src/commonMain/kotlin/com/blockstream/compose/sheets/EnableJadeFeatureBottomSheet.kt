package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.blockstream_jade_device
import blockstream_green.common.generated.resources.id_enable_swaps
import blockstream_green.common.generated.resources.id_get_more_out_of_jade
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_not_now
import blockstream_green.common.generated.resources.id_unlock_swaps_from_this_wallet
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableJadeFeatureBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_get_more_out_of_jade),
        subtitle = stringResource(Res.string.id_unlock_swaps_from_this_wallet),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = onDismissRequest
    ) {

        Image(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            imageVector = vectorResource(Res.drawable.blockstream_jade_device),
            contentDescription = "Jade device"
        )

        GreenButton(
            text = stringResource(Res.string.id_enable_swaps),
            type = GreenButtonType.COLOR,
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                NavigateDestinations.EnableJadeFeature.setResult(true)
                onDismissRequest()
            }
        )

        GreenButton(
            text = stringResource(Res.string.id_not_now),
            type = GreenButtonType.OUTLINE,
            color = GreenButtonColor.GREENER,
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onDismissRequest()
            }
        )

        GreenButton(
            text = stringResource(Res.string.id_learn_more),
            type = GreenButtonType.TEXT,
            color = GreenButtonColor.GREEN,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // viewModel.postEvent(Events.OpenBrowser(Urls.JADE_TROUBLESHOOT))
            }
        )
    }
}

@Composable
@Preview
fun EnableJadeFeatureBottomSheetPreview() {
    GreenPreview {
        EnableJadeFeatureBottomSheet(
            viewModel = GreenViewModel.preview(),
            onDismissRequest = { }
        )
    }
}