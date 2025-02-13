package com.blockstream.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.brand
import blockstream_green.common.generated.resources.id_by_using_blockstream_app_you_agree
import blockstream_green.common.generated.resources.id_connect_jade
import blockstream_green.common.generated.resources.id_everything_you_need_to_take
import blockstream_green.common.generated.resources.id_get_started
import blockstream_green.common.generated.resources.id_privacy_policy
import blockstream_green.common.generated.resources.id_simple__secure_selfcustody
import blockstream_green.common.generated.resources.id_terms_of_service
import com.blockstream.common.events.Events
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.home.HomeViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.Promo
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.displayLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    viewModel: HomeViewModelAbstract,
) {
    NavigateDestinations.Analytics.getResult<Boolean> {
        viewModel.postEvent(Events.Continue)
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
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

                    GreenColumn(
                        padding = 0,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

                        GreenButton(
                            stringResource(Res.string.id_get_started),
                            modifier = Modifier.fillMaxWidth().testTag("getStarted"),
                            size = GreenButtonSize.BIG,
                            enabled = !onProgress
                        ) {
                            viewModel.postEvent(HomeViewModel.LocalEvents.GetStarted)
                        }

                        GreenButton(
                            stringResource(Res.string.id_connect_jade),
                            modifier = Modifier.fillMaxWidth().testTag("connectJade"),
                            size = GreenButtonSize.BIG,
                            type = GreenButtonType.OUTLINE,
                            color = GreenButtonColor.GREEN,
                            enabled = !onProgress
                        ) {
                            viewModel.postEvent(HomeViewModel.LocalEvents.ConnectJade)
                        }

                        val annotatedString = colorText(
                            text = stringResource(Res.string.id_by_using_blockstream_app_you_agree),
                            coloredTexts = listOf(
                                Res.string.id_terms_of_service,
                                Res.string.id_privacy_policy
                            ).map { stringResource(it) }
                        )

                        ClickableText(
                            text = annotatedString,
                            modifier = Modifier.padding(top = 14.dp).padding(horizontal = 16.dp),
                            style = bodyMedium.copy(textAlign = TextAlign.Center),
                            onClick = {
                                annotatedString.getStringAnnotations(
                                    "Index",
                                    start = it,
                                    end = it
                                ).firstOrNull()?.item?.toIntOrNull()?.also { index ->
                                    (if (index == 0) {
                                        HomeViewModel.LocalEvents.ClickTermsOfService()
                                    } else {
                                        HomeViewModel.LocalEvents.ClickPrivacyPolicy()
                                    }).also { event ->
                                        viewModel.postEvent(event)
                                    }
                                }
                            }
                        )
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

    }
}