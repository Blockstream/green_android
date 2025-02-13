package com.blockstream.compose.screens.exchange

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_square_out
import blockstream_green.common.generated.resources.id_account
import blockstream_green.common.generated.resources.id_asset_to_buy
import blockstream_green.common.generated.resources.id_buy
import blockstream_green.common.generated.resources.id_buy_with_s
import blockstream_green.common.generated.resources.id_create_new_account
import blockstream_green.common.generated.resources.id_provided_by_
import blockstream_green.common.generated.resources.id_select_account
import blockstream_green.common.generated.resources.id_sell
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.exchange.OnOffRampsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.navigation.PopTo
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_errorContainer
import com.blockstream.compose.theme.md_theme_surface
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenArrow
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnOffRampsScreen(
    viewModel: OnOffRampsViewModelAbstract
) {

    NavigateDestinations.ReviewAddAccount.getResult<AccountAsset> {
        viewModel.buyAccount.value = it.accountAssetBalance
    }

    NavigateDestinations.Assets.getResult<AssetBalance> {
        viewModel.buyAsset.value = it
    }

    NavigateDestinations.AssetsAccounts.getResult<AccountAssetBalance> {
        // TODO support sell account
        viewModel.buyAccount.value = it
    }

    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    SetupScreen(viewModel = viewModel, withPadding = false) {
        GreenColumn {
            GreenColumn(
                padding = 0,
                modifier = Modifier.weight(1f).verticalScroll(
                    rememberScrollState()
                )
            ) {
                val isBuy by viewModel.isBuy.collectAsStateWithLifecycle()

                if (viewModel.appInfo.isDevelopment) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        (0..1).forEach { index ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = 2
                                ),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = md_theme_surface,
                                    activeContentColor = green
                                ),
                                onClick = {
                                    viewModel.isBuy.value = index == 0
                                },
                                selected = (if (isBuy) 0 else 1) == index
                            ) {
                                Text(stringResource(if (index == 0) Res.string.id_buy else Res.string.id_sell))
                            }
                        }
                    }
                }

                AnimatedVisibility(isBuy) {

                    GreenColumn(padding = 0) {

                        val buyAsset by viewModel.buyAsset.collectAsStateWithLifecycle()

                        GreenAsset(
                            assetBalance = buyAsset,
                            session = viewModel.sessionOrNull,
                            title = stringResource(Res.string.id_asset_to_buy),
                            withEditIcon = false,
//                        onClick = {
//                            viewModel.postEvent(NavigateDestinations.Assets(viewModel.buyAssets.value))
//                        }
                        )

                        val buyAccount by viewModel.buyAccount.collectAsStateWithLifecycle()

                        if (buyAccount != null) {
                            GreenAccountAsset(
                                accountAssetBalance = buyAccount,
                                session = viewModel.sessionOrNull,
                                title = stringResource(Res.string.id_account),
                                selectText = stringResource(Res.string.id_select_account),
                                withAsset = false,
                                withEditIcon = true
                            ) {
                                viewModel.postEvent(NavigateDestinations.AssetsAccounts(greenWallet = viewModel.greenWallet, assetsAccounts = AccountAssetBalanceList(viewModel.buyAccounts.value)))
                            }
                        } else {
                            GreenDataLayout(
                                title = stringResource(Res.string.id_account),
                                withPadding = false,
                                onClick = {
                                    viewModel.postEvent(
                                        NavigateDestinations.ChooseAccountType(
                                            greenWallet = viewModel.greenWallet,
                                            assetBalance = viewModel.buyAsset.value,
                                            allowAssetSelection = false,
                                            popTo = PopTo.OnOffRamps
                                        )
                                    )
                                }) {
                                GreenRow(
                                    padding = 16,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(Res.string.id_create_new_account),
                                        style = labelLarge,
                                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                                    )

                                    GreenArrow()
                                }
                            }
                        }

                        val amount by viewModel.amount.collectAsStateWithLifecycle()
                        val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
                        val amountError by viewModel.amountError.collectAsStateWithLifecycle()
                        val amountHint by viewModel.amountHint.collectAsStateWithLifecycle()
                        val denomination by viewModel.denomination.collectAsStateWithLifecycle()

                        AnimatedVisibility(visible = buyAsset != null && buyAccount != null) {

                            GreenAmountField(
                                value = amount,
                                onValueChange = viewModel.amount.onValueChange(),
                                assetId = buyAsset?.assetId,
                                session = viewModel.sessionOrNull,
                                denomination = denomination,
                                helperText = amountError ?: amountHint,
                                helperContainerColor = if (amountError != null) md_theme_errorContainer else green20,
                                footerContent = {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = amountExchange,
                                            textAlign = TextAlign.End,
                                            style = bodyMedium,
                                            color = whiteLow,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                onDenominationClick = {
                                    viewModel.postEvent(Events.SelectDenomination)
                                }

                            )
                        }
                    }
                }

                AnimatedVisibility(!isBuy) {
                    GreenColumn(padding = 0) {
                        Text("Not yet implemented")
                    }
                }
            }

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

            if (viewModel.appInfo.isDevelopment) {
                val isSandboxEnvironment by viewModel.isSandboxEnvironment.collectAsStateWithLifecycle()

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    (0..1).forEach { index ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index, count = 2
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = md_theme_surface,
                                activeContentColor = green
                            ),
                            onClick = {
                                viewModel.isSandboxEnvironment.value = index == 1
                            },
                            selected = (if (isSandboxEnvironment) 1 else 0) == index
                        ) {
                            Text(if (index == 0) "Production" else "Sandbox")
                        }
                    }
                }
            }

            GreenColumn(padding = 0) {

                Text(
                    stringResource(Res.string.id_provided_by_, "meld.io"),
                    style = bodyMedium,
                    color = whiteLow
                )

                GreenButton(
                    text = stringResource(Res.string.id_buy_with_s, "meld.io"),
                    modifier = Modifier.fillMaxWidth(),
                    size = GreenButtonSize.BIG,
                    icon = painterResource(Res.drawable.arrow_square_out),
                    enabled = buttonEnabled,
                    onProgress = onProgress
                ) {
                    viewModel.postEvent(Events.Continue)
                }
            }
        }
    }
}