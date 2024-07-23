package com.blockstream.compose.screens.addresses

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.binoculars
import blockstream_green.common.generated.resources.copy
import blockstream_green.common.generated.resources.id_actions
import blockstream_green.common.generated.resources.id_address
import blockstream_green.common.generated.resources.id_no_addresses
import blockstream_green.common.generated.resources.id_view_in_explorer
import blockstream_green.common.generated.resources.signature
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.looks.account.AddressLook
import com.blockstream.common.models.addresses.AddressesViewModel
import com.blockstream.common.models.addresses.AddressesViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSearchField
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.reachedBottom
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
data class AddressesScreen(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AddressesViewModel>() {
            parametersOf(greenWallet, accountAsset)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        AddressesScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddressesScreen(
    viewModel: AddressesViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    val platformManager = LocalPlatformManager.current
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    val listState: LazyListState = rememberLazyListState()
    val reachedBottom: Boolean by remember { derivedStateOf { listState.reachedBottom() } }

    LaunchedEffect(reachedBottom) {
        if (reachedBottom && hasMore) {
            viewModel.postEvent(AddressesViewModel.LocalEvents.LoadMore)
        }
    }

    Column {
        GreenColumn {
            GreenSearchField(
                value = query,
                onValueChange = viewModel.query.onValueChange(),
            )
        }

        Box {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                state = listState
            ) {
                item {
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.id_address),
                            style = labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Tx",
                            style = labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .widthIn(min = 30.dp)
                        )
                        Text(
                            text = stringResource(Res.string.id_actions),
                            style = labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width((8 + (48 * if (viewModel.canSign) 2 else 1)).dp)
                        )
                    }
                }

                if (addresses.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.id_no_addresses),
                            style = bodyMedium,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                items(addresses) { address ->
                    Box {
                        HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))

                        AddressListItem(look = address, onCopyClick = {
                            platformManager.copyToClipboard(content = address.address)
                        }, onExplorerClick = {
                            viewModel.postEvent(
                                AddressesViewModel.LocalEvents.AddressBlockExplorer(
                                    address = address.address
                                )
                            )
                        }, onSignatureClick = if (viewModel.canSign) {
                            {
                                viewModel.postEvent(
                                    NavigateDestinations.SignMessage(
                                        accountAsset = viewModel.accountAsset.value!!,
                                        address = address.address
                                    )
                                )
                            }
                        } else null)
                    }
                }

                item {
                    androidx.compose.animation.AnimatedVisibility(hasMore) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .padding(all = 16.dp)
                                .height(1.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddressListItem(
    look: AddressLook,
    onCopyClick: () -> Unit = {},
    onExplorerClick: () -> Unit = {},
    onSignatureClick: (() -> Unit)? = null
) {

    val popupState = remember {
        PopupState()
    }

    Box(modifier = Modifier.clickable {
        popupState.isContextMenuVisible.value = true
    }) {
        GreenRow(
            padding = 0,
            space = 6,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(start = 16.dp, end = 8.dp)
        ) {

            Text(text = "#${look.index}", style = bodyMedium, color = whiteMedium)

            // MiddleEllipsisText adds a performance penalty
            Text(
                text = look.address,
                fontFamily = MonospaceFont(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = whiteHigh
            )

            Text(
                text = look.txCount,
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = labelLarge,
                modifier = Modifier.widthIn(min = 30.dp)
            )

            Row {
                IconButton(
                    onClick = onCopyClick
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.copy), contentDescription = "Copy"
                    )
                }
                onSignatureClick?.also {
                    IconButton(
                        onClick = onSignatureClick
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.signature),
                            contentDescription = "Sign"
                        )
                    }
                }
            }
        }

        PopupMenu(
            state = popupState,
            entries = listOf(
                MenuEntry(
                    title = stringResource(Res.string.id_view_in_explorer),
                    iconRes = Res.drawable.binoculars,
                    onClick = {
                        onExplorerClick()
                    }
                )
            )
        )
    }
}