package com.blockstream.compose.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.brand
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.facebook_logo
import blockstream_green.common.generated.resources.github_logo
import blockstream_green.common.generated.resources.globe
import blockstream_green.common.generated.resources.id_copy_device_id
import blockstream_green.common.generated.resources.id_get_support
import blockstream_green.common.generated.resources.id_give_us_your_feedback
import blockstream_green.common.generated.resources.id_privacy_policy
import blockstream_green.common.generated.resources.id_terms_of_service
import blockstream_green.common.generated.resources.id_visit_the_blockstream_help
import blockstream_green.common.generated.resources.linkedin_logo
import blockstream_green.common.generated.resources.number_zero
import blockstream_green.common.generated.resources.telegram_logo
import blockstream_green.common.generated.resources.trash
import blockstream_green.common.generated.resources.x
import blockstream_green.common.generated.resources.x_logo
import blockstream_green.common.generated.resources.youtube_logo
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Bug
import com.adamglin.phosphoricons.regular.Copy
import com.adamglin.phosphoricons.regular.Fire
import com.blockstream.common.SupportType
import com.blockstream.common.data.SupportData
import com.blockstream.common.models.about.AboutViewModel
import com.blockstream.common.models.about.AboutViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(
    viewModel: AboutViewModelAbstract,
) {
    val popupState = remember { PopupState() }

    SetupScreen(viewModel = viewModel, sideEffectsHandler = {
        when (it) {
            is SideEffects.OpenMenu -> {
                popupState.isContextMenuVisible.value = true
            }
        }
    }, verticalArrangement = Arrangement.SpaceBetween) {

        ConstraintLayout(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            val (box, logo) = createRefs()

            Text(
                viewModel.version, style = bodyMedium, modifier = Modifier
                    .constrainAs(logo) {
                        end.linkTo(box.end)
                        top.linkTo(box.bottom)
                    })

            Box(
                modifier = Modifier
                    .constrainAs(box) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }) {
                Image(
                    painter = painterResource(Res.drawable.brand),
                    contentDescription = null,
                    modifier = Modifier
                        .heightIn(80.dp, 120.dp)
                        .align(Alignment.Center)
                        .noRippleClickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickLogo)
                        }
                )

                PopupMenu(
                    state = popupState,
                    entries = listOf(
                        MenuEntry(
                            title = "Copy Firebase ID",
                            imageVector = PhosphorIcons.Regular.Fire,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.CopyFirebaseId)
                            }
                        ),
                        MenuEntry(
                            title = stringResource(Res.string.id_copy_device_id),
                            imageVector = PhosphorIcons.Regular.Copy,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.CountlyCopyDeviceId)
                            }
                        ), MenuEntry(
                            title = "Reset Device ID",
                            iconRes = Res.drawable.trash,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.CountlyResetDeviceId)
                            }
                        ), MenuEntry(
                            title = "Set Countly Offset to zero",
                            iconRes = Res.drawable.number_zero,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.CountlyZeroOffset)
                            }
                        ), MenuEntry(
                            title = "Reset Promos",
                            iconRes = Res.drawable.x,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.ResetPromos)
                            }
                        ), MenuEntry(
                            title = "Delete Events",
                            iconRes = Res.drawable.eye,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.DeleteEvents)
                            }
                        ), MenuEntry(
                            title = "Create Crash Report",
                            imageVector = PhosphorIcons.Regular.Bug,
                            onClick = {
                                viewModel.postEvent(AboutViewModel.LocalEvents.CrashReport)
                            }
                        )
                    )
                )
            }
        }

        GreenColumn(padding = 0, space = 32) {
            GreenRow(
                padding = 0,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally)
            ) {
                Image(
                    painter = painterResource(Res.drawable.globe),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickWebsite)
                        },
                    contentDescription = null,
                )
                Image(
                    painter = painterResource(Res.drawable.x_logo),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickTwitter)
                        },
                    contentDescription = null,
                )
                Image(
                    painter = painterResource(Res.drawable.linkedin_logo),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickLinkedIn)
                        },
                    contentDescription = null,
                )
                Image(
                    painter = painterResource(Res.drawable.facebook_logo),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickFacebook)
                        },
                    contentDescription = null,
                )
            }

            GreenRow(
                padding = 0,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally)
            ) {
                Image(
                    painter = painterResource(Res.drawable.telegram_logo),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickTelegram)
                        },
                    contentDescription = null,
                )
                Image(
                    painter = painterResource(Res.drawable.github_logo),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickGitHub)
                        },
                    contentDescription = null,
                )
                Image(
                    painter = painterResource(Res.drawable.youtube_logo),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            viewModel.postEvent(AboutViewModel.LocalEvents.ClickYouTube)
                        },
                    contentDescription = null,
                )
            }
        }


        GreenColumn(padding = 0) {

            GreenCard(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        viewModel.postEvent(
                            NavigateDestinations.Support(
                                type = SupportType.FEEDBACK,
                                supportData = SupportData.create()
                            )
                        )
                    },
            ) {
                Text(
                    stringResource(Res.string.id_give_us_your_feedback),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    style = labelLarge,
                )
            }

            GreenCard(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        viewModel.postEvent(AboutViewModel.LocalEvents.ClickHelp)
                    },
            ) {
                Text(
                    stringResource(Res.string.id_visit_the_blockstream_help),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    style = labelLarge,
                )
            }

            GreenCard(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        viewModel.postEvent(
                            NavigateDestinations.Support(
                                type = SupportType.INCIDENT,
                                supportData = SupportData.create()
                            )
                        )
                    },
            ) {
                Text(
                    stringResource(Res.string.id_get_support),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    style = labelLarge,
                )
            }

        }

        GreenColumn(padding = 0, modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GreenButton(
                    text = stringResource(Res.string.id_terms_of_service),
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL
                ) {
                    viewModel.postEvent(AboutViewModel.LocalEvents.ClickTermsOfService)
                }

                GreenButton(
                    text = stringResource(Res.string.id_privacy_policy),
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL
                ) {
                    viewModel.postEvent(AboutViewModel.LocalEvents.ClickPrivacyPolicy)
                }
            }

            Text(
                text = "@ ${viewModel.year} Blockstream Corporation Inc.",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                color = whiteLow,
                style = bodySmall,
            )
        }
    }
}

