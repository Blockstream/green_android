package com.blockstream.compose.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.toggle
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelAbstract
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.AnalyticsBottomSheet
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.displayLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect


class SetupNewWalletScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<SetupNewWalletViewModel>()

        AppBar()

        SetupNewWalletScreen(viewModel = viewModel)
    }
}

@Composable
fun SetupNewWalletScreen(
    viewModel: SetupNewWalletViewModelAbstract
) {
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel = viewModel) {
        if(it is SetupNewWalletViewModel.LocalSideEffects.ShowConsent){
            bottomSheetNavigator.show(AnalyticsBottomSheet {
                viewModel.postEvent(it.event)
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {

        GreenColumn(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .padding(bottom = 32.dp)
                .weight(3f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            Image(
                painter = painterResource(id = R.drawable.brand),
                contentDescription = null,
                modifier = Modifier
                    .heightIn(50.dp, 70.dp)
                    .align(Alignment.CenterHorizontally)
            )

            GreenColumn {
                Text(
                    text = stringResource(R.string.id_simple__secure_selfcustody),
                    style = displayLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.id_everything_you_need_to_take),
                    textAlign = TextAlign.Center,
                    color = whiteMedium
                )
            }
        }

        GreenColumn(
            padding = 24,
            modifier = Modifier
                .weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val termsOfServiceIsChecked by viewModel.termsOfServiceIsChecked.collectAsStateWithLifecycle()

            GreenButton(
                stringResource(R.string.id_add_wallet),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG,
                enabled = termsOfServiceIsChecked
            ) {
                viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickAddWallet)
            }

            GreenButton(
                stringResource(R.string.id_use_hardware_device),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE,
                enabled = termsOfServiceIsChecked
            ) {
                viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickUseHardwareDevice)
            }


            Row(
                modifier = Modifier.toggleable(
                    value = termsOfServiceIsChecked,
                    onValueChange = viewModel.termsOfServiceIsChecked.onValueChange(),
                    role = Role.Checkbox
                )
            ) {
                Checkbox(
                    checked = termsOfServiceIsChecked,
                    onCheckedChange = viewModel.termsOfServiceIsChecked.onValueChange()
                )

                val annotatedString = colorText(
                    stringResource(R.string.id_i_agree_to_the_terms_of_service),
                    listOf(
                        R.string.id_terms_of_service,
                        R.string.id_privacy_policy
                    ).map { stringResource(it) })

                ClickableText(text = annotatedString,
                    modifier = Modifier.padding(top = 14.dp),
                    onClick = {
                        annotatedString.getStringAnnotations("Index", start = it, end = it)
                            .firstOrNull()?.item?.toIntOrNull()?.also { index ->
                                (if (index == 0) {
                                    SetupNewWalletViewModel.LocalEvents.ClickTermsOfService()
                                } else {
                                    SetupNewWalletViewModel.LocalEvents.ClickPrivacyPolicy()
                                }).also {
                                    viewModel.postEvent(it)
                                }
                            } ?: run {
                                viewModel.termsOfServiceIsChecked.toggle()
                        }
                    })
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        AppSettingsButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp)
        ) {
            viewModel.postEvent(Events.AppSettings)
        }
    }

}

@Composable
@Preview
fun SetupNewWalletScreenPreview() {
    GreenTheme {
        BottomSheetNavigatorM3 {
            SetupNewWalletScreen(viewModel = SetupNewWalletViewModelPreview.preview())
        }
    }
}