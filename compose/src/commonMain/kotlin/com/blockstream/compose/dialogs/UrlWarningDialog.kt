package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_advanced
import blockstream_green.common.generated.resources.id_allow_nondefault_connection
import blockstream_green.common.generated.resources.id_connection_attempt_to_s
import blockstream_green.common.generated.resources.id_connection_blocked
import blockstream_green.common.generated.resources.id_contact_support
import blockstream_green.common.generated.resources.id_dont_ask_me_again_for_this
import blockstream_green.common.generated.resources.id_if_you_did_not_change_your_oracle_settings
import blockstream_green.common.generated.resources.id_jade_is_trying_to_connect_to_a_non_default
import blockstream_green.common.generated.resources.id_this_is_not_the_default_blind_pin_oracle
import blockstream_green.common.generated.resources.id_warning
import blockstream_green.common.generated.resources.warning
import blockstream_green.common.generated.resources.x_bold
import com.blockstream.common.SupportType
import com.blockstream.common.data.SupportData
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.utils.hostname
import com.blockstream.compose.LocalAppCoroutine
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.redDark
import com.blockstream.compose.theme.textHigh
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun UrlWarningDialog(
    viewModel: GreenViewModel,
    urls: List<String>,
    onDismiss: (allow: Boolean, remember : Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismiss(false, false)
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        val dialog = LocalDialog.current
        val appCoroutine = LocalAppCoroutine.current
        val platformManager = LocalPlatformManager.current

        var showAdvanced by remember { mutableStateOf(false) }
        var rememberSwitch by remember { mutableStateOf(false) }

        GreenCard(
            colors = CardDefaults.elevatedCardColors(containerColor = redDark),
            padding = 0
        ) {

            Column {

                Box(modifier = Modifier.fillMaxWidth()) {
                    GreenRow(padding = 0, space = 8, modifier = Modifier.align(Alignment.Center)) {
                        Icon(
                            painter = painterResource(Res.drawable.warning),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(Res.string.id_warning),
                            color = textHigh,
                            style = titleMedium,
                        )
                    }

                    IconButton(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(all = 8.dp),
                        onClick = {
                            onDismiss(false, false)
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.x_bold),
                            contentDescription = "Close",
                        )
                    }
                }

                GreenColumn(
                    padding = 0,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)
                ) {

                    if (!showAdvanced) {
                        GreenColumn(
                            padding = 0,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.id_connection_blocked),
                                color = textHigh,
                                textAlign = TextAlign.Center,
                                style = titleLarge
                            )

                            Text(
                                text = stringResource(Res.string.id_jade_is_trying_to_connect_to_a_non_default),
                                color = textHigh,
                                textAlign = TextAlign.Center,
                                style = bodyMedium
                            )
                        }
                    }

                    if (showAdvanced) {
                        GreenColumn(
                            padding = 0,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = stringResource(
                                    Res.string.id_connection_attempt_to_s,
                                    urls.joinToString(", ") { it.hostname() }
                                ),
                                color = textHigh,
                                textAlign = TextAlign.Center,
                                style = titleSmall
                            )

                            Text(
                                text = stringResource(Res.string.id_this_is_not_the_default_blind_pin_oracle),
                                color = textHigh,
                                textAlign = TextAlign.Center,
                                style = labelLarge
                            )

                            Text(
                                text = stringResource(Res.string.id_if_you_did_not_change_your_oracle_settings),
                                color = textMedium,
                                textAlign = TextAlign.Center,
                                style = bodyMedium
                            )
                        }
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_contact_support),
                        type = GreenButtonType.COLOR,
                        color = GreenButtonColor.WHITE,
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.Support(
                                type = SupportType.INCIDENT,
                                supportData = SupportData(
                                    subject = "Non-default PIN server",
                                    zendeskHardwareWallet = DeviceModel.BlockstreamGeneric.zendeskValue
                                )
                            )
                        )

                        onDismiss(false, false)
                    }

                    GreenColumn(padding = 0) {

                        if (!showAdvanced) {
                            GreenButton(
                                text = stringResource(Res.string.id_advanced),
                                type = GreenButtonType.OUTLINE,
                                color = GreenButtonColor.WHITE,
                                size = GreenButtonSize.BIG,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                showAdvanced = true
                            }
                        }

                        if (showAdvanced) {

                            GreenButton(
                                text = stringResource(Res.string.id_allow_nondefault_connection),
                                type = GreenButtonType.OUTLINE,
                                color = GreenButtonColor.WHITE,
                                size = GreenButtonSize.BIG,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                onDismiss(true, rememberSwitch)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(Res.string.id_dont_ask_me_again_for_this),
                                    modifier = Modifier.weight(1f),
                                    color = textMedium,
                                    style = bodyMedium
                                )

                                Switch(
                                    checked = rememberSwitch,
                                    onCheckedChange = {
                                        rememberSwitch = !rememberSwitch
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
