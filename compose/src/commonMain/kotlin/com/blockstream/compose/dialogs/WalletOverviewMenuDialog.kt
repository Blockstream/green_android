package com.blockstream.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_accounts
import blockstream_green.common.generated.resources.id_add_new_account
import blockstream_green.common.generated.resources.id_denomination
import blockstream_green.common.generated.resources.id_general
import blockstream_green.common.generated.resources.id_get_support
import blockstream_green.common.generated.resources.id_logout
import blockstream_green.common.generated.resources.id_refresh
import blockstream_green.common.generated.resources.id_rename
import blockstream_green.common.generated.resources.id_view_archived_accounts
import blockstream_green.common.generated.resources.id_wallet
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowsCounterClockwise
import com.adamglin.phosphoricons.regular.BoxArrowDown
import com.adamglin.phosphoricons.regular.Coins
import com.adamglin.phosphoricons.regular.Headset
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.SignOut
import com.adamglin.phosphoricons.regular.TextAa
import com.blockstream.common.SupportType
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.SupportData
import com.blockstream.common.events.Events
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.ui.components.GreenRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun WalletOverviewMenuDialog(viewModel: WalletOverviewViewModelAbstract, onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
        },
        DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .noRippleClickable {
                    onDismissRequest()
                },
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                modifier = Modifier
                    .noRippleClickable {
                        // prevent click to be propagated
                    }
                    .align(Alignment.TopEnd)
                    .widthIn(max = 300.dp)
                    .padding(top = 8.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp).padding(top = 8.dp)) {

                    MenuHeader(text = stringResource(Res.string.id_wallet))

                    MenuItem(
                        text = stringResource(Res.string.id_rename),
                        icon = PhosphorIcons.Regular.TextAa
                    ) {
                        viewModel.postEvent(NavigateDestinations.RenameWallet(viewModel.greenWallet))
                        onDismissRequest()
                    }

                    MenuItem(
                        text = stringResource(Res.string.id_denomination),
                        icon = PhosphorIcons.Regular.Coins
                    ) {
                        viewModel.postEvent(WalletOverviewViewModel.LocalEvents.DenominationExchangeRate)
                        onDismissRequest()
                    }

//                    MenuItem(
//                        text = stringResource(Res.string.id_settings),
//                        icon = PhosphorIcons.Regular.GearSix
//                    ) {
//                        viewModel.postEvent(NavigateDestinations.WalletSettings(greenWallet = viewModel.greenWallet))
//                        onDismissRequest()
//                    }

                    if (viewModel.sessionOrNull?.isWatchOnlyValue == false && !viewModel.greenWallet.isLightning) {
                        MenuHeader(text = stringResource(Res.string.id_accounts))

                        MenuItem(
                            text = stringResource(Res.string.id_add_new_account),
                            icon = PhosphorIcons.Regular.Plus
                        ) {
                            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.MenuNewAccountClick)
                            onDismissRequest()
                        }
                    }

                    val archivedAccounts by viewModel.archivedAccounts.collectAsStateWithLifecycle()
                    if (archivedAccounts > 0) {
                        MenuItem(
                            text = stringResource(Res.string.id_view_archived_accounts),
                            icon = PhosphorIcons.Regular.BoxArrowDown,
                            count = archivedAccounts.takeIf { it > 0 }?.let { "$it" }
                        ) {
                            viewModel.postEvent(NavigateDestinations.ArchivedAccounts(greenWallet = viewModel.greenWallet))
                            onDismissRequest()
                        }
                    }

                    MenuHeader(text = stringResource(Res.string.id_general))

                    MenuItem(
                        text = stringResource(Res.string.id_refresh),
                        icon = PhosphorIcons.Regular.ArrowsCounterClockwise
                    ) {
                        viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Refresh)
                        onDismissRequest()
                    }

                    MenuItem(
                        text = stringResource(Res.string.id_get_support),
                        icon = PhosphorIcons.Regular.Headset
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.Support(
                                type = SupportType.INCIDENT,
                                supportData = SupportData.create(session = viewModel.sessionOrNull),
                                greenWalletOrNull = viewModel.greenWalletOrNull
                            )
                        )
                        onDismissRequest()
                    }

                    MenuHeader()

                    MenuItem(
                        text = stringResource(Res.string.id_logout),
                        icon = PhosphorIcons.Regular.SignOut,
                        color = red
                    ) {
                        viewModel.postEvent(Events.Logout(reason = LogoutReason.USER_ACTION))
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector, count: String? = null, color: Color = whiteHigh, onClick: () -> Unit = {}) {
    GreenRow(
        padding = 0,
        space = 8,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(text = text, color = color, style = bodyLarge, overflow = TextOverflow.Ellipsis, maxLines = 1, modifier = Modifier.weight(1f))
        if (count != null) {
            Card(colors = CardDefaults.cardColors(containerColor = md_theme_outline, contentColor = whiteMedium)) {
                Text(count, color = whiteMedium, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
fun MenuHeader(text: String? = null) {
    GreenRow(
        padding = 0,
        space = 8,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        text?.also {
            Text(text = it.uppercase(), color = whiteLow, style = bodySmall)
        }
        HorizontalDivider(color = whiteLow.copy(alpha = 0.2f))
    }
}
