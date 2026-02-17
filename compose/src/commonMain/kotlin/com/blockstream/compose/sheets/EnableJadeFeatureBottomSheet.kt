package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enable_swaps
import blockstream_green.common.generated.resources.id_get_more_out_of_jade
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_not_now
import blockstream_green.common.generated.resources.id_unlock_swaps_from_this_wallet
import blockstream_green.common.generated.resources.swap_ln_lbtc
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.utils.SwapUtils
import com.blockstream.data.Urls
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
            modifier =
                Modifier.align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp),
            painter = painterResource(Res.drawable.swap_ln_lbtc),
            contentDescription = null
        )

        GreenButton(
            text = stringResource(Res.string.id_enable_swaps),
            type = GreenButtonType.COLOR,
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                SwapUtils.navigateToDeviceScanOrJadeQr(viewModel)
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
                 viewModel.postEvent(Events.OpenBrowser(Urls.HELP_JADE_SWAP))
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