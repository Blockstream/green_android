package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.hardware_keys
import blockstream_green.common.generated.resources.id_convenient_spending
import blockstream_green.common.generated.resources.id_dont_have_one_buy_a_jade
import blockstream_green.common.generated.resources.id_hardware
import blockstream_green.common.generated.resources.id_ideal_for_long_term_bitcoin
import blockstream_green.common.generated.resources.id_ideal_for_small_amounts
import blockstream_green.common.generated.resources.id_keys_stored_on_mobile_device
import blockstream_green.common.generated.resources.id_keys_stored_on_specialized
import blockstream_green.common.generated.resources.id_mitigates_common_attacks
import blockstream_green.common.generated.resources.id_mobile
import blockstream_green.common.generated.resources.id_security_level
import blockstream_green.common.generated.resources.id_security_level_
import blockstream_green.common.generated.resources.id_selected
import blockstream_green.common.generated.resources.id_setup_hardware_wallet
import blockstream_green.common.generated.resources.phone_keys
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Coins
import com.adamglin.phosphoricons.regular.Key
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.ShieldChevron
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.headlineLarge
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.components.GreenSpacer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityLevelBottomSheet(
    viewModel: SetupNewWalletViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_security_level),
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        GreenColumn(
            padding = 0
        ) {

            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState(pageCount = { 2 })

            val options = listOf(Res.string.id_mobile, Res.string.id_hardware)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index, count = options.size
                        ), onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }, selected = pagerState.currentPage == index
                    ) {
                        Text(stringResource(label))
                    }
                }
            }

            HorizontalPager(
                state = pagerState
            ) { page ->

                val isMobile = page == 0

                GreenColumn(
                    padding = 0,
                    space = 32,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    GreenColumn(
                        padding = 0,
                        space = 8,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {

                        Icon(
                            imageVector = PhosphorIcons.Regular.ShieldChevron,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )

                        val title = when (isMobile) {
                            true -> stringResource(Res.string.id_mobile)
                            false -> stringResource(Res.string.id_hardware)
                        }

                        Text(
                            text = title, style = headlineLarge, textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(
                                Res.string.id_security_level_, if (isMobile) "I" else "II"
                            ), style = bodySmall, color = whiteMedium, textAlign = TextAlign.Center
                        )

                        Image(
                            imageVector = vectorResource(if(isMobile) Res.drawable.phone_keys else Res.drawable.hardware_keys),
                            contentDescription = null,
                            modifier = Modifier.height(150.dp)
                        )
                    }

                    GreenColumn(
                        padding = 0, space = 8, modifier = Modifier.padding(horizontal = 16.dp)
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

                    GreenColumn(padding = 0) {
                        val cta1 = when (isMobile) {
                            true -> stringResource(Res.string.id_selected)
                            false -> stringResource(Res.string.id_setup_hardware_wallet)
                        }

                        val cta2 = stringResource(Res.string.id_dont_have_one_buy_a_jade)

                        GreenButton(
                            text = cta1,
                            enabled = !isMobile,
                            size = GreenButtonSize.BIG,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isMobile) {
                                 viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.SetupHardwareWallet)
                            }
                        }

                        if (isMobile) {
                            GreenSpacer(space = 50)
                        } else {
                            GreenButton(
                                text = cta2,
                                type = GreenButtonType.TEXT,
                                size = GreenButtonSize.BIG,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.BuyJade)
                            }
                        }
                    }
                }
            }
        }
    }
}