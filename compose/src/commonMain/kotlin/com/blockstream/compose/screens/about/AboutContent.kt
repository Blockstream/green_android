package com.blockstream.compose.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.brand
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.id_copy_device_id
import blockstream_green.common.generated.resources.id_get_support
import blockstream_green.common.generated.resources.id_give_us_your_feedback
import blockstream_green.common.generated.resources.id_privacy_policy
import blockstream_green.common.generated.resources.id_terms_of_service
import blockstream_green.common.generated.resources.id_version_1s
import blockstream_green.common.generated.resources.id_visit_the_blockstream_help
import blockstream_green.common.generated.resources.number_zero
import blockstream_green.common.generated.resources.trash
import blockstream_green.common.generated.resources.x
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Bug
import com.adamglin.phosphoricons.regular.Copy
import com.adamglin.phosphoricons.regular.FacebookLogo
import com.adamglin.phosphoricons.regular.Fire
import com.adamglin.phosphoricons.regular.GithubLogo
import com.adamglin.phosphoricons.regular.Link
import com.adamglin.phosphoricons.regular.LinkedinLogo
import com.adamglin.phosphoricons.regular.Swap
import com.adamglin.phosphoricons.regular.TelegramLogo
import com.adamglin.phosphoricons.regular.XLogo
import com.adamglin.phosphoricons.regular.YoutubeLogo
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.models.about.AboutViewModel
import com.blockstream.compose.models.about.AboutViewModelAbstract
import com.blockstream.compose.models.about.AboutViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.data.SupportType
import com.blockstream.data.data.SupportData
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AboutContent(
    viewModel: AboutViewModelAbstract,
    popupState: PopupState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(Res.string.id_version_1s, viewModel.version),
            style = bodySmall,
            color = whiteMedium,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Image(
                painter = painterResource(Res.drawable.brand),
                contentDescription = null,
                modifier = Modifier
                    .height(34.dp)
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
                        title = "Delete Swaps",
                        imageVector = PhosphorIcons.Regular.Swap,
                        onClick = {
                            viewModel.postEvent(AboutViewModel.LocalEvents.DeleteSwaps)
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

        Spacer(modifier = Modifier.height(24.dp))

        SocialRow {
            SocialButton(PhosphorIcons.Regular.FacebookLogo, "Facebook", "about_link_facebook") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickFacebook)
            }
            SocialButton(PhosphorIcons.Regular.LinkedinLogo, "LinkedIn", "about_link_linkedin") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickLinkedIn)
            }
            SocialButton(PhosphorIcons.Regular.YoutubeLogo, "YouTube", "about_link_youtube") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickYouTube)
            }
            SocialButton(PhosphorIcons.Regular.GithubLogo, "GitHub", "about_link_github") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickGitHub)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SocialRow {
            SocialButton(PhosphorIcons.Regular.Link, "Blockstream website", "about_link_web") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickWebsite)
            }
            SocialButton(PhosphorIcons.Regular.XLogo, "X", "about_link_x") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickTwitter)
            }
            SocialButton(PhosphorIcons.Regular.TelegramLogo, "Telegram", "about_link_telegram") {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickTelegram)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            AboutRow(title = stringResource(Res.string.id_give_us_your_feedback)) {
                viewModel.postEvent(
                    NavigateDestinations.Support(
                        type = SupportType.FEEDBACK,
                        supportData = SupportData.create()
                    )
                )
            }

            AboutRow(title = stringResource(Res.string.id_get_support)) {
                viewModel.postEvent(
                    NavigateDestinations.Support(
                        type = SupportType.INCIDENT,
                        supportData = SupportData.create()
                    )
                )
            }

            AboutRow(title = stringResource(Res.string.id_visit_the_blockstream_help)) {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickHelp)
            }

            AboutRow(title = stringResource(Res.string.id_terms_of_service)) {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickTermsOfService)
            }

            AboutRow(title = stringResource(Res.string.id_privacy_policy)) {
                viewModel.postEvent(AboutViewModel.LocalEvents.ClickPrivacyPolicy)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "© ${viewModel.year} Blockstream Corporation Inc.",
            style = bodySmall,
            color = whiteLow,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SocialRow(content: @Composable RowScope.() -> Unit) {
    GreenRow(
        padding = 0,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        content = content
    )
}

@Composable
private fun SocialButton(
    imageVector: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .appTestTag(testTag)
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = whiteHigh,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AboutRow(
    title: String,
    onClick: () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

    Text(
        text = title,
        style = bodyLarge,
        color = whiteHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
@Preview
fun AboutContentPreview() {
    GreenPreview {
        GreenColumn {
            AboutContent(
                viewModel = AboutViewModelPreview.preview(),
                popupState = remember { PopupState() }
            )
        }
    }
}
