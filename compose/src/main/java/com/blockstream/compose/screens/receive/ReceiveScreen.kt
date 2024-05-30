package com.blockstream.compose.screens.receive

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.receive.ReceiveViewModelAbstract
import com.blockstream.common.models.receive.ReceiveViewModelPreview
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenGradient
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf


@Parcelize
data class ReceiveScreen(
    val accountAsset: AccountAsset,
    val greenWallet: GreenWallet,
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ReceiveViewModel> {
            parametersOf(accountAsset, greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        ReceiveScreen(viewModel = viewModel)
    }
}

@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModelAbstract
) {
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel = viewModel)

    Column {

        Box(modifier = Modifier.weight(1f)) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {

                val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()
                accountAsset?.also {
                    GreenAccountAsset(
                        accountAssetBalance = it.accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(R.string.id_account__asset),
                        withEditIcon = true
                    ) {
//                        bottomSheetNavigator.show(
//                            AssetsAccountBottomSheet(
//                                greenWallet = viewModel.greenWallet,
//                                accounts = viewModel.assetsAndAccounts.value ?: listOf()
//                            )
//                        )
                    }
                }

                AnimatedNullableVisibility(value = accountAsset) {
                    val denomination by viewModel.denomination.collectAsStateWithLifecycle()
                    val amount by viewModel.amount.collectAsStateWithLifecycle()
                    val maxReceiveAmount by viewModel.maxReceiveAmount.collectAsStateWithLifecycle()
                    val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
                    val amountError by viewModel.amountError.collectAsStateWithLifecycle()
                    GreenAmountField(
                        value = amount,
                        onValueChange = viewModel.amount.onValueChange(),
                        assetId = it.assetId,
                        session = viewModel.sessionOrNull,
                        error = amountError,
                        denomination = denomination,
                        footerContent = {
                            Row(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResourceId(id = maxReceiveAmount),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f),
                                    style = bodyMedium,
                                    color = whiteLow
                                )

                                Text(
                                    text = amountExchange,
                                    textAlign = TextAlign.End,
                                    style = bodyMedium,
                                    color = whiteLow
                                )
                            }
                        },
                        onDenominationClick = {
                            viewModel.postEvent(Events.SelectDenomination)
                        }
                    )
                }

                Column {

                    Text(
                        stringResource(id = R.string.id_account_address),
                        style = labelMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )

                    Card {

                        Box {
                            Image(
                                painter = painterResource(id = R.drawable.arrows_counter_clockwise),
                                contentDescription = "Refresh",
                                contentScale = ContentScale.Inside,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .clickable {
                                        viewModel.postEvent(ReceiveViewModel.LocalEvents.GenerateNewAddress)
                                    }
                            )

                            Column {
                                val receiveAddress by viewModel.receiveAddress.collectAsStateWithLifecycle()

                                GreenQR(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp)
                                        .padding(top = 48.dp),
                                    data = receiveAddress,
                                    visibilityClick = {
                                        viewModel.postEvent(RecoveryPhraseViewModel.LocalEvents.ShowQR)
                                    }
                                )

                                GreenColumn(
                                    padding = 16,
                                    space = 8,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    GreenAddress(address = receiveAddress ?: "")

                                    GreenButton(
                                        text = stringResource(id = R.string.id_edit),
                                        type = GreenButtonType.OUTLINE,
                                        icon = painterResource(id = R.drawable.pencil_simple_line),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                    }

                                    GreenButton(
                                        text = stringResource(id = R.string.id_verify_on_device),
                                        type = GreenButtonType.OUTLINE,
                                        color = GreenButtonColor.GREENER,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                    }

                                    Text(
                                        text = stringResource(id = R.string.id_please_verify_that_the_address),
                                        style = bodySmall
                                    )
                                }
                            }
                        }

                    }
                }
            }

            GreenGradient(modifier = Modifier.align(Alignment.BottomCenter), size = 24)
        }

        GreenColumn(
            space = 8,
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            GreenButton(
                text = stringResource(id = R.string.id_share),
                icon = painterResource(id = R.drawable.share_network),
                modifier = Modifier.fillMaxWidth()
            ) {

            }

            GreenButton(
                text = stringResource(id = R.string.id_more_options),
                type = GreenButtonType.OUTLINE,
                modifier = Modifier.fillMaxWidth()
            ) {

            }
        }
    }
}

@Composable
@Preview
fun ReceiveScreenPreview() {
    GreenPreview {
        ReceiveScreen(viewModel = ReceiveViewModelPreview.preview())
    }
}