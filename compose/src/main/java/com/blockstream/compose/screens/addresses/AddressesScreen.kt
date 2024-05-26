package com.blockstream.compose.screens.addresses

import android.os.Parcelable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.looks.account.AddressLook
import com.blockstream.common.models.addresses.AddressesViewModel
import com.blockstream.common.models.addresses.AddressesViewModelAbstract
import com.blockstream.common.models.addresses.AddressesViewModelPreview
import com.blockstream.common.models.overview.AccountOverviewViewModel
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSearchField
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.monospaceFont
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.copyToClipboard
import com.blockstream.compose.utils.getClipboard
import com.blockstream.compose.utils.reachedBottom
import com.blockstream.compose.utils.stringResourceId
import io.github.mataku.middleellipsistext3.MiddleEllipsisText
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Parcelize
data class AddressesScreen(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<AddressesViewModel>() {
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

    val context = LocalContext.current
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Box{

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            state = listState
        ) {
            item {
                Row {
                    Text(
                        text = stringResource(R.string.id_address),
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
                        text = stringResource(R.string.id_actions),
                        style = labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width((8 + (48 * if (viewModel.canSign) 2 else 1)).dp)
                    )
                }
            }

            if(addresses.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.id_no_addresses),
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
                Box(modifier = Modifier.animateItemPlacement()) {
                    AddressListItem(look = address, onCopyClick = {
                        copyToClipboard(context = context, content = address.address)
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

    GreenCard(onClick = {
        popupState.isContextMenuVisible.value = true
    }, padding = 0) {
        GreenRow(
            padding = 0,
            space = 6,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(start = 16.dp, end = 8.dp)
        ) {
            MiddleEllipsisText(
                text = look.address, fontFamily = monospaceFont, modifier = Modifier.weight(1f)
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
                    colors = IconButtonDefaults.iconButtonColors(containerColor = md_theme_outline),
                    onClick = onCopyClick
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.copy), contentDescription = "Copy"
                    )
                }
                onSignatureClick?.also {
                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(containerColor = md_theme_outline),
                        onClick = onSignatureClick
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.signature),
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
                    title = stringResource(id = R.string.id_view_in_explorer),
                    iconRes = R.drawable.binoculars,
                    onClick = {
                        onExplorerClick()
                    }
                )
            )
        )
    }
}

@Composable
@Preview
fun AddressListItemPreview() {
    GreenThemePreview {
        GreenColumn {
            AddressListItem(
                AddressLook(
                    address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                    txCount = "99",
                    canSign = true
                )
            )
        }
    }
}

@Composable
@Preview
fun AddressesScreenPreview() {
    GreenPreview {
        AddressesScreen(viewModel = AddressesViewModelPreview.preview())
    }
}