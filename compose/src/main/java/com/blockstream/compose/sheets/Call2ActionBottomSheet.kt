package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.sheets.AnalyticsViewModelPreview
import com.blockstream.common.models.sheets.AssetDetailsViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.HandleSideEffectDialog
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Parcelize
data class Call2ActionBottomSheet(
    val greenWallet: GreenWallet,
    val network: Network,
) : BottomScreen(), Parcelable {
    // Temp fix until fully migration to Compose
    @Transient
    @IgnoredOnParcel
    var parentViewModel: GreenViewModel? = null

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<SimpleGreenViewModel> {
            parametersOf(greenWallet)
        }.also {
            val navigator = LocalRootNavigator.current
            if(navigator == null) {
                it.parentViewModel = parentViewModel
            }
        }

        Call2ActionBottomSheet(
            viewModel = viewModel,
            network = network,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Call2ActionBottomSheet(
    viewModel: GreenViewModel,
    network: Network,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_enable_2fa),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        HandleSideEffect(viewModel = viewModel)

        Text(
            text = stringResource(id = R.string.id_2fa_isnt_set_up_yetnnyou_can),
            textAlign = TextAlign.Center
        )

        GreenButton(
            text = stringResource(R.string.id_setup_2fa_now),
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(NavigateDestinations.TwoFactorAuthentication(network = network))
            onDismissRequest()
        }
    }
}

@Composable
@Preview
fun Call2ActionBottomSheetPreview() {
    GreenPreview {
        Call2ActionBottomSheet(
            viewModel = AnalyticsViewModelPreview.preview(),
            network = previewNetwork(),
            onDismissRequest = { }
        )
    }
}