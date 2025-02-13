package com.blockstream.compose.screens

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.brand
import blockstream_green.common.generated.resources.id_everything_you_need_to_take
import blockstream_green.common.generated.resources.id_get_started
import blockstream_green.common.generated.resources.id_i_agree_to_the_terms_of_service
import blockstream_green.common.generated.resources.id_privacy_policy
import blockstream_green.common.generated.resources.id_simple__secure_selfcustody
import blockstream_green.common.generated.resources.id_terms_of_service
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.toggle
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.AboutButton
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.Promo
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.displayLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    viewModel: WalletsViewModelAbstract,
) {
    var pendingEvent by remember { mutableStateOf<Event?>(null) }

    NavigateDestinations.Analytics.getResult<Boolean> {
        pendingEvent?.also {
            viewModel.postEvent(it)
        }
    }

    SetupScreen(viewModel = viewModel, sideEffectsHandler = {
        if (it is HomeViewModel.LocalSideEffects.ShowConsent) {
            pendingEvent = it.event
            viewModel.postEvent(NavigateDestinations.Analytics)
        }
    }, withPadding = false, modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Column {
            Image(
                painter = painterResource(Res.drawable.brand),
                contentDescription = null,
                modifier = Modifier
                    .heightIn(50.dp, 70.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Banner(viewModel = viewModel, withTopPadding = true)

            val isEmptyWallet by viewModel.isEmptyWallet.collectAsStateWithLifecycle()

            when (isEmptyWallet) {
                true -> {
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {

                        Promo(
                            viewModel = viewModel,
                            withAnimation = true,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Column(modifier = Modifier.padding(vertical = 32.dp)) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.id_simple__secure_selfcustody),
                                style = displayLarge,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.id_everything_you_need_to_take),
                                textAlign = TextAlign.Center,
                                color = whiteMedium
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {

                            val termsOfServiceIsChecked by viewModel.termsOfServiceIsChecked.collectAsStateWithLifecycle()

                            GreenButton(
                                stringResource(Res.string.id_get_started),
                                modifier = Modifier.fillMaxWidth().testTag("getStarted"),
                                size = GreenButtonSize.BIG,
                                enabled = termsOfServiceIsChecked
                            ) {
                                viewModel.postEvent(HomeViewModel.LocalEvents.ClickGetStarted)
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
                                    onCheckedChange = viewModel.termsOfServiceIsChecked.onValueChange(),
                                    modifier = Modifier.testTag("termsOfService")
                                )

                                val annotatedString = colorText(
                                    stringResource(Res.string.id_i_agree_to_the_terms_of_service),
                                    listOf(
                                        Res.string.id_terms_of_service,
                                        Res.string.id_privacy_policy
                                    ).map { stringResource(it) })

                                ClickableText(
                                    text = annotatedString,
                                    modifier = Modifier.padding(top = 14.dp),
                                    onClick = {
                                        annotatedString.getStringAnnotations(
                                            "Index",
                                            start = it,
                                            end = it
                                        )
                                            .firstOrNull()?.item?.toIntOrNull()?.also { index ->
                                                (if (index == 0) {
                                                    HomeViewModel.LocalEvents.ClickTermsOfService()
                                                } else {
                                                    HomeViewModel.LocalEvents.ClickPrivacyPolicy()
                                                }).also { event ->
                                                    viewModel.postEvent(event)
                                                }
                                            } ?: run {
                                            viewModel.termsOfServiceIsChecked.toggle()
                                        }
                                    })
                            }
                        }
                    }

                }

                false -> {
                    WalletsScreen(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp),
                        viewModel = viewModel,
                    )

                }

                null -> {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AboutButton {
                    viewModel.postEvent(NavigateDestinations.About)
                }

                AppSettingsButton {
                    viewModel.postEvent(NavigateDestinations.AppSettings)
                }
            }
        }
    }
}

@Composable
@Preview
fun HomeScreenPreview2() {
    GreenPreview {
        HomeScreen(viewModel = WalletsViewModelPreview.previewEmpty())
    }
}