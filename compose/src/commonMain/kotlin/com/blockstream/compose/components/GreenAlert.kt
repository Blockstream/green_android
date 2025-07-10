package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_2fa_dispute_in_progress
import blockstream_green.common.generated.resources.id_2fa_reset_in_progress
import blockstream_green.common.generated.resources.id_back_up_your_wallet_now
import blockstream_green.common.generated.resources.id_backup
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_lightning_account
import blockstream_green.common.generated.resources.id_lightning_service_is_undergoing
import blockstream_green.common.generated.resources.id_passphrase_protected
import blockstream_green.common.generated.resources.id_reenable_2fa
import blockstream_green.common.generated.resources.id_some_accounts_cannot_be_logged
import blockstream_green.common.generated.resources.id_some_coins_are_no_longer_2fa_protected
import blockstream_green.common.generated.resources.id_system_message
import blockstream_green.common.generated.resources.id_the_lightning_service_is
import blockstream_green.common.generated.resources.id_this_wallet_is_based_on_your
import blockstream_green.common.generated.resources.id_this_wallet_operates_on_a_test
import blockstream_green.common.generated.resources.id_try_again
import blockstream_green.common.generated.resources.id_warning
import blockstream_green.common.generated.resources.id_warning_wallet_locked_by
import blockstream_green.common.generated.resources.id_your_recovery_phrase_is_the_only_way
import blockstream_green.common.generated.resources.id_your_wallet_is_locked_for_a
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Lightning
import com.adamglin.phosphoricons.regular.Password
import com.adamglin.phosphoricons.regular.Warning
import com.adamglin.phosphoricons.regular.X
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.theme.blueOutline
import com.blockstream.compose.theme.blueSurface
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.orangeOutline
import com.blockstream.compose.theme.orangeSurface
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenAlert(modifier: Modifier = Modifier, alertType: AlertType, viewModel: GreenViewModel) {
    when (alertType) {
        is AlertType.TestnetWarning -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_warning),
                message = stringResource(Res.string.id_this_wallet_operates_on_a_test),
                icon = PhosphorIcons.Regular.Info,
                isBlue = true
            )
        }

        is AlertType.RecoveryIsUnconfirmed -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_back_up_your_wallet_now),
                message = stringResource(Res.string.id_your_recovery_phrase_is_the_only_way),
                icon = PhosphorIcons.Regular.Warning,
                primaryButton = stringResource(Res.string.id_backup),
                onPrimaryClick = {
                    viewModel.postEvent(
                        NavigateDestinations.RecoveryIntro(
                            setupArgs = SetupArgs(
                                greenWallet = viewModel.greenWallet,
                                isShowRecovery = true
                            )
                        )
                    )
                },
                onCloseClick = if (alertType.withCloseButton) {
                    {
                        viewModel.postEvent(Events.DismissWalletBackupAlert)
                    }
                } else null
            )
        }

        is AlertType.SystemMessage -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_system_message),
                message = alertType.message,
                maxLines = 3,
                primaryButton = stringResource(Res.string.id_learn_more),
                icon = PhosphorIcons.Regular.Info,
                isBlue = true,
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
                icon = PhosphorIcons.Regular.Warning,
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
                message = stringResource(
                    Res.string.id_your_wallet_is_locked_for_a,
                    alertType.twoFactorReset.daysRemaining
                ),
                icon = PhosphorIcons.Regular.Warning,
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
                icon = PhosphorIcons.Regular.Password,
                isBlue = true
            )
        }

        is AlertType.Banner -> {
            Banner(
                banner = alertType.banner,
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
                icon = PhosphorIcons.Regular.Warning,
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
                icon = PhosphorIcons.Regular.Lightning,
                isBlue = true
            )
        }

        is AlertType.ReEnable2FA -> {
            GreenAlert(
                modifier = modifier,
                title = stringResource(Res.string.id_reenable_2fa),
                message = stringResource(Res.string.id_some_coins_are_no_longer_2fa_protected),
                icon = PhosphorIcons.Regular.Warning,
                primaryButton = stringResource(Res.string.id_reenable_2fa),
                onPrimaryClick = {
                    viewModel.postEvent(NavigateDestinations.ReEnable2FA(viewModel.greenWallet))
                }
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
    isBlue: Boolean = false,
    icon: ImageVector? = null,
    primaryButton: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
) {
    val containerColor = if (isBlue) blueSurface else orangeSurface
    val outline = if (isBlue) blueOutline else orangeOutline

    GreenCard(
        padding = 0,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, outline),
        modifier = Modifier.then(modifier)
    ) {
        Box {
            GreenRow(
                padding = 0,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {

                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                GreenColumn(space = 4, padding = 0, modifier = Modifier.weight(1f)) {
                    title?.also { title ->
                        Text(
                            text = title,
                            maxLines = 2,
                            style = titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    end = if (onCloseClick != null) 16.dp else 0.dp
                                )
                        )
                    }
                    message?.also { message ->
                        Text(
                            text = message,
                            style = bodyMedium,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            color = whiteMedium,
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
                            type = GreenButtonType.OUTLINE,
                            size = GreenButtonSize.SMALL,
                            color = GreenButtonColor.WHITE,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .align(Alignment.Start)
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
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun GreenAlertPreview() {
    GreenPreview {
        GreenColumn {

            GreenAlert(title = "Important!")

            GreenAlert(message = "This is a message")


            GreenAlert(
                title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
            )

            GreenAlert(
                title = "Lorem ipsum dolor sit amet",
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                onCloseClick = {

                })

            GreenAlert(
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                onCloseClick = {

                })

            GreenAlert(
                title = "Important!",
                message = "This is a message",
                icon = PhosphorIcons.Regular.Warning,
                primaryButton = "Learn More",
                onCloseClick = {

                })

            GreenAlert(
                title = "Lightning Account",
                message = "The Lightning service is currently unavailable. We apologize for the disruption.",
                icon = PhosphorIcons.Regular.Lightning,
                isBlue = true
            )
        }
    }
}