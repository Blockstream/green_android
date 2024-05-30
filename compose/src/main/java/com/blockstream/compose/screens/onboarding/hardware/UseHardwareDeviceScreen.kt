package com.blockstream.compose.screens.onboarding.hardware

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.models.onboarding.hardware.UseHardwareDeviceViewModel
import com.blockstream.common.models.onboarding.hardware.UseHardwareDeviceViewModelAbstract
import com.blockstream.common.models.onboarding.hardware.UseHardwareDeviceViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.noRippleClickable


object UseHardwareDeviceScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<UseHardwareDeviceViewModel>()

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        UseHardwareDeviceScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UseHardwareDeviceScreen(
    viewModel: UseHardwareDeviceViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        val pagerState = rememberPagerState(pageCount = { 3 })

        Column(modifier = Modifier.weight(1f)) {
            // Display 10 items

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->

                val title = when (page) {
                    0 -> stringResource(id = R.string.id_welcome_to_blockstream_jade)
                    1 -> stringResource(id = R.string.id_hardware_security)
                    2 -> stringResource(id = R.string.id_offline_key_storage)
                    else -> stringResource(id = R.string.id_fully_air_gapped_with_qr)
                }
                val message = when (page) {
                    0 -> stringResource(id = R.string.id_jade_is_a_specialized_device)
                    1 -> stringResource(id = R.string.id_your_bitcoin_and_liquid_assets)
                    2 -> stringResource(id = R.string.id_jade_is_an_isolated_device_not)
                    else -> stringResource(id = R.string.id_qr_mode_allows_you_to_communicate)
                }
                val image = when (page) {
                    0 -> painterResource(id = R.drawable.jade_welcome)
                    1 -> painterResource(id = R.drawable.hardware_security)
                    2 -> painterResource(id = R.drawable.offline_key_storage)
                    else -> painterResource(id = R.drawable.qr_air_gapped)
                }

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hw_matrix_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                        )

                        Image(
                            painter = image,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }

                    GreenColumn(
                        space = 8,
                        padding = 32,
                        modifier = Modifier
                            .padding(horizontal = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = displayMedium,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = message,
                            textAlign = TextAlign.Center,
                            color = whiteMedium
                        )
                    }
                }
            }

            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color =
                        if (pagerState.currentPage == iteration) green else md_theme_surfaceCircle
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }

        GreenColumn(
            padding = 24,
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            GreenButton(
                stringResource(R.string.id_connect_jade),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG,
            ) {
                viewModel.postEvent(UseHardwareDeviceViewModel.LocalEvents.ConnectJade)
            }

            GreenButton(
                stringResource(R.string.id_connect_a_different_hardware),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                viewModel.postEvent(UseHardwareDeviceViewModel.LocalEvents.ConnectDifferentHardwareDevice)
            }

            GreenRow(padding = 0, space = 4, modifier = Modifier
                .noRippleClickable {
                    viewModel.postEvent(UseHardwareDeviceViewModel.LocalEvents.JadeStore)
                }
                .padding(8.dp)) {
                Text(
                    text = stringResource(R.string.id_dont_have_a_jade), style = bodyLarge
                )

                Text(
                    text = stringResource(R.string.id_check_our_store), style = labelLarge,
                    color = green,
                )

                Image(
                    painter = painterResource(id = R.drawable.arrow_square_out),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(
                        green
                    )
                )
            }
        }
    }
}

@Composable
@Preview
fun UseHardwareDeviceScreenPreview() {
    GreenPreview {
        UseHardwareDeviceScreen(viewModel = UseHardwareDeviceViewModelPreview.preview())
    }
}