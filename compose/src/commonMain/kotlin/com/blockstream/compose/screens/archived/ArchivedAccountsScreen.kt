package com.blockstream.compose.screens.archived

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.box_arrow_up
import blockstream_green.common.generated.resources.id_no_archived_accounts
import blockstream_green.common.generated.resources.id_rename_account
import blockstream_green.common.generated.resources.id_unarchive_account
import blockstream_green.common.generated.resources.text_aa
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
data class ArchivedAccountsScreen(
    val greenWallet: GreenWallet,
    val navigateToRoot: Boolean
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ArchivedAccountsViewModel>() {
            parametersOf(greenWallet, navigateToRoot)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        ArchivedAccountsScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchivedAccountsScreen(
    viewModel: ArchivedAccountsViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    val archivedAccounts by viewModel.archivedAccounts.collectAsStateWithLifecycle()

    ScreenContainer(onProgress = archivedAccounts.isLoading(), blurBackground = false) {
        if (archivedAccounts.isEmpty()) {
            Text(
                text = stringResource(Res.string.id_no_archived_accounts),
                style = bodyMedium,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center)
            )
        } else if (archivedAccounts.isSuccess()) {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(all = 16.dp),
            ) {
                items(archivedAccounts.data() ?: listOf()) { account ->
                    Box(modifier = Modifier.animateItemPlacement()) {

                        val popupState = remember {
                            PopupState()
                        }

                        val density = LocalDensity.current

                        GreenAccountAsset(
                            modifier = Modifier.onSizeChanged {
                                popupState.offset.value =
                                    with(density) {
                                        DpOffset(54.dp, (-it.height.toDp() + 14.dp + 24.dp))
                                    }
                            },
                            accountAssetBalance = account,
                            withAsset = false,
                            session = viewModel.sessionOrNull,
                            onClick = {
                                popupState.isContextMenuVisible.value = true
                            }
                        )

                        PopupMenu(
                            state = popupState,
                            entries =
                            listOf(
                                MenuEntry(
                                    title = stringResource(Res.string.id_rename_account),
                                    iconRes = Res.drawable.text_aa,
                                    onClick = {
                                        viewModel.postEvent(
                                            NavigateDestinations.RenameAccount(
                                                account = account.account
                                            )
                                        )
                                    }
                                ),
                                MenuEntry(
                                    title = stringResource(Res.string.id_unarchive_account),
                                    iconRes = Res.drawable.box_arrow_up,
                                    onClick = {
                                        viewModel.postEvent(
                                            Events.UnArchiveAccount(
                                                account = account.account,
                                                navigateToRoot = viewModel.navigateToRoot
                                            )
                                        )
                                    }
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}