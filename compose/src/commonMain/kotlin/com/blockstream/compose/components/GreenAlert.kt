package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bip39_passphrase
import blockstream_green.common.generated.resources.id_2fa_dispute_in_progress
import blockstream_green.common.generated.resources.id_2fa_reset_in_progress
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_lightning_account
import blockstream_green.common.generated.resources.id_lightning_service_is_undergoing
import blockstream_green.common.generated.resources.id_passphrase_protected
import blockstream_green.common.generated.resources.id_some_accounts_cannot_be_logged
import blockstream_green.common.generated.resources.id_system_message
import blockstream_green.common.generated.resources.id_the_lightning_service_is
import blockstream_green.common.generated.resources.id_this_wallet_is_based_on_your
import blockstream_green.common.generated.resources.id_this_wallet_operates_on_a_test
import blockstream_green.common.generated.resources.id_try_again
import blockstream_green.common.generated.resources.id_warning
import blockstream_green.common.generated.resources.id_warning_wallet_locked_by
import blockstream_green.common.generated.resources.id_your_wallet_is_locked_for_a
import blockstream_green.common.generated.resources.lightning
import blockstream_green.common.generated.resources.warning
import blockstream_green.common.generated.resources.x
import com.blockstream.common.data.AlertType
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAlert(modifier: Modifier = Modifier, alertType: AlertType, viewModel: GreenViewModel) {
    when (alertType) {
        is AlertType.TestnetWarning -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_warning),
                message = stringResource(Res.string.id_this_wallet_operates_on_a_test),
                icon = painterResource(Res.drawable.warning)
            )
        }

        is AlertType.SystemMessage -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_system_message),
                message = alertType.message,
                maxLines = 3,
                primaryButton = stringResource(Res.string.id_learn_more),
                onPrimaryClick = {
                    viewModel.postEvent(
                        NavigateDestinations.SystemMessage(
                            greenWallet = viewModel.greenWallet,
                            network = alertType.network,
                            message = alertType.message
                        )
                    )
                },
                onCloseClick = {
                    viewModel.postEvent(Events.DismissSystemMessage)
                }
            )
        }
        is AlertType.Dispute2FA -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_2fa_dispute_in_progress),
                message = stringResource(Res.string.id_warning_wallet_locked_by),
                primaryButton = stringResource(Res.string.id_learn_more),
                onPrimaryClick = {
                    viewModel.postEvent(
                        NavigateDestinations.TwoFactorReset(
                            greenWallet = viewModel.greenWallet,
                            network = alertType.network,
                            twoFactorReset = viewModel.sessionOrNull?.twoFactorReset(alertType.network)?.value
                        )
                    )
                }
            )
        }
        is AlertType.Reset2FA -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_2fa_reset_in_progress),
                message = stringResource(Res.string.id_your_wallet_is_locked_for_a, alertType.twoFactorReset.daysRemaining),
                primaryButton = stringResource(Res.string.id_learn_more),
                onPrimaryClick = {
                    viewModel.postEvent(
                        NavigateDestinations.TwoFactorReset(
                            greenWallet = viewModel.greenWallet,
                            network = alertType.network,
                            twoFactorReset = viewModel.sessionOrNull?.twoFactorReset(alertType.network)?.value
                        )
                    )
                }
            )
        }

        is AlertType.EphemeralBip39 -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_passphrase_protected),
                message = stringResource(Res.string.id_this_wallet_is_based_on_your),
                icon = painterResource(Res.drawable.bip39_passphrase)
            )
        }
        is AlertType.Banner -> {
            Banner(banner = alertType.banner,
                modifier = modifier,
                onClick = {
                    viewModel.postEvent(Events.BannerAction)
                }, onClose = {
                    viewModel.postEvent(Events.BannerDismiss)
                }
            )
        }
        is AlertType.FailedNetworkLogin -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_warning),
                message = stringResource(Res.string.id_some_accounts_cannot_be_logged),
                primaryButton = stringResource(Res.string.id_try_again),
                onPrimaryClick = {
                    viewModel.postEvent(Events.ReconnectFailedNetworks)
                }
            )
        }
        is AlertType.LspStatus -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_lightning_account),
                message = stringResource(if (alertType.maintenance) Res.string.id_lightning_service_is_undergoing else Res.string.id_the_lightning_service_is),
                icon = painterResource(Res.drawable.lightning)
            )
        }

    }
}

@Composable
fun GreenAlert(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    icon: Painter? = null,
    primaryButton: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.then(modifier)) {
        Box {
            GreenRow(
                padding = 0,
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (primaryButton == null) 16.dp else 8.dp
                    )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {

                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                GreenColumn(space = 4, padding = 0, modifier = Modifier.weight(1f)) {
                    title?.also { title ->
                        Text(
                            text = title,
                            maxLines = 2,
                            style = labelLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    end = if (onCloseClick != null) 24.dp else 0.dp
                                )
                        )
                    }
                    message?.also { message ->
                        Text(
                            text = message,
                            style = bodyMedium,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    end = if (onCloseClick != null && title == null) 24.dp else 0.dp
                                )
                        )
                    }

                    primaryButton?.also {
                        GreenButton(
                            text = it,
                            type = GreenButtonType.TEXT,
                            size = GreenButtonSize.SMALL,
                            modifier = Modifier
                                .padding(0.dp)
                                .align(Alignment.End)
                        ) {
                            onPrimaryClick?.invoke()
                        }
                    }
                }
            }

            if (onCloseClick != null) {
                IconButton(
                    onClick = { onCloseClick.invoke() }, modifier = Modifier
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.x),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
