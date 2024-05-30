package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModelAbstract
import com.blockstream.common.models.onboarding.phone.AddWalletViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.EnvironmentBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect


object AddWalletScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AddWalletViewModel>()

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        AddWalletScreen(viewModel = viewModel)
    }
}

@Composable
fun AddWalletScreen(
    viewModel: AddWalletViewModelAbstract
) {

    getNavigationResult<Boolean>(EnvironmentBottomSheet.resultKey).value?.also {
        viewModel.postEvent(
            AddWalletViewModel.LocalEvents.SelectEnviroment(
                isTestnet = it,
                customNetwork = null
            )
        )
    }

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel = viewModel) { sideEffect ->
        if (sideEffect is AddWalletViewModel.LocalSideEffects.SelectEnvironment) {
            bottomSheetNavigator.show(EnvironmentBottomSheet)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Box(
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 24.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.hw_matrix_bg),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )

            Image(
                painter = painterResource(id = R.drawable.phone_keys),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        GreenColumn(
            space = 8,
            modifier = Modifier.padding(horizontal = 0.dp).weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.id_take_control_your_keys_your),
                style = displayMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.id_your_keys_secure_your_coins_on),
                textAlign = TextAlign.Center,
                color = whiteMedium
            )
        }

        Column() {
            GreenColumn(
                padding = 24,
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                GreenButton(
                    stringResource(R.string.id_new_wallet),
                    modifier = Modifier.fillMaxWidth(),
                    size = GreenButtonSize.BIG,
                ) {
                    viewModel.postEvent(AddWalletViewModel.LocalEvents.NewWallet)
                }

                GreenButton(
                    stringResource(R.string.id_restore_wallet),
                    modifier = Modifier.fillMaxWidth(),
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE,
                    size = GreenButtonSize.BIG,
                ) {
                    viewModel.postEvent(AddWalletViewModel.LocalEvents.RestoreWallet)
                }

            }
        }
    }

}

@Composable
@PreviewScreenSizes
@Preview
fun AddWalletScreenPreview() {
    GreenPreview {
        AddWalletScreen(viewModel = AddWalletViewModelPreview.preview())
    }
}