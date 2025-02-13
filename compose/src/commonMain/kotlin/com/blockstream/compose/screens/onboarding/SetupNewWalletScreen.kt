package com.blockstream.compose.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_convenient_spending
import blockstream_green.common.generated.resources.id_dont_have_one_buy_a_jade
import blockstream_green.common.generated.resources.id_hardware
import blockstream_green.common.generated.resources.id_ideal_for_long_term_bitcoin
import blockstream_green.common.generated.resources.id_ideal_for_small_amounts
import blockstream_green.common.generated.resources.id_keys_stored_on_mobile_device
import blockstream_green.common.generated.resources.id_keys_stored_on_specialized
import blockstream_green.common.generated.resources.id_mitigates_common_attacks
import blockstream_green.common.generated.resources.id_mobile
import blockstream_green.common.generated.resources.id_restore_from_backup
import blockstream_green.common.generated.resources.id_security_level_
import blockstream_green.common.generated.resources.id_setup_a_new_wallet
import blockstream_green.common.generated.resources.id_setup_hardware_wallet
import blockstream_green.common.generated.resources.id_setup_mobile_wallet
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Coins
import com.adamglin.phosphoricons.regular.Key
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.ShieldChevron
import com.blockstream.common.events.Events
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.headlineLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.getResult
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SetupNewWalletScreen(
    viewModel: SetupNewWalletViewModelAbstract
) {
    NavigateDestinations.Environment.getResult<Int> {
        if (it >= 0) {
            viewModel.postEvent(
                Events.SelectEnviroment(
                    isTestnet = it == 1,
                    customNetwork = null
                )
            )
        }
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        onProgressStyle = OnProgressStyle.Full(
            bluBackground = false,
            riveAnimation = RiveAnimation.ROCKET
        )
    ) {

        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { 2 })
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        GreenColumn(
            padding = 0,
            space = 24,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.id_setup_a_new_wallet),
                style = titleLarge
            )

            val options = listOf(Res.string.id_mobile, Res.string.id_hardware)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        enabled = !onProgress,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index, count = options.size
                        ),
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        selected = pagerState.currentPage == index
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->

            GreenColumn(
                padding = 0,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {

                val isMobile = page == 0

                val title = when (isMobile) {
                    true -> stringResource(Res.string.id_mobile)
                    false -> stringResource(Res.string.id_hardware)
                }

                GreenColumn(
                    padding = 0,
                    space = 16,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ShieldChevron,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = stringResource(
                            Res.string.id_security_level_,
                            if (isMobile) "I" else "II"
                        ),
                        style = bodySmall,
                        color = whiteMedium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = title,
                        style = headlineLarge,
                        textAlign = TextAlign.Center
                    )

                    GreenColumn(
                        padding = 0,
                        space = 8,
                        modifier = Modifier.padding(top = 24.dp).padding(horizontal = 16.dp)
                    ) {
                        when (isMobile) {
                            true -> listOf(
                                PhosphorIcons.Regular.Coins to Res.string.id_ideal_for_small_amounts,
                                PhosphorIcons.Regular.ShieldCheck to Res.string.id_convenient_spending,
                                PhosphorIcons.Regular.Key to Res.string.id_keys_stored_on_mobile_device
                            )

                            false -> listOf(
                                PhosphorIcons.Regular.Coins to Res.string.id_ideal_for_long_term_bitcoin,
                                PhosphorIcons.Regular.ShieldCheck to Res.string.id_mitigates_common_attacks,
                                PhosphorIcons.Regular.Key to Res.string.id_keys_stored_on_specialized
                            )
                        }.also {
                            it.forEach { (icon, stringRes) ->
                                GreenRow(padding = 0, space = 16) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = whiteLow
                                    )

                                    Text(
                                        text = stringResource(stringRes),
                                        style = bodyLarge,
                                        color = whiteMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                val cta1 = when (isMobile) {
                    true -> stringResource(Res.string.id_setup_mobile_wallet)
                    false -> stringResource(Res.string.id_setup_hardware_wallet)
                }

                val cta2 = when (isMobile) {
                    true -> stringResource(Res.string.id_restore_from_backup)
                    false -> stringResource(Res.string.id_dont_have_one_buy_a_jade)
                }

                GreenColumn(padding = 0) {
                    GreenButton(
                        text = cta1,
                        size = GreenButtonSize.BIG,
                        enabled = !onProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isMobile) {
                            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.SetupMobileWallet)
                        } else {
                            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.SetupHardwareWallet)
                        }
                    }

                    GreenButton(
                        text = cta2,
                        type = GreenButtonType.TEXT,
                        size = GreenButtonSize.BIG,
                        enabled = !onProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isMobile) {
                            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.RestoreWallet)
                        } else {
                            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.BuyJade)
                        }
                    }
                }
            }
        }
    }
}
